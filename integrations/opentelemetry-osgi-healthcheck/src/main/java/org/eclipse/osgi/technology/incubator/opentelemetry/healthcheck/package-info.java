/**
 * Bridges Apache Felix Health Check results into OpenTelemetry.
 * <p>
 * This package uses the Felix Health Check API to execute registered health checks
 * and expose results as OpenTelemetry signals:
 * <ul>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.healthcheck.HealthCheckMetricsComponent} — Async gauge metrics
 *       for health check counts per status and execution time</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.healthcheck.HealthCheckTracingComponent} — Periodic trace spans
 *       per health check execution result</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.healthcheck.HealthCheckInventoryComponent} — Structured log records
 *       for a complete health check inventory at startup</li>
 * </ul>
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.healthcheck;
