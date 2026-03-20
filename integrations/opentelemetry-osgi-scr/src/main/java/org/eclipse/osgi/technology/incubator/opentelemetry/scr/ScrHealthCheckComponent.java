package org.eclipse.osgi.technology.incubator.opentelemetry.scr;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Periodically inspects the DS component landscape and creates OpenTelemetry
 * traces for component health checks.
 * <p>
 * Every 30 seconds, this component creates a parent span {@code osgi.scr.healthcheck}
 * with a child span per component that is not in the ACTIVE state.
 * This makes component resolution problems visible in distributed trace backends.
 * <p>
 * Components in FAILED_ACTIVATION state produce error spans with the failure message.
 * Components with unsatisfied references produce warning spans listing the missing dependencies.
 */
@Component(immediate = true)
public class ScrHealthCheckComponent {

    private static final Logger LOG = Logger.getLogger(ScrHealthCheckComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.scr.healthcheck";

    @Reference
    private OpenTelemetry openTelemetry;

    @Reference
    private ServiceComponentRuntime scr;

    private ScheduledExecutorService scheduler;

    @Activate
    public void activate() {
        LOG.info("ScrHealthCheckComponent activated — starting periodic health checks");

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "otel-scr-healthcheck");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::runHealthCheck, 5, 30, TimeUnit.SECONDS);
    }

    @Deactivate
    public void deactivate() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        LOG.info("ScrHealthCheckComponent deactivated");
    }

    private void runHealthCheck() {
        try {
            Tracer tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE, "0.1.0");
            Collection<ComponentDescriptionDTO> descriptions = scr.getComponentDescriptionDTOs();

            int totalComponents = 0;
            int problemComponents = 0;

            Span parentSpan = tracer.spanBuilder("osgi.scr.healthcheck")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("osgi.scr.description.count", (long) descriptions.size())
                .startSpan();

            try (Scope parentScope = parentSpan.makeCurrent()) {
                for (ComponentDescriptionDTO desc : descriptions) {
                    for (ComponentConfigurationDTO config : scr.getComponentConfigurationDTOs(desc)) {
                        totalComponents++;

                        if (config.state != ComponentConfigurationDTO.ACTIVE
                                && config.state != ComponentConfigurationDTO.SATISFIED) {
                            problemComponents++;
                            traceComponentProblem(tracer, desc, config);
                        }
                    }
                }

                parentSpan.setAttribute("osgi.scr.configuration.total", totalComponents);
                parentSpan.setAttribute("osgi.scr.configuration.problems", problemComponents);

                if (problemComponents > 0) {
                    parentSpan.setStatus(StatusCode.ERROR,
                        problemComponents + " component(s) with problems");
                    parentSpan.addEvent("Health check found " + problemComponents + " problem(s)");
                } else {
                    parentSpan.addEvent("All " + totalComponents + " components healthy");
                }
            } finally {
                parentSpan.end();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "SCR health check failed", e);
        }
    }

    private void traceComponentProblem(Tracer tracer, ComponentDescriptionDTO desc,
            ComponentConfigurationDTO config) {
        String stateName = ScrMetricsComponent.configStateToString(config.state);

        Span span = tracer.spanBuilder("osgi.scr.problem." + stateName.toLowerCase())
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute(AttributeKey.stringKey("osgi.scr.component.name"), desc.name)
            .setAttribute(AttributeKey.stringKey("osgi.scr.component.class"), desc.implementationClass)
            .setAttribute(AttributeKey.stringKey("osgi.scr.component.state"), stateName)
            .setAttribute(AttributeKey.longKey("osgi.scr.component.config_id"), config.id)
            .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            if (desc.bundle != null) {
                span.setAttribute("osgi.scr.bundle.name", desc.bundle.symbolicName);
            }

            // Report unsatisfied references
            if (config.unsatisfiedReferences != null) {
                for (UnsatisfiedReferenceDTO ref : config.unsatisfiedReferences) {
                    span.addEvent("Unsatisfied reference: " + ref.name
                        + (ref.target != null ? " (target=" + ref.target + ")" : ""));
                }
            }

            // Report failure
            if (config.failure != null && !config.failure.isEmpty()) {
                span.setStatus(StatusCode.ERROR, "Activation failed");
                span.addEvent("Failure: " + config.failure);
            } else {
                span.setStatus(StatusCode.ERROR, "Component not active: " + stateName);
            }
        } finally {
            span.end();
        }
    }
}
