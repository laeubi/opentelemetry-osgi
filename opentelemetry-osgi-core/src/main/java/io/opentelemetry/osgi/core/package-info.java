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
 *   <li>{@link io.opentelemetry.osgi.core.BundleInventoryComponent} — Live inventory of bundles:
 *       emits a snapshot at activation and change records on every bundle lifecycle event</li>
 *   <li>{@link io.opentelemetry.osgi.core.ServiceInventoryComponent} — Live inventory of services:
 *       emits a snapshot at activation and change records on every service registration change</li>
 * </ul>
 */
package io.opentelemetry.osgi.core;
