package org.eclipse.osgi.technology.incubator.opentelemetry.demo;

/**
 * Simple DTO used as a typed event payload for the Typed Event demo.
 * Conforms to OSGi DTO rules: public fields, no methods other than
 * those inherited from Object.
 */
public class DemoEvent {

    public long sequence;

    public String timestamp;

    public String source;

    public String message;

    public double value;
}
