/**
 * OpenTelemetry Java Agent extension for OSGi framework instrumentation.
 * <p>
 * This package provides automatic instrumentation of OSGi frameworks when used
 * with the OpenTelemetry Java Agent. It instruments the following aspects:
 * <ul>
 *   <li><b>Resource detection</b> - Adds OSGi framework metadata (vendor, version, bundle counts)
 *       as resource attributes to all telemetry signals</li>
 *   <li><b>Framework metrics</b> - Continuously reports bundle counts, active bundles,
 *       service counts, and per-state bundle distributions</li>
 *   <li><b>Lifecycle tracing</b> - Creates spans for bundle events (install, start, stop, update)
 *       and service events (register, unregister, modify)</li>
 *   <li><b>Bundle inventory</b> - Emits structured log records with a complete snapshot
 *       of all installed bundles at startup</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * Build the agent extension JAR and add it to the Java Agent's extension path:
 * <pre>
 * java -javaagent:opentelemetry-javaagent.jar \
 *      -Dotel.javaagent.extensions=opentelemetry-osgi-agent.jar \
 *      -jar your-osgi-application.jar
 * </pre>
 *
 * @see OsgiAgentExtension
 * @see OsgiResourceProvider
 * @see OsgiMetricsProvider
 * @see OsgiEventListener
 * @see OsgiBundleInventoryLogger
 */
package io.opentelemetry.osgi.agent;
