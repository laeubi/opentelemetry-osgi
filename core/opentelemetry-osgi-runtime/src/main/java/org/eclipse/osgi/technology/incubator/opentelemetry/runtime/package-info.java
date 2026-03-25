/**
 * This package provides OSGi runtime services that create, configure, and publish
 * {@link io.opentelemetry.api.OpenTelemetry} SDK instances as OSGi services.
 * <p>
 * Each supported exporter type has its own service component with a dedicated
 * configuration PID, allowing multiple exporters to be active simultaneously:
 * <ul>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.runtime.LoggingOpenTelemetryService}
 *       — exports to stdout via {@code java.util.logging}</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.runtime.OtlpHttpOpenTelemetryService}
 *       — exports via OTLP/HTTP to a collector (port 4318)</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.runtime.OtlpGrpcOpenTelemetryService}
 *       — exports via OTLP/gRPC to a collector (port 4317)</li>
 * </ul>
 * <p>
 * The individual provider interfaces ({@link io.opentelemetry.api.trace.TracerProvider},
 * {@link io.opentelemetry.api.metrics.MeterProvider},
 * {@link io.opentelemetry.api.logs.LoggerProvider},
 * {@link io.opentelemetry.context.propagation.ContextPropagators}) are registered as
 * separate OSGi services with an {@code opentelemetry.name} property for filtering.
 * <p>
 * All configuration annotations use OSGi Metatype annotations
 * ({@code @ObjectClassDefinition}, {@code @AttributeDefinition}) for runtime
 * discoverability via management tools.
 *
 * @see org.eclipse.osgi.technology.incubator.opentelemetry.runtime.AbstractOpenTelemetryService
 * @see org.eclipse.osgi.technology.incubator.opentelemetry.runtime.OpenTelemetryProviderRegistration
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.runtime;
