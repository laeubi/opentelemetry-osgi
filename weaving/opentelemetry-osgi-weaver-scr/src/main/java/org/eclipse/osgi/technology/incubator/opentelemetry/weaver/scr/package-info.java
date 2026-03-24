/**
 * SCR lifecycle weaver fragment that instruments Declarative Services component
 * classes to produce OpenTelemetry spans and metrics for lifecycle operations
 * (activate, deactivate, modified, constructor injection).
 * <p>
 * The weaver parses the {@code Service-Component} manifest header and DS XML
 * descriptors to identify component implementation classes and their lifecycle
 * method names.
 * Method matching is based solely on the XML descriptor attributes — no
 * annotation scanning is performed, as annotations are not mandatory for DS.
 * <p>
 * The DS specification defines default method names: {@code activate} defaults
 * to {@code "activate"}, {@code deactivate} defaults to {@code "deactivate"},
 * and {@code modified} has no default (only instrumented when explicitly declared).
 * Constructor injection is detected via the {@code init} attribute.
 */
package org.eclipse.osgi.technology.incubator.opentelemetry.weaver.scr;
