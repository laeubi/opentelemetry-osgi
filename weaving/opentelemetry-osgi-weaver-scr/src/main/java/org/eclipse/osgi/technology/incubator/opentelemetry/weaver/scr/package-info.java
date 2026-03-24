/**
 * SCR lifecycle weaver fragment that instruments Declarative Services component
 * classes to produce OpenTelemetry spans and metrics for lifecycle operations
 * (activate, deactivate, modified, constructor injection).
 * <p>
 * The weaver parses the {@code Service-Component} manifest header and DS XML
 * descriptors to identify component implementation classes.
 * Only classes listed in the DS XML are considered for weaving.
 * Lifecycle methods are detected via {@code @Activate}, {@code @Deactivate},
 * and {@code @Modified} annotations from {@code org.osgi.service.component.annotations}.
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.weaver.scr;
