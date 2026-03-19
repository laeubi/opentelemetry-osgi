package io.opentelemetry.osgi.client;

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
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;

/**
 * Demonstrates the OpenTelemetry Log Bridge API within an OSGi environment.
 * <p>
 * The Log Bridge API is designed to bridge logs from existing logging frameworks
 * (e.g. SLF4J, JUL, Log4j) into OpenTelemetry.
 * It is <b>not</b> intended as a replacement logging API.
 * <p>
 * This component demonstrates:
 * <ul>
 *   <li>Creating log records with different severity levels</li>
 *   <li>Adding attributes and context to log records</li>
 *   <li>Emitting structured log data from OSGi bundle context</li>
 * </ul>
 */
@Component(immediate = true)
public class LogBridgeDemoComponent {

    private static final Logger LOG = Logger.getLogger(LogBridgeDemoComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.osgi.client.logbridge";

    @Reference
    private OpenTelemetry openTelemetry;

    @Activate
    public void activate(BundleContext context) {
        LOG.info("LogBridgeDemoComponent activated - demonstrating log bridge features");
        demonstrateLogBridge(context);
    }

    @Deactivate
    public void deactivate() {
        LOG.info("LogBridgeDemoComponent deactivated");
    }

    private void demonstrateLogBridge(BundleContext context) {
        io.opentelemetry.api.logs.Logger otelLogger =
            openTelemetry.getLogsBridge().loggerBuilder(INSTRUMENTATION_SCOPE)
                .setInstrumentationVersion("0.1.0")
                .build();

        // Emit an informational log about OSGi environment
        otelLogger.logRecordBuilder()
            .setSeverity(Severity.INFO)
            .setBody("OSGi environment initialized")
            .setAttribute(AttributeKey.stringKey("osgi.framework.vendor"),
                context.getProperty("org.osgi.framework.vendor"))
            .setAttribute(AttributeKey.stringKey("osgi.framework.version"),
                context.getProperty("org.osgi.framework.version"))
            .emit();

        LOG.info("  [LogBridge] Emitted OSGi environment info log record");

        // Emit log records for each installed bundle
        for (Bundle bundle : context.getBundles()) {
            otelLogger.logRecordBuilder()
                .setSeverity(Severity.DEBUG)
                .setBody("Bundle discovered: " + bundle.getSymbolicName())
                .setAttribute(AttributeKey.stringKey("bundle.symbolic_name"),
                    bundle.getSymbolicName())
                .setAttribute(AttributeKey.longKey("bundle.id"), bundle.getBundleId())
                .setAttribute(AttributeKey.stringKey("bundle.version"),
                    bundle.getVersion().toString())
                .setAttribute(AttributeKey.stringKey("bundle.state"),
                    bundleStateToString(bundle.getState()))
                .emit();
        }

        LOG.info("  [LogBridge] Emitted log records for " + context.getBundles().length + " bundles");

        // Demonstrate warning-level log
        otelLogger.logRecordBuilder()
            .setSeverity(Severity.WARN)
            .setBody("This is a demo warning from the OpenTelemetry OSGi client")
            .setAttribute(AttributeKey.stringKey("demo.component"), "LogBridgeDemoComponent")
            .emit();

        LOG.info("  [LogBridge] Emitted warning-level demo log record");
    }

    static String bundleStateToString(int state) {
        return switch (state) {
            case Bundle.UNINSTALLED -> "UNINSTALLED";
            case Bundle.INSTALLED -> "INSTALLED";
            case Bundle.RESOLVED -> "RESOLVED";
            case Bundle.STARTING -> "STARTING";
            case Bundle.STOPPING -> "STOPPING";
            case Bundle.ACTIVE -> "ACTIVE";
            default -> "UNKNOWN(" + state + ")";
        };
    }
}
