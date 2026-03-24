/**
 * Bridges the OSGi JAX-RS Whiteboard runtime into OpenTelemetry.
 * <p>
 * This package provides Declarative Services components that introspect
 * the {@link org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime} service
 * and expose the JAX-RS Whiteboard state — applications, resources,
 * extensions, and resource methods — as OpenTelemetry signals:
 * <ul>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.jaxrs.JaxrsWhiteboardMetricsComponent} — Async gauges
 *       for per-application counts of resources, extensions, and resource methods</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.jaxrs.JaxrsWhiteboardInventoryComponent} — Structured
 *       log records showing the full DTO hierarchy at activation and on configuration changes</li>
 * </ul>
 * <p>
 * The integration uses the pre-Jakarta JAX-RS Whiteboard API ({@code org.osgi.service.jaxrs.runtime})
 * compatible with Apache Aries JAX-RS Whiteboard and similar implementations.
 * Components only activate when a {@code JaxrsServiceRuntime} service is available.
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.jaxrs;
