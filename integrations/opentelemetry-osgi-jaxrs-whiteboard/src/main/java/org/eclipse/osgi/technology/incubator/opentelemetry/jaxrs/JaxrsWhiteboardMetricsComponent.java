package org.eclipse.osgi.technology.incubator.opentelemetry.jaxrs;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;

/**
 * Registers JAX-RS Whiteboard runtime metrics as OpenTelemetry async gauges.
 * <p>
 * Queries the {@link JaxrsServiceRuntime} DTO on every metric collection cycle
 * to reflect the live state of the JAX-RS Whiteboard:
 * <ul>
 *   <li>{@code osgi.jaxrs.applications} — Number of active JAX-RS applications</li>
 *   <li>{@code osgi.jaxrs.resources} — Number of resources per application</li>
 *   <li>{@code osgi.jaxrs.extensions} — Number of extensions per application</li>
 *   <li>{@code osgi.jaxrs.resource.methods} — Number of resource methods per application</li>
 *   <li>{@code osgi.jaxrs.failed} — Total failed registrations across all types</li>
 * </ul>
 */
@Component(immediate = true)
public class JaxrsWhiteboardMetricsComponent {

    private static final Logger LOG = Logger.getLogger(JaxrsWhiteboardMetricsComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.jaxrs";

    @Reference
    private OpenTelemetry openTelemetry;

    @Reference
    private JaxrsServiceRuntime jaxrsServiceRuntime;

    private ObservableLongGauge applicationsGauge;
    private ObservableLongGauge resourcesGauge;
    private ObservableLongGauge extensionsGauge;
    private ObservableLongGauge resourceMethodsGauge;
    private ObservableLongGauge failedGauge;

    @Activate
    public void activate() {
        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        applicationsGauge = meter.gaugeBuilder("osgi.jaxrs.applications")
            .setDescription("Number of active JAX-RS Whiteboard applications")
            .setUnit("{applications}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = jaxrsServiceRuntime.getRuntimeDTO();
                    long count = dto.applicationDTOs != null ? dto.applicationDTOs.length : 0;
                    if (dto.defaultApplication != null) count++;
                    measurement.record(count);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read application count", e);
                }
            });

        resourcesGauge = meter.gaugeBuilder("osgi.jaxrs.resources")
            .setDescription("Number of JAX-RS resources per application")
            .setUnit("{resources}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = jaxrsServiceRuntime.getRuntimeDTO();
                    if (dto.defaultApplication != null) {
                        emitApplicationResources(measurement, dto.defaultApplication);
                    }
                    if (dto.applicationDTOs != null) {
                        for (ApplicationDTO app : dto.applicationDTOs) {
                            emitApplicationResources(measurement, app);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read resource count", e);
                }
            });

        extensionsGauge = meter.gaugeBuilder("osgi.jaxrs.extensions")
            .setDescription("Number of JAX-RS extensions per application")
            .setUnit("{extensions}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = jaxrsServiceRuntime.getRuntimeDTO();
                    if (dto.defaultApplication != null) {
                        emitApplicationExtensions(measurement, dto.defaultApplication);
                    }
                    if (dto.applicationDTOs != null) {
                        for (ApplicationDTO app : dto.applicationDTOs) {
                            emitApplicationExtensions(measurement, app);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read extension count", e);
                }
            });

        resourceMethodsGauge = meter.gaugeBuilder("osgi.jaxrs.resource.methods")
            .setDescription("Number of JAX-RS resource methods per application")
            .setUnit("{methods}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = jaxrsServiceRuntime.getRuntimeDTO();
                    if (dto.defaultApplication != null) {
                        emitApplicationResourceMethods(measurement, dto.defaultApplication);
                    }
                    if (dto.applicationDTOs != null) {
                        for (ApplicationDTO app : dto.applicationDTOs) {
                            emitApplicationResourceMethods(measurement, app);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read resource method count", e);
                }
            });

        failedGauge = meter.gaugeBuilder("osgi.jaxrs.failed")
            .setDescription("Total number of failed JAX-RS Whiteboard registrations")
            .setUnit("{registrations}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    RuntimeDTO dto = jaxrsServiceRuntime.getRuntimeDTO();
                    long failed = 0;
                    if (dto.failedApplicationDTOs != null) failed += dto.failedApplicationDTOs.length;
                    if (dto.failedResourceDTOs != null) failed += dto.failedResourceDTOs.length;
                    if (dto.failedExtensionDTOs != null) failed += dto.failedExtensionDTOs.length;
                    measurement.record(failed);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read failed registration count", e);
                }
            });

        LOG.info("JaxrsWhiteboardMetricsComponent activated — JAX-RS Whiteboard metrics registered");
    }

    @Deactivate
    public void deactivate() {
        if (applicationsGauge != null) applicationsGauge.close();
        if (resourcesGauge != null) resourcesGauge.close();
        if (extensionsGauge != null) extensionsGauge.close();
        if (resourceMethodsGauge != null) resourceMethodsGauge.close();
        if (failedGauge != null) failedGauge.close();
        LOG.info("JaxrsWhiteboardMetricsComponent deactivated");
    }

    private static void emitApplicationResources(
            io.opentelemetry.api.metrics.ObservableLongMeasurement measurement,
            BaseApplicationDTO app) {
        Attributes attrs = applicationAttributes(app);
        measurement.record(app.resourceDTOs != null ? app.resourceDTOs.length : 0, attrs);
    }

    private static void emitApplicationExtensions(
            io.opentelemetry.api.metrics.ObservableLongMeasurement measurement,
            BaseApplicationDTO app) {
        Attributes attrs = applicationAttributes(app);
        measurement.record(app.extensionDTOs != null ? app.extensionDTOs.length : 0, attrs);
    }

    private static void emitApplicationResourceMethods(
            io.opentelemetry.api.metrics.ObservableLongMeasurement measurement,
            BaseApplicationDTO app) {
        Attributes attrs = applicationAttributes(app);
        long methodCount = 0;
        if (app.resourceDTOs != null) {
            for (var resource : app.resourceDTOs) {
                if (resource.resourceMethods != null) {
                    methodCount += resource.resourceMethods.length;
                }
            }
        }
        measurement.record(methodCount, attrs);
    }

    private static Attributes applicationAttributes(BaseApplicationDTO app) {
        return Attributes.of(
            AttributeKey.stringKey("application.name"), app.name != null ? app.name : "default",
            AttributeKey.stringKey("application.base"), app.base != null ? app.base : "/"
        );
    }
}
