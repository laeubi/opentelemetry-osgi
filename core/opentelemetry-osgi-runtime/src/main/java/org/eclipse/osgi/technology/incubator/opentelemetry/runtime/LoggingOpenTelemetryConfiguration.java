package org.eclipse.osgi.technology.incubator.opentelemetry.runtime;

/**
 * Configuration for the logging-based OpenTelemetry exporter.
 * <p>
 * Properties can be set via OSGi ConfigAdmin using the PID
 * {@value #PID}.
 * <p>
 * The logging exporter outputs telemetry to stdout via {@code java.util.logging},
 * useful for development and debugging without requiring an external collector.
 */
public @interface LoggingOpenTelemetryConfiguration {

    String COMPONENT_NAME = "logging-opentelemetry";

    String PID = "org.eclipse.osgi.technology.incubator.opentelemetry.runtime.logging";

    String serviceName() default "osgi-application";

    String serviceVersion() default "0.1.0";

    String serviceNamespace() default "";

    String[] additionalResourceAttributes() default {};
}
