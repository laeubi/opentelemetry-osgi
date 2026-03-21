/**
 * Bridges OSGi Configuration Admin events and inventory into OpenTelemetry.
 * <p>
 * This package uses the OSGi Configuration Admin service to track configuration
 * changes and expose them as OpenTelemetry signals:
 * <ul>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.cm.ConfigAdminMetricsComponent} — Async gauge metrics
 *       for configuration counts and event counters</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.cm.ConfigAdminEventComponent} — Trace spans
 *       for configuration update, delete, and location change events</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.cm.ConfigAdminInventoryComponent} — Structured log records
 *       for a complete configuration inventory at startup</li>
 * </ul>
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.cm;
