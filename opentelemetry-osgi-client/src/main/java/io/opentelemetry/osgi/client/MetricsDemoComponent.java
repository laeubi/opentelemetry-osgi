package io.opentelemetry.osgi.client;

import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;

/**
 * Demonstrates OpenTelemetry metrics capabilities within an OSGi environment.
 * <p>
 * This component showcases various metric instruments:
 * <ul>
 *   <li>{@link LongCounter} - monotonically increasing counter (e.g. request count)</li>
 *   <li>{@link LongUpDownCounter} - counter that can increase and decrease (e.g. active tasks)</li>
 *   <li>{@link DoubleHistogram} - distribution of values (e.g. request duration)</li>
 *   <li>{@link ObservableDoubleGauge} - asynchronous gauge (e.g. memory usage)</li>
 * </ul>
 */
@Component(immediate = true)
public class MetricsDemoComponent {

    private static final Logger LOG = Logger.getLogger(MetricsDemoComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.osgi.client.metrics";

    @Reference
    private OpenTelemetry openTelemetry;

    private ObservableDoubleGauge memoryGauge;

    @Activate
    public void activate() {
        LOG.info("MetricsDemoComponent activated - demonstrating metrics features");
        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);
        demonstrateCounter(meter);
        demonstrateUpDownCounter(meter);
        demonstrateHistogram(meter);
        demonstrateGauge(meter);
    }

    @Deactivate
    public void deactivate() {
        if (memoryGauge != null) {
            memoryGauge.close();
        }
        LOG.info("MetricsDemoComponent deactivated");
    }

    private void demonstrateCounter(Meter meter) {
        LongCounter requestCounter = meter.counterBuilder("osgi.client.requests")
            .setDescription("Total number of requests processed")
            .setUnit("{requests}")
            .build();

        // Simulate counting some requests
        requestCounter.add(1, Attributes.of(
            AttributeKey.stringKey("request.type"), "GET",
            AttributeKey.stringKey("request.path"), "/api/bundles"
        ));
        requestCounter.add(3, Attributes.of(
            AttributeKey.stringKey("request.type"), "POST",
            AttributeKey.stringKey("request.path"), "/api/services"
        ));

        LOG.info("  [Metrics] Counter 'osgi.client.requests' incremented");
    }

    private void demonstrateUpDownCounter(Meter meter) {
        LongUpDownCounter activeTaskCounter = meter.upDownCounterBuilder("osgi.client.active_tasks")
            .setDescription("Number of currently active tasks")
            .setUnit("{tasks}")
            .build();

        // Simulate tasks starting and completing
        activeTaskCounter.add(5);
        activeTaskCounter.add(-2);

        LOG.info("  [Metrics] UpDownCounter 'osgi.client.active_tasks' updated (5 added, 2 removed)");
    }

    private void demonstrateHistogram(Meter meter) {
        DoubleHistogram durationHistogram = meter.histogramBuilder("osgi.client.request_duration")
            .setDescription("Duration of request processing")
            .setUnit("ms")
            .build();

        // Simulate recording some durations
        durationHistogram.record(12.5, Attributes.of(
            AttributeKey.stringKey("operation"), "bundleLookup"
        ));
        durationHistogram.record(45.2, Attributes.of(
            AttributeKey.stringKey("operation"), "serviceResolution"
        ));
        durationHistogram.record(3.8, Attributes.of(
            AttributeKey.stringKey("operation"), "configRead"
        ));

        LOG.info("  [Metrics] Histogram 'osgi.client.request_duration' recorded values");
    }

    private void demonstrateGauge(Meter meter) {
        Runtime runtime = Runtime.getRuntime();

        memoryGauge = meter.gaugeBuilder("osgi.client.jvm_memory_used")
            .setDescription("Current JVM memory usage")
            .setUnit("By")
            .buildWithCallback(measurement -> {
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                measurement.record(usedMemory, Attributes.of(
                    AttributeKey.stringKey("memory.pool"), "heap"
                ));
            });

        LOG.info("  [Metrics] Gauge 'osgi.client.jvm_memory_used' registered (async callback)");
    }
}
