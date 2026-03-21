package org.eclipse.osgi.technology.incubator.opentelemetry.cm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
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
 * Exposes OSGi Configuration Admin state as OpenTelemetry metrics.
 * <p>
 * Publishes:
 * <ul>
 *   <li>{@code osgi.cm.configuration.count} — total number of configurations</li>
 *   <li>{@code osgi.cm.events.total} — counter of configuration events by type
 *       (CM_UPDATED, CM_DELETED, CM_LOCATION_CHANGED)</li>
 *   <li>{@code osgi.cm.factory.count} — number of factory configurations</li>
 * </ul>
 */
@Component(immediate = true, service = ConfigurationListener.class)
public class ConfigAdminMetricsComponent implements ConfigurationListener {

    private static final Logger LOG = Logger.getLogger(ConfigAdminMetricsComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.cm";

    @Reference
    private OpenTelemetry openTelemetry;

    @Reference
    private ConfigurationAdmin configAdmin;

    private ObservableLongGauge configCountGauge;
    private ObservableLongGauge factoryCountGauge;
    private LongCounter eventsCounter;

    @Activate
    public void activate() {
        LOG.info("ConfigAdminMetricsComponent activated — registering Config Admin metrics");
        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        eventsCounter = meter.counterBuilder("osgi.cm.events.total")
            .setDescription("Total number of configuration events")
            .setUnit("{events}")
            .build();

        configCountGauge = meter.gaugeBuilder("osgi.cm.configuration.count")
            .setDescription("Total number of configurations")
            .setUnit("{configurations}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    Configuration[] configs = configAdmin.listConfigurations(null);
                    measurement.record(configs != null ? configs.length : 0);
                } catch (IOException | org.osgi.framework.InvalidSyntaxException e) {
                    LOG.log(Level.FINE, "Failed to count configurations", e);
                }
            });

        factoryCountGauge = meter.gaugeBuilder("osgi.cm.factory.count")
            .setDescription("Number of factory configurations")
            .setUnit("{configurations}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    Configuration[] configs = configAdmin.listConfigurations(null);
                    if (configs != null) {
                        long factoryCount = 0;
                        for (Configuration config : configs) {
                            if (config.getFactoryPid() != null) {
                                factoryCount++;
                            }
                        }
                        measurement.record(factoryCount);
                    } else {
                        measurement.record(0);
                    }
                } catch (IOException | org.osgi.framework.InvalidSyntaxException e) {
                    LOG.log(Level.FINE, "Failed to count factory configurations", e);
                }
            });

        LOG.info("ConfigAdminMetricsComponent — Config Admin metrics registered");
    }

    @Deactivate
    public void deactivate() {
        closeQuietly(configCountGauge);
        closeQuietly(factoryCountGauge);
        LOG.info("ConfigAdminMetricsComponent deactivated");
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        try {
            String eventType = eventTypeToString(event.getType());
            eventsCounter.add(1, Attributes.of(
                AttributeKey.stringKey("cm.event.type"), eventType
            ));
        } catch (Exception e) {
            LOG.log(Level.FINE, "Failed to record configuration event metric", e);
        }
    }

    static String eventTypeToString(int type) {
        return switch (type) {
            case ConfigurationEvent.CM_UPDATED -> "CM_UPDATED";
            case ConfigurationEvent.CM_DELETED -> "CM_DELETED";
            case ConfigurationEvent.CM_LOCATION_CHANGED -> "CM_LOCATION_CHANGED";
            default -> "UNKNOWN(" + type + ")";
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
