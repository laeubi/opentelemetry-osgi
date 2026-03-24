/**
 * Bridges the OSGi HTTP Whiteboard runtime into OpenTelemetry.
 * <p>
 * This package provides Declarative Services components that introspect
 * the {@link org.osgi.service.http.runtime.HttpServiceRuntime} service
 * and expose the HTTP Whiteboard state — servlet contexts, servlets,
 * filters, listeners, resources, and error pages — as OpenTelemetry signals:
 * <ul>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.http.HttpWhiteboardMetricsComponent} — Async gauges
 *       for per-context counts of servlets, filters, listeners, resources, and error pages</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.http.HttpWhiteboardInventoryComponent} — Structured
 *       log records showing the full DTO hierarchy at activation and on configuration changes</li>
 * </ul>
 * <p>
 * The integration uses the pre-Jakarta HTTP Whiteboard API ({@code org.osgi.service.http.runtime})
 * compatible with Karaf 4.4.x and Pax Web 8.x.
 * Components only activate when an {@code HttpServiceRuntime} service is available.
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.http;
