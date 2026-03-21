/**
 * This package provides an OSGi runtime service that creates, configures, and publishes
 * an {@link io.opentelemetry.api.OpenTelemetry} SDK instance as an OSGi service.
 * <p>
 * In addition to the main {@link io.opentelemetry.api.OpenTelemetry} service, the individual
 * provider interfaces ({@link io.opentelemetry.api.trace.TracerProvider},
 * {@link io.opentelemetry.api.metrics.MeterProvider},
 * {@link io.opentelemetry.api.logs.LoggerProvider},
 * {@link io.opentelemetry.context.propagation.ContextPropagators}) are registered as
 * separate OSGi services for direct consumption.
 * <p>
 * The service is managed as an OSGi Declarative Services component and is configurable
 * via OSGi ConfigAdmin.
 *
 * @see org.eclipse.osgi.technology.incubator.opentelemetry.runtime.OpenTelemetryService
 * @see org.eclipse.osgi.technology.incubator.opentelemetry.runtime.OpenTelemetryProviderRegistration
 * @see org.eclipse.osgi.technology.incubator.opentelemetry.runtime.OpenTelemetryConfiguration
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.runtime;
