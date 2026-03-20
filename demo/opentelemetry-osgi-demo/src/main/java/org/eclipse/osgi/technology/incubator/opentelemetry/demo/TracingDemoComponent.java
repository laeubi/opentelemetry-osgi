package org.eclipse.osgi.technology.incubator.opentelemetry.demo;

import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/**
 * Demonstrates OpenTelemetry tracing capabilities within an OSGi environment.
 * <p>
 * This component creates various types of spans to showcase:
 * <ul>
 *   <li>Basic span creation and lifecycle</li>
 *   <li>Nested/child spans with parent-child relationships</li>
 *   <li>Span attributes, events, and status codes</li>
 *   <li>Different span kinds (INTERNAL, CLIENT, SERVER)</li>
 *   <li>Error recording on spans</li>
 * </ul>
 */
@Component(immediate = true)
public class TracingDemoComponent {

    private static final Logger LOG = Logger.getLogger(TracingDemoComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.demo.tracing";

    @Reference
    private OpenTelemetry openTelemetry;

    @Activate
    public void activate() {
        LOG.info("TracingDemoComponent activated - demonstrating tracing features");
        Tracer tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE, "0.1.0");
        demonstrateBasicSpan(tracer);
        demonstrateNestedSpans(tracer);
        demonstrateSpanWithError(tracer);
    }

    @Deactivate
    public void deactivate() {
        LOG.info("TracingDemoComponent deactivated");
    }

    private void demonstrateBasicSpan(Tracer tracer) {
        Span span = tracer.spanBuilder("osgi.client.basicOperation")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("osgi.component", "TracingDemoComponent")
            .setAttribute("demo.type", "basic")
            .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            span.addEvent("Processing started");
            LOG.info("  [Tracing] Basic span created and active");
            span.addEvent("Processing completed");
        } finally {
            span.end();
        }
    }

    private void demonstrateNestedSpans(Tracer tracer) {
        Span parentSpan = tracer.spanBuilder("osgi.client.parentOperation")
            .setSpanKind(SpanKind.SERVER)
            .setAttribute("osgi.component", "TracingDemoComponent")
            .startSpan();

        try (Scope parentScope = parentSpan.makeCurrent()) {
            LOG.info("  [Tracing] Parent span active");

            // Child span automatically linked via Context
            Span childSpan = tracer.spanBuilder("osgi.client.childOperation")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("demo.type", "nested-child")
                .startSpan();

            try (Scope childScope = childSpan.makeCurrent()) {
                LOG.info("  [Tracing] Child span active (parent-child relationship)");
                childSpan.addEvent("Child processing");
            } finally {
                childSpan.end();
            }
        } finally {
            parentSpan.end();
        }
    }

    private void demonstrateSpanWithError(Tracer tracer) {
        Span span = tracer.spanBuilder("osgi.client.errorOperation")
            .setAttribute("demo.type", "error-handling")
            .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            span.addEvent("About to simulate an error");
            throw new RuntimeException("Simulated error for tracing demo");
        } catch (RuntimeException e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            LOG.info("  [Tracing] Error recorded on span: " + e.getMessage());
        } finally {
            span.end();
        }
    }
}
