package org.eclipse.osgi.technology.incubator.opentelemetry.http;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;

/**
 * Registers HTTP Whiteboard runtime metrics as OpenTelemetry async gauges.
 * <p>
 * Queries the {@link HttpServiceRuntime} DTO on every metric collection cycle
 * to reflect the live state of the HTTP Whiteboard:
 * <ul>
 *   <li>{@code osgi.http.whiteboard.contexts} — Number of active servlet contexts</li>
 *   <li>{@code osgi.http.whiteboard.servlets} — Number of servlets per context</li>
 *   <li>{@code osgi.http.whiteboard.filters} — Number of filters per context</li>
 *   <li>{@code osgi.http.whiteboard.listeners} — Number of listeners per context</li>
 *   <li>{@code osgi.http.whiteboard.resources} — Number of resources per context</li>
 *   <li>{@code osgi.http.whiteboard.error.pages} — Number of error pages per context</li>
 *   <li>{@code osgi.http.whiteboard.failed} — Total failed registrations across all types</li>
 * </ul>
 */
@Component(immediate = true)
public class HttpWhiteboardMetricsComponent {

    private static final Logger LOG = Logger.getLogger(HttpWhiteboardMetricsComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.http";

    @Reference
    private OpenTelemetry openTelemetry;

    @Reference
    private HttpServiceRuntime httpServiceRuntime;

    private ObservableLongGauge contextsGauge;
    private ObservableLongGauge servletsGauge;
    private ObservableLongGauge filtersGauge;
    private ObservableLongGauge listenersGauge;
    private ObservableLongGauge resourcesGauge;
    private ObservableLongGauge errorPagesGauge;
    private ObservableLongGauge failedGauge;

    @Activate
    public void activate() {
        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        contextsGauge = meter.gaugeBuilder("osgi.http.whiteboard.contexts")
            .setDescription("Number of active HTTP Whiteboard servlet contexts")
            .setUnit("{contexts}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = httpServiceRuntime.getRuntimeDTO();
                    measurement.record(dto.servletContextDTOs != null ? dto.servletContextDTOs.length : 0);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read servlet context count", e);
                }
            });

        servletsGauge = meter.gaugeBuilder("osgi.http.whiteboard.servlets")
            .setDescription("Number of servlets registered per servlet context")
            .setUnit("{servlets}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = httpServiceRuntime.getRuntimeDTO();
                    if (dto.servletContextDTOs != null) {
                        for (ServletContextDTO ctx : dto.servletContextDTOs) {
                            Attributes attrs = contextAttributes(ctx);
                            measurement.record(ctx.servletDTOs != null ? ctx.servletDTOs.length : 0, attrs);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read servlet count", e);
                }
            });

        filtersGauge = meter.gaugeBuilder("osgi.http.whiteboard.filters")
            .setDescription("Number of filters registered per servlet context")
            .setUnit("{filters}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = httpServiceRuntime.getRuntimeDTO();
                    if (dto.servletContextDTOs != null) {
                        for (ServletContextDTO ctx : dto.servletContextDTOs) {
                            Attributes attrs = contextAttributes(ctx);
                            measurement.record(ctx.filterDTOs != null ? ctx.filterDTOs.length : 0, attrs);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read filter count", e);
                }
            });

        listenersGauge = meter.gaugeBuilder("osgi.http.whiteboard.listeners")
            .setDescription("Number of listeners registered per servlet context")
            .setUnit("{listeners}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = httpServiceRuntime.getRuntimeDTO();
                    if (dto.servletContextDTOs != null) {
                        for (ServletContextDTO ctx : dto.servletContextDTOs) {
                            Attributes attrs = contextAttributes(ctx);
                            measurement.record(ctx.listenerDTOs != null ? ctx.listenerDTOs.length : 0, attrs);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read listener count", e);
                }
            });

        resourcesGauge = meter.gaugeBuilder("osgi.http.whiteboard.resources")
            .setDescription("Number of resources registered per servlet context")
            .setUnit("{resources}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = httpServiceRuntime.getRuntimeDTO();
                    if (dto.servletContextDTOs != null) {
                        for (ServletContextDTO ctx : dto.servletContextDTOs) {
                            Attributes attrs = contextAttributes(ctx);
                            measurement.record(ctx.resourceDTOs != null ? ctx.resourceDTOs.length : 0, attrs);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read resource count", e);
                }
            });

        errorPagesGauge = meter.gaugeBuilder("osgi.http.whiteboard.error.pages")
            .setDescription("Number of error pages registered per servlet context")
            .setUnit("{error_pages}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = httpServiceRuntime.getRuntimeDTO();
                    if (dto.servletContextDTOs != null) {
                        for (ServletContextDTO ctx : dto.servletContextDTOs) {
                            Attributes attrs = contextAttributes(ctx);
                            measurement.record(ctx.errorPageDTOs != null ? ctx.errorPageDTOs.length : 0, attrs);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read error page count", e);
                }
            });

        failedGauge = meter.gaugeBuilder("osgi.http.whiteboard.failed")
            .setDescription("Total number of failed HTTP Whiteboard registrations")
            .setUnit("{registrations}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = httpServiceRuntime.getRuntimeDTO();
                    long failed = 0;
                    if (dto.failedServletDTOs != null) failed += dto.failedServletDTOs.length;
                    if (dto.failedFilterDTOs != null) failed += dto.failedFilterDTOs.length;
                    if (dto.failedListenerDTOs != null) failed += dto.failedListenerDTOs.length;
                    if (dto.failedResourceDTOs != null) failed += dto.failedResourceDTOs.length;
                    if (dto.failedErrorPageDTOs != null) failed += dto.failedErrorPageDTOs.length;
                    if (dto.failedServletContextDTOs != null) failed += dto.failedServletContextDTOs.length;
                    measurement.record(failed);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read failed registration count", e);
                }
            });

        LOG.info("HttpWhiteboardMetricsComponent activated — HTTP Whiteboard metrics registered");
    }

    @Deactivate
    public void deactivate() {
        if (contextsGauge != null) contextsGauge.close();
        if (servletsGauge != null) servletsGauge.close();
        if (filtersGauge != null) filtersGauge.close();
        if (listenersGauge != null) listenersGauge.close();
        if (resourcesGauge != null) resourcesGauge.close();
        if (errorPagesGauge != null) errorPagesGauge.close();
        if (failedGauge != null) failedGauge.close();
        LOG.info("HttpWhiteboardMetricsComponent deactivated");
    }

    private static Attributes contextAttributes(ServletContextDTO ctx) {
        return Attributes.of(
            AttributeKey.stringKey("context.name"), ctx.name != null ? ctx.name : "unknown",
            AttributeKey.stringKey("context.path"), ctx.contextPath != null ? ctx.contextPath : "/"
        );
    }
}
