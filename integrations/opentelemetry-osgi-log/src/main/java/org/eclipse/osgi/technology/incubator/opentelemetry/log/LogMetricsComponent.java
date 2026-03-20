package org.eclipse.osgi.technology.incubator.opentelemetry.log;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;

/**
 * Exposes OSGi Log Service statistics as OpenTelemetry metrics.
 * <p>
 * Listens to the {@link LogReaderService} and maintains counters for log entries
 * grouped by level and originating bundle. Metrics include:
 * <ul>
 *   <li>{@code osgi.log.entries} — counter of log entries, with {@code log.level} attribute</li>
 *   <li>{@code osgi.log.errors} — counter of ERROR-level entries, with {@code bundle.symbolic_name} attribute</li>
 * </ul>
 */
@Component(immediate = true)
public class LogMetricsComponent implements LogListener {

    private static final Logger LOG = Logger.getLogger(LogMetricsComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.log.metrics";

    @Reference
    private OpenTelemetry openTelemetry;

    @Reference
    private LogReaderService logReaderService;

    private LongCounter logEntryCounter;
    private LongCounter errorCounter;

    @Activate
    public void activate() {
        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        logEntryCounter = meter.counterBuilder("osgi.log.entries")
            .setDescription("Number of OSGi log entries by level")
            .setUnit("{entries}")
            .build();

        errorCounter = meter.counterBuilder("osgi.log.errors")
            .setDescription("Number of OSGi ERROR log entries by bundle")
            .setUnit("{entries}")
            .build();

        logReaderService.addLogListener(this);
        LOG.info("LogMetricsComponent activated — counting OSGi log entries");
    }

    @Deactivate
    public void deactivate() {
        logReaderService.removeLogListener(this);
        LOG.info("LogMetricsComponent deactivated");
    }

    @Override
    public void logged(LogEntry entry) {
        try {
            LogLevel level = entry.getLogLevel();
            String levelName = level != null ? level.name() : "UNKNOWN";

            logEntryCounter.add(1, Attributes.of(
                AttributeKey.stringKey("log.level"), levelName
            ));

            if (level == LogLevel.ERROR) {
                String bundleName = "unknown";
                if (entry.getBundle() != null && entry.getBundle().getSymbolicName() != null) {
                    bundleName = entry.getBundle().getSymbolicName();
                }
                errorCounter.add(1, Attributes.of(
                    AttributeKey.stringKey("bundle.symbolic_name"), bundleName
                ));
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error counting log entry", e);
        }
    }
}
