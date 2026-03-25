package org.eclipse.osgi.technology.incubator.opentelemetry.runtime;

/**
 * Configuration for the OTLP/HTTP OpenTelemetry exporter.
 * <p>
 * Properties can be set via OSGi ConfigAdmin using the PID
 * {@value #PID}.
 * <p>
 * The OTLP exporter sends telemetry via OTLP/HTTP to an OpenTelemetry Collector
 * or compatible backend.
 * The endpoint can be overridden by the {@code OTEL_EXPORTER_OTLP_ENDPOINT}
 * environment variable.
 */
public @interface OtlpOpenTelemetryConfiguration {

    String COMPONENT_NAME = "otlp-opentelemetry";

    String PID = "org.eclipse.osgi.technology.incubator.opentelemetry.runtime.otlp";

    String serviceName() default "osgi-application";

    String serviceVersion() default "0.1.0";

    String serviceNamespace() default "";

    String otlpEndpoint() default "http://localhost:4318";

    String[] additionalResourceAttributes() default {};
}
