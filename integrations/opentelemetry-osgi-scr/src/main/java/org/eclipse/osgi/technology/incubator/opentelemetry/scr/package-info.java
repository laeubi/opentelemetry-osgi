/**
 * Bridges OSGi Declarative Services (SCR) introspection data into OpenTelemetry.
 * <p>
 * This package uses the {@link org.osgi.service.component.runtime.ServiceComponentRuntime}
 * introspection API to expose DS component state as OpenTelemetry signals:
 * <ul>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.scr.ScrMetricsComponent} — Async gauge metrics for
 *       component counts, per-state distributions, and reference satisfaction</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.scr.ScrInventoryComponent} — Structured log records
 *       for a complete DS component inventory at startup</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.scr.ScrHealthCheckComponent} — Periodic trace spans
 *       highlighting components that are not active (unsatisfied refs, failed activation)</li>
 * </ul>
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.scr;
