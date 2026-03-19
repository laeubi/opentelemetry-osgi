package io.opentelemetry.osgi.agent;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

/**
 * Registers OSGi framework metrics with the OpenTelemetry SDK.
 * <p>
 * This provider hooks into the auto-configuration process to register
 * asynchronous gauge metrics that continuously report the state of the
 * OSGi framework. Metrics include:
 * <ul>
 *   <li>{@code osgi.bundle.count} - Total number of installed bundles</li>
 *   <li>{@code osgi.bundle.active} - Number of active bundles</li>
 *   <li>{@code osgi.service.count} - Number of registered services</li>
 *   <li>{@code osgi.bundle.states} - Bundle count per state (ACTIVE, RESOLVED, etc.)</li>
 * </ul>
 * <p>
 * Registered via SPI in
 * {@code META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider}.
 */
public class OsgiMetricsProvider implements AutoConfigurationCustomizerProvider {

    private static final Logger LOG = Logger.getLogger(OsgiMetricsProvider.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.osgi.agent";

    @Override
    public void customize(io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer autoConfiguration) {
        autoConfiguration.addMeterProviderCustomizer((meterProviderBuilder, config) -> {
            LOG.info("OSGi metrics provider: customizing MeterProvider");
            return meterProviderBuilder;
        });

        autoConfiguration.addTracerProviderCustomizer((tracerProviderBuilder, config) -> {
            LOG.info("OSGi metrics provider: customizing TracerProvider");
            return tracerProviderBuilder;
        });
    }

    /**
     * Registers OSGi metrics on the given {@link OpenTelemetry} instance.
     * Called from {@link OsgiAgentExtension} after the SDK is fully initialized.
     */
    static void registerMetrics(OpenTelemetry openTelemetry) {
        if (!isOsgiAvailable()) {
            LOG.info("OSGi not available - skipping OSGi metrics registration");
            return;
        }

        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        meter.gaugeBuilder("osgi.bundle.count")
            .setDescription("Total number of installed OSGi bundles")
            .setUnit("{bundles}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    OsgiFrameworkAccess access = new OsgiFrameworkAccess();
                    measurement.record(access.getBundleCount());
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read bundle count", e);
                }
            });

        meter.gaugeBuilder("osgi.bundle.active")
            .setDescription("Number of active OSGi bundles")
            .setUnit("{bundles}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    OsgiFrameworkAccess access = new OsgiFrameworkAccess();
                    measurement.record(access.getActiveBundleCount());
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read active bundle count", e);
                }
            });

        meter.gaugeBuilder("osgi.service.count")
            .setDescription("Number of registered OSGi services")
            .setUnit("{services}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    OsgiFrameworkAccess access = new OsgiFrameworkAccess();
                    measurement.record(access.getServiceCount());
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read service count", e);
                }
            });

        // Per-state bundle counts
        meter.gaugeBuilder("osgi.bundle.states")
            .setDescription("Number of OSGi bundles per state")
            .setUnit("{bundles}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    OsgiFrameworkAccess access = new OsgiFrameworkAccess();
                    var bundleInfos = access.getBundleInfos();

                    // Count bundles per state
                    var stateCounts = new java.util.HashMap<String, Long>();
                    for (BundleInfo info : bundleInfos) {
                        stateCounts.merge(info.stateName(), 1L, Long::sum);
                    }

                    stateCounts.forEach((stateName, count) ->
                        measurement.record(count, Attributes.of(
                            AttributeKey.stringKey("osgi.bundle.state"), stateName
                        ))
                    );
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read bundle states", e);
                }
            });

        LOG.info("OSGi framework metrics registered successfully");
    }

    private static boolean isOsgiAvailable() {
        try {
            Class.forName("org.osgi.framework.Bundle");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
