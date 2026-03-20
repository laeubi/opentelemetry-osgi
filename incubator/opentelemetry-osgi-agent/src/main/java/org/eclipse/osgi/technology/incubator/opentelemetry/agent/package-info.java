/**
 * OpenTelemetry Java Agent extension for OSGi framework instrumentation.
 *
 * <p>This extension uses ByteBuddy bytecode instrumentation (via the OTel
 * javaagent extension API) to intercept OSGi framework operations and produce
 * traces, metrics, and logs — without requiring any OSGi bundles to be installed.
 *
 * <h2>Instrumentation Targets</h2>
 * <ul>
 *   <li>{@code Framework.init()} — captures system BundleContext, registers metrics</li>
 *   <li>{@code Bundle.start/stop/update/uninstall()} — traces bundle lifecycle</li>
 *   <li>{@code BundleActivator.start/stop()} — traces activator execution</li>
 *   <li>{@code BundleContext.registerService/installBundle()} — traces service and bundle operations</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * java -javaagent:opentelemetry-javaagent.jar \
 *      -Dotel.javaagent.extensions=opentelemetry-osgi-agent.jar \
 *      -jar your-osgi-application.jar
 * </pre>
 *
 * <h2>Vendor Agnostic</h2>
 * Works with any OSGi R4+ framework implementation (Felix, Equinox, Knopflerfish, etc.)
 * by instrumenting standard OSGi interfaces, not vendor-specific classes.
 *
 * @see org.eclipse.osgi.technology.incubator.opentelemetry.agent.OsgiInstrumentationModule
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.agent;
