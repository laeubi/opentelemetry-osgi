/**
 * Bridges the OSGi Typed Event Service into OpenTelemetry.
 * <p>
 * This package observes events flowing through the
 * {@link org.osgi.service.typedevent.TypedEventBus TypedEventBus} and exposes
 * them as OpenTelemetry signals:
 * <ul>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.typedevent.TypedEventMetricsComponent} —
 *       Counters for events by topic and gauges for registered handler counts</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.typedevent.TypedEventTracingComponent} —
 *       Trace spans for each event delivered through the bus</li>
 *   <li>{@link org.eclipse.osgi.technology.incubator.opentelemetry.typedevent.TypedEventInventoryComponent} —
 *       Structured log records enumerating all registered event handlers at startup</li>
 * </ul>
 * <p>
 * The metrics and tracing components register as
 * {@link org.osgi.service.typedevent.UntypedEventHandler UntypedEventHandler}
 * services with {@code event.topics=*} to non-invasively observe all events.
 * Note that this means all events are considered "handled" — the
 * {@link org.osgi.service.typedevent.UnhandledEventHandler} will not fire for
 * events that would otherwise be unhandled.
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.typedevent;
