package org.eclipse.osgi.technology.incubator.opentelemetry.healthcheck;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckMetadata;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;

/**
 * Emits OpenTelemetry log records for all registered Felix Health Check results,
 * providing a detailed inventory of the system health status.
 * <p>
 * On activation, this component executes all health checks via the
 * {@link HealthCheckExecutor} and emits structured log records containing:
 * <ul>
 *   <li>Health check name, tags, and title</li>
 *   <li>Result status and execution duration</li>
 *   <li>Detailed result log entries</li>
 *   <li>Timeout information</li>
 * </ul>
 */
@Component(immediate = true)
public class HealthCheckInventoryComponent {

    private static final Logger LOG = Logger.getLogger(HealthCheckInventoryComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.healthcheck.inventory";

    @Reference
    private OpenTelemetry openTelemetry;

    @Reference
    private HealthCheckExecutor executor;

    @Activate
    public void activate() {
        LOG.info("HealthCheckInventoryComponent activated — logging health check inventory");

        try {
            io.opentelemetry.api.logs.Logger otelLogger =
                openTelemetry.getLogsBridge().loggerBuilder(INSTRUMENTATION_SCOPE)
                    .setInstrumentationVersion("0.1.0")
                    .build();

            List<HealthCheckExecutionResult> results = executor.execute(HealthCheckSelector.empty());

            long okCount = results.stream()
                .filter(r -> r.getHealthCheckResult().isOk())
                .count();

            // Summary log
            otelLogger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setBody("Health check inventory: " + results.size() + " checks ("
                    + okCount + " OK, " + (results.size() - okCount) + " with issues)")
                .setAttribute(AttributeKey.longKey("hc.total"), (long) results.size())
                .setAttribute(AttributeKey.longKey("hc.ok"), okCount)
                .setAttribute(AttributeKey.longKey("hc.problems"), (long) results.size() - okCount)
                .emit();

            // Detailed per-check logs
            for (HealthCheckExecutionResult execResult : results) {
                emitResultLog(otelLogger, execResult);
            }

            LOG.info("HealthCheckInventoryComponent — emitted " + results.size() + " health check log records");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to emit health check inventory", e);
        }
    }

    @Deactivate
    public void deactivate() {
        LOG.info("HealthCheckInventoryComponent deactivated");
    }

    private void emitResultLog(io.opentelemetry.api.logs.Logger otelLogger,
            HealthCheckExecutionResult execResult) {
        HealthCheckMetadata metadata = execResult.getHealthCheckMetadata();
        Result result = execResult.getHealthCheckResult();
        String name = metadata.getName() != null ? metadata.getName() : "unknown";
        String status = result.getStatus().name();

        Severity severity = switch (result.getStatus()) {
            case OK -> Severity.INFO;
            case WARN -> Severity.WARN;
            case TEMPORARILY_UNAVAILABLE -> Severity.WARN;
            case CRITICAL -> Severity.ERROR;
            case HEALTH_CHECK_ERROR -> Severity.ERROR;
        };

        var builder = otelLogger.logRecordBuilder()
            .setSeverity(severity)
            .setBody("Health check: " + name + " [" + status + "]")
            .setAttribute(AttributeKey.stringKey("hc.name"), name)
            .setAttribute(AttributeKey.stringKey("hc.status"), status)
            .setAttribute(AttributeKey.longKey("hc.duration_ms"), execResult.getElapsedTimeInMs())
            .setAttribute(AttributeKey.booleanKey("hc.timed_out"), execResult.hasTimedOut());

        if (metadata.getTitle() != null) {
            builder.setAttribute(AttributeKey.stringKey("hc.title"), metadata.getTitle());
        }

        List<String> tags = metadata.getTags();
        if (tags != null && !tags.isEmpty()) {
            builder.setAttribute(AttributeKey.stringArrayKey("hc.tags"), tags);
        }

        // Collect result log messages
        StringBuilder messages = new StringBuilder();
        for (ResultLog.Entry entry : result) {
            if (!messages.isEmpty()) {
                messages.append("; ");
            }
            messages.append(entry.getStatus().name()).append(": ").append(entry.getMessage());
        }
        if (!messages.isEmpty()) {
            builder.setAttribute(AttributeKey.stringKey("hc.result_log"), messages.toString());
        }

        builder.emit();
    }
}
