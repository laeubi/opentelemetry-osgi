package org.eclipse.osgi.technology.incubator.opentelemetry.healthcheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;

/**
 * Exposes Apache Felix Health Check results as OpenTelemetry metrics.
 * <p>
 * Periodically executes all registered health checks and publishes:
 * <ul>
 *   <li>{@code osgi.hc.count} — total number of registered health checks</li>
 *   <li>{@code osgi.hc.status} — number of health checks per result status
 *       (OK, WARN, TEMPORARILY_UNAVAILABLE, CRITICAL, HEALTH_CHECK_ERROR)</li>
 *   <li>{@code osgi.hc.duration.milliseconds} — last execution duration per health check</li>
 *   <li>{@code osgi.hc.executions.total} — counter of total health check executions</li>
 * </ul>
 */
@Component(immediate = true)
public class HealthCheckMetricsComponent {

    private static final Logger LOG = Logger.getLogger(HealthCheckMetricsComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.healthcheck";

    @Reference
    private OpenTelemetry openTelemetry;

    @Reference
    private HealthCheckExecutor executor;

    private ObservableLongGauge countGauge;
    private ObservableLongGauge statusGauge;
    private ObservableLongGauge durationGauge;
    private LongCounter executionsCounter;
    private ScheduledExecutorService scheduler;

    // Cached results from the most recent execution
    private volatile List<HealthCheckExecutionResult> lastResults = List.of();

    @Activate
    public void activate() {
        LOG.info("HealthCheckMetricsComponent activated — registering health check metrics");
        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        executionsCounter = meter.counterBuilder("osgi.hc.executions.total")
            .setDescription("Total number of health check executions")
            .setUnit("{executions}")
            .build();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "otel-hc-metrics");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::executeChecks, 10, 30, TimeUnit.SECONDS);

        countGauge = meter.gaugeBuilder("osgi.hc.count")
            .setDescription("Total number of registered health checks")
            .setUnit("{checks}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    measurement.record(lastResults.size());
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read health check count", e);
                }
            });

        statusGauge = meter.gaugeBuilder("osgi.hc.status")
            .setDescription("Number of health checks per result status")
            .setUnit("{checks}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    Map<String, Long> statusCounts = new HashMap<>();
                    for (HealthCheckExecutionResult result : lastResults) {
                        String status = result.getHealthCheckResult().getStatus().name();
                        statusCounts.merge(status, 1L, Long::sum);
                    }
                    statusCounts.forEach((status, count) ->
                        measurement.record(count, Attributes.of(
                            AttributeKey.stringKey("hc.status"), status
                        ))
                    );
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read health check status counts", e);
                }
            });

        durationGauge = meter.gaugeBuilder("osgi.hc.duration.milliseconds")
            .setDescription("Last execution duration per health check in milliseconds")
            .setUnit("ms")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    for (HealthCheckExecutionResult result : lastResults) {
                        String name = result.getHealthCheckMetadata().getName();
                        if (name != null) {
                            measurement.record(result.getElapsedTimeInMs(), Attributes.of(
                                AttributeKey.stringKey("hc.name"), name
                            ));
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read health check durations", e);
                }
            });

        LOG.info("HealthCheckMetricsComponent — health check metrics registered");
    }

    @Deactivate
    public void deactivate() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        closeQuietly(countGauge);
        closeQuietly(statusGauge);
        closeQuietly(durationGauge);
        LOG.info("HealthCheckMetricsComponent deactivated");
    }

    private void executeChecks() {
        try {
            List<HealthCheckExecutionResult> results = executor.execute(HealthCheckSelector.empty());
            lastResults = results;
            executionsCounter.add(results.size());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to execute health checks for metrics", e);
        }
    }

    static String statusToString(Result.Status status) {
        return switch (status) {
            case OK -> "OK";
            case WARN -> "WARN";
            case TEMPORARILY_UNAVAILABLE -> "TEMPORARILY_UNAVAILABLE";
            case CRITICAL -> "CRITICAL";
            case HEALTH_CHECK_ERROR -> "HEALTH_CHECK_ERROR";
        };
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
