/**
 * Bridges the OSGi core framework into OpenTelemetry.
 * <p>
 * This package provides Declarative Services components that expose
 * OSGi framework state — bundles, services, and lifecycle events —
 * as OpenTelemetry signals:
 * <ul>
 *   <li>{@link io.opentelemetry.osgi.core.FrameworkMetricsComponent} — Async gauges for
 *       bundle count, active bundles, service count, and per-state bundle distribution</li>
 *   <li>{@link io.opentelemetry.osgi.core.FrameworkEventComponent} — Traces for bundle
 *       and service lifecycle events (install, start, stop, register, unregister)</li>
 *   <li>{@link io.opentelemetry.osgi.core.BundleInventoryComponent} — Structured log records
 *       for a complete bundle inventory snapshot at activation</li>
 * </ul>
 */
package io.opentelemetry.osgi.core;
