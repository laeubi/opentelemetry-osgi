/**
 * OSGi Weaving Hook based bytecode instrumentation for OpenTelemetry.
 * <p>
 * This package provides infrastructure for lightweight, OSGi-native instrumentation
 * using {@link org.osgi.framework.hooks.weaving.WeavingHook}.
 * Weaver implementations are discovered via Java SPI from fragment bundles attached
 * to this host bundle.
 * <p>
 * This bundle intentionally avoids Declarative Services to ensure it activates
 * before DS-managed components, allowing it to weave DS component classes.
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.weaving;
