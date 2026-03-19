package io.opentelemetry.osgi.client;

import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.context.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates OpenTelemetry context propagation within an OSGi environment.
 * <p>
 * Context propagation is how distributed traces are linked across process
 * or service boundaries. This component demonstrates:
 * <ul>
 *   <li>Injecting trace context into a carrier (simulating outbound call)</li>
 *   <li>Extracting trace context from a carrier (simulating inbound call)</li>
 *   <li>Baggage propagation for cross-cutting concerns</li>
 * </ul>
 */
@Component(immediate = true)
public class ContextPropagationDemoComponent {

    private static final Logger LOG = Logger.getLogger(ContextPropagationDemoComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.osgi.client.propagation";

    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };

    private static final TextMapSetter<Map<String, String>> MAP_SETTER = Map::put;

    @Reference
    private OpenTelemetry openTelemetry;

    @Activate
    public void activate() {
        LOG.info("ContextPropagationDemoComponent activated - demonstrating context propagation");
        demonstrateContextPropagation();
    }

    @Deactivate
    public void deactivate() {
        LOG.info("ContextPropagationDemoComponent deactivated");
    }

    private void demonstrateContextPropagation() {
        Tracer tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE, "0.1.0");
        ContextPropagators propagators = openTelemetry.getPropagators();

        // Simulate a producer creating a span and injecting context into a carrier
        Map<String, String> carrier = new HashMap<>();

        Span producerSpan = tracer.spanBuilder("osgi.client.produceMessage")
            .setAttribute("demo.type", "context-propagation")
            .startSpan();

        try (Scope ignored = producerSpan.makeCurrent()) {
            // Inject current context into the carrier (like HTTP headers)
            propagators.getTextMapPropagator().inject(Context.current(), carrier, MAP_SETTER);
            LOG.info("  [Propagation] Context injected into carrier: " + carrier);
        } finally {
            producerSpan.end();
        }

        // Simulate a consumer extracting context from the carrier
        Context extractedContext = propagators.getTextMapPropagator()
            .extract(Context.current(), carrier, MAP_GETTER);

        Span consumerSpan = tracer.spanBuilder("osgi.client.consumeMessage")
            .setParent(extractedContext)
            .setAttribute("demo.type", "context-propagation")
            .startSpan();

        try (Scope ignored = consumerSpan.makeCurrent()) {
            LOG.info("  [Propagation] Context extracted - consumer span linked to producer");
        } finally {
            consumerSpan.end();
        }
    }
}
