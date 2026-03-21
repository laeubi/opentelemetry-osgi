package org.eclipse.osgi.technology.incubator.opentelemetry.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;

/**
 * Registers the individual OpenTelemetry provider interfaces as separate OSGi
 * services.
 * <p>
 * This allows consumers to depend directly on the specific provider they need
 * (e.g. {@link TracerProvider}, {@link MeterProvider}) instead of fetching the
 * full {@link OpenTelemetry} service and calling the corresponding getter.
 * <p>
 * The following services are registered:
 * <ul>
 *   <li>{@link TracerProvider}</li>
 *   <li>{@link MeterProvider}</li>
 *   <li>{@link LoggerProvider}</li>
 *   <li>{@link ContextPropagators}</li>
 * </ul>
 */
@Component(immediate = true)
public class OpenTelemetryProviderRegistration {

    private static final Logger LOG = Logger.getLogger(OpenTelemetryProviderRegistration.class.getName());

    @Reference
    private OpenTelemetry openTelemetry;

    private final List<ServiceRegistration<?>> registrations = new ArrayList<>();

    @Activate
    public void activate(BundleContext context) {
        registrations.add(context.registerService(
                TracerProvider.class, openTelemetry.getTracerProvider(), null));
        registrations.add(context.registerService(
                MeterProvider.class, openTelemetry.getMeterProvider(), null));
        registrations.add(context.registerService(
                LoggerProvider.class, openTelemetry.getLogsBridge(), null));
        registrations.add(context.registerService(
                ContextPropagators.class, openTelemetry.getPropagators(), null));

        LOG.info("Registered OpenTelemetry provider services: TracerProvider, MeterProvider, LoggerProvider, ContextPropagators");
    }

    @Deactivate
    public void deactivate() {
        for (ServiceRegistration<?> registration : registrations) {
            try {
                registration.unregister();
            } catch (IllegalStateException e) {
                // Already unregistered
            }
        }
        registrations.clear();
        LOG.info("Unregistered OpenTelemetry provider services");
    }
}
