package io.opentelemetry.osgi.runtime;

/**
 * Configuration for the OpenTelemetry SDK service.
 * <p>
 * Properties can be set via OSGi ConfigAdmin using the PID
 * {@code io.opentelemetry.osgi.runtime}.
 */
public @interface OpenTelemetryConfiguration {

    String serviceName() default "osgi-application";

    String serviceVersion() default "0.1.0";

    String serviceNamespace() default "";

    String exporterType() default "logging";

    String otlpEndpoint() default "http://localhost:4317";

    String[] additionalResourceAttributes() default {};
}
