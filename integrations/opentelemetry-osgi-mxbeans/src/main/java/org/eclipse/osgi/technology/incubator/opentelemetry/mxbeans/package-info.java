/**
 * Exposes Java Management Extensions (MXBeans) as OpenTelemetry metrics.
 * <p>
 * This package bridges the {@link java.lang.management.ManagementFactory} MXBeans
 * into OpenTelemetry async gauges and counters, providing visibility into JVM runtime
 * characteristics such as memory usage, CPU load, garbage collection, thread counts,
 * and class loading statistics.
 * <p>
 * The integration is configurable — individual MXBean metric groups can be enabled
 * or disabled via OSGi Configuration Admin.
 *
 * @see org.eclipse.osgi.technology.incubator.opentelemetry.mxbeans.MxBeansConfiguration
 * @see org.eclipse.osgi.technology.incubator.opentelemetry.mxbeans.MxBeansMetricsComponent
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.mxbeans;
