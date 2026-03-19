package io.opentelemetry.osgi.agent;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.logs.Severity;

/**
 * Emits structured log records for OSGi bundle inventory via the OpenTelemetry Log Bridge API.
 * <p>
 * At initialization time, this class creates a snapshot of all installed bundles and emits
 * a log record for each one, providing a complete inventory of the OSGi environment.
 * This is useful for correlating telemetry with specific bundle deployments.
 */
class OsgiBundleInventoryLogger {

    private static final Logger LOG = Logger.getLogger(OsgiBundleInventoryLogger.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.osgi.agent.inventory";

    /**
     * Logs the current bundle inventory via OpenTelemetry log records.
     */
    static void logBundleInventory(OpenTelemetry openTelemetry) {
        if (!isOsgiAvailable()) {
            return;
        }

        try {
            OsgiFrameworkAccess access = new OsgiFrameworkAccess();
            if (!access.isFrameworkRunning()) {
                return;
            }

            io.opentelemetry.api.logs.Logger otelLogger =
                openTelemetry.getLogsBridge().loggerBuilder(INSTRUMENTATION_SCOPE)
                    .setInstrumentationVersion("0.1.0")
                    .build();

            // Emit a summary log
            otelLogger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setBody("OSGi bundle inventory: " + access.getBundleCount()
                    + " bundles, " + access.getActiveBundleCount() + " active, "
                    + access.getServiceCount() + " services")
                .setAttribute(AttributeKey.stringKey("osgi.framework.vendor"), access.getFrameworkVendor())
                .setAttribute(AttributeKey.stringKey("osgi.framework.version"), access.getFrameworkVersion())
                .setAttribute(AttributeKey.longKey("osgi.bundle.total"), access.getBundleCount())
                .setAttribute(AttributeKey.longKey("osgi.bundle.active"), access.getActiveBundleCount())
                .setAttribute(AttributeKey.longKey("osgi.service.total"), access.getServiceCount())
                .emit();

            // Emit a log record per bundle
            for (BundleInfo info : access.getBundleInfos()) {
                otelLogger.logRecordBuilder()
                    .setSeverity(Severity.DEBUG)
                    .setBody("Bundle: " + info.symbolicName() + " [" + info.stateName() + "]")
                    .setAttribute(AttributeKey.longKey("osgi.bundle.id"), info.bundleId())
                    .setAttribute(AttributeKey.stringKey("osgi.bundle.symbolic_name"), info.symbolicName())
                    .setAttribute(AttributeKey.stringKey("osgi.bundle.version"), info.version())
                    .setAttribute(AttributeKey.stringKey("osgi.bundle.state"), info.stateName())
                    .setAttribute(AttributeKey.stringKey("osgi.bundle.location"), info.location())
                    .emit();
            }

            LOG.info("Bundle inventory logged: " + access.getBundleCount() + " bundles");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to log bundle inventory", e);
        }
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
