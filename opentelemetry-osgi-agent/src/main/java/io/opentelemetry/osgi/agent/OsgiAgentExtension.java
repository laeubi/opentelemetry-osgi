package io.opentelemetry.osgi.agent;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

/**
 * Main entry point for the OSGi agent extension.
 * <p>
 * This class coordinates the initialization of all OSGi instrumentation components:
 * <ul>
 *   <li>{@link OsgiResourceProvider} - adds OSGi resource attributes (registered via separate SPI)</li>
 *   <li>{@link OsgiMetricsProvider} - registers framework metrics</li>
 *   <li>{@link OsgiEventListener} - traces bundle and service lifecycle events</li>
 *   <li>{@link OsgiBundleInventoryLogger} - emits bundle inventory log records</li>
 * </ul>
 * <p>
 * This provider hooks into the SDK auto-configuration process. When the OpenTelemetry
 * Java Agent initializes, it discovers this class via SPI and invokes {@link #customize}
 * to install OSGi-aware instrumentation.
 * <p>
 * Registered via SPI in
 * {@code META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider}.
 */
public class OsgiAgentExtension implements AutoConfigurationCustomizerProvider {

    private static final Logger LOG = Logger.getLogger(OsgiAgentExtension.class.getName());

    @Override
    public void customize(io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer autoConfiguration) {
        LOG.info("OpenTelemetry OSGi Agent Extension: initializing");

        autoConfiguration.addTracerProviderCustomizer((tracerProviderBuilder, config) -> {
            LOG.info("OSGi Agent Extension: TracerProvider customization applied");
            return tracerProviderBuilder;
        });

        autoConfiguration.addMeterProviderCustomizer((meterProviderBuilder, config) -> {
            LOG.info("OSGi Agent Extension: MeterProvider customization applied");
            return meterProviderBuilder;
        });

        LOG.info("OpenTelemetry OSGi Agent Extension: configuration hooks installed");
    }

    /**
     * Initializes runtime components that require a fully built OpenTelemetry instance.
     * This is intended to be called after SDK initialization is complete.
     */
    public static void initializeRuntime(OpenTelemetry openTelemetry) {
        LOG.info("OSGi Agent Extension: initializing runtime components");

        // Register framework metrics
        OsgiMetricsProvider.registerMetrics(openTelemetry);

        // Start event listener for bundle/service lifecycle tracing
        OsgiEventListener listener = new OsgiEventListener(openTelemetry);
        if (listener.register()) {
            LOG.info("OSGi Agent Extension: event listener active");
        }

        // Log initial bundle inventory
        OsgiBundleInventoryLogger.logBundleInventory(openTelemetry);

        LOG.info("OSGi Agent Extension: runtime initialization complete");
    }
}
