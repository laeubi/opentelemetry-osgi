package io.opentelemetry.osgi.client;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Periodically generates OpenTelemetry traces, metrics, and logs to provide
 * a continuous stream of telemetry data for demonstration purposes.
 * <p>
 * This component simulates realistic OSGi operations such as bundle resolution,
 * service lookups, and configuration updates, producing telemetry that can be
 * explored in Grafana dashboards.
 */
@Component(immediate = true)
public class DemoSchedulerComponent {

    private static final Logger LOG = Logger.getLogger(DemoSchedulerComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.osgi.client.scheduler";

    private static final String[] OPERATIONS = {
        "bundle.resolve", "service.lookup", "config.update",
        "bundle.refresh", "service.bind", "package.wire"
    };

    @Reference
    private OpenTelemetry openTelemetry;

    private ScheduledExecutorService scheduler;
    private Tracer tracer;
    private Meter meter;
    private LongCounter operationCounter;
    private DoubleHistogram operationDuration;
    private io.opentelemetry.api.logs.Logger otelLogger;
    private final Random random = new Random();

    @Activate
    public void activate(BundleContext context) {
        LOG.info("DemoSchedulerComponent activated - starting periodic telemetry generation");

        tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE, "0.1.0");
        meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);
        otelLogger = openTelemetry.getLogsBridge().loggerBuilder(INSTRUMENTATION_SCOPE)
            .setInstrumentationVersion("0.1.0")
            .build();

        operationCounter = meter.counterBuilder("osgi.demo.operations")
            .setDescription("Number of simulated OSGi operations")
            .setUnit("{operations}")
            .build();

        operationDuration = meter.histogramBuilder("osgi.demo.operation_duration")
            .setDescription("Duration of simulated OSGi operations")
            .setUnit("ms")
            .build();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "otel-osgi-demo-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> runDemoCycle(context), 2, 5, TimeUnit.SECONDS);
    }

    @Deactivate
    public void deactivate() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        LOG.info("DemoSchedulerComponent deactivated");
    }

    private void runDemoCycle(BundleContext context) {
        try {
            simulateOsgiOperation(context);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error in demo cycle", e);
        }
    }

    private void simulateOsgiOperation(BundleContext context) {
        String operation = OPERATIONS[random.nextInt(OPERATIONS.length)];
        Bundle[] bundles = context.getBundles();
        Bundle targetBundle = bundles[random.nextInt(bundles.length)];
        String bundleName = targetBundle.getSymbolicName() != null
            ? targetBundle.getSymbolicName() : "bundle-" + targetBundle.getBundleId();

        long startTime = System.nanoTime();

        Span parentSpan = tracer.spanBuilder("osgi." + operation)
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("osgi.bundle.symbolic_name", bundleName)
            .setAttribute("osgi.bundle.id", targetBundle.getBundleId())
            .setAttribute("osgi.operation", operation)
            .startSpan();

        try (Scope ignored = parentSpan.makeCurrent()) {
            // Simulate sub-operations
            simulateSubOperation(tracer, "validate", bundleName);

            // Simulate some work with random duration
            Thread.sleep(random.nextInt(50) + 10);

            simulateSubOperation(tracer, "execute", bundleName);

            // Randomly simulate errors (~10% of the time)
            if (random.nextInt(10) == 0) {
                throw new RuntimeException("Simulated error in " + operation + " for " + bundleName);
            }

            parentSpan.addEvent("Operation completed successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            parentSpan.setStatus(StatusCode.ERROR, "Interrupted");
        } catch (RuntimeException e) {
            parentSpan.setStatus(StatusCode.ERROR, e.getMessage());
            parentSpan.recordException(e);

            otelLogger.logRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setBody("Operation failed: " + operation + " on " + bundleName)
                .setAttribute(AttributeKey.stringKey("osgi.operation"), operation)
                .setAttribute(AttributeKey.stringKey("osgi.bundle.symbolic_name"), bundleName)
                .setAttribute(AttributeKey.stringKey("error.message"), e.getMessage())
                .emit();
        } finally {
            parentSpan.end();
        }

        double durationMs = (System.nanoTime() - startTime) / 1_000_000.0;

        Attributes attrs = Attributes.of(
            AttributeKey.stringKey("osgi.operation"), operation,
            AttributeKey.stringKey("osgi.bundle.symbolic_name"), bundleName
        );
        operationCounter.add(1, attrs);
        operationDuration.record(durationMs, attrs);

        // Emit a log record for the operation
        otelLogger.logRecordBuilder()
            .setSeverity(Severity.INFO)
            .setBody("OSGi operation: " + operation + " on " + bundleName
                + " (" + String.format("%.1f", durationMs) + "ms)")
            .setAttribute(AttributeKey.stringKey("osgi.operation"), operation)
            .setAttribute(AttributeKey.stringKey("osgi.bundle.symbolic_name"), bundleName)
            .setAttribute(AttributeKey.doubleKey("duration_ms"), durationMs)
            .emit();
    }

    private void simulateSubOperation(Tracer tracer, String phase, String bundleName) {
        Span span = tracer.spanBuilder("osgi.phase." + phase)
            .setAttribute("osgi.bundle.symbolic_name", bundleName)
            .setAttribute("osgi.phase", phase)
            .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            Thread.sleep(random.nextInt(20) + 5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            span.end();
        }
    }
}
