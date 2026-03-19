/**
 * Bridges the OSGi Log Service into OpenTelemetry.
 * <p>
 * This package registers a {@link org.osgi.service.log.LogListener} with the
 * {@link org.osgi.service.log.LogReaderService} to capture all log entries
 * produced by bundles in the OSGi framework and forward them as OpenTelemetry signals:
 * <ul>
 *   <li>{@link io.opentelemetry.osgi.log.LogBridgeComponent} — Forwards every log entry as an
 *       OpenTelemetry log record with OSGi-specific attributes (bundle, service, thread, location)</li>
 *   <li>{@link io.opentelemetry.osgi.log.LogMetricsComponent} — Counts log entries as metrics,
 *       grouped by log level and bundle</li>
 * </ul>
 */
package io.opentelemetry.osgi.log;
