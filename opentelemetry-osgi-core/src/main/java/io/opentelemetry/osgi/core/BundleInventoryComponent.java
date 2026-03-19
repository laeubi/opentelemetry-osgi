package io.opentelemetry.osgi.core;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;

/**
 * Emits structured log records for a complete OSGi bundle inventory at activation.
 * <p>
 * When activated, this component takes a snapshot of all installed bundles and emits:
 * <ul>
 *   <li>A summary log record with total bundle, active bundle, and service counts</li>
 *   <li>One log record per bundle with id, symbolic name, version, state, and location</li>
 * </ul>
 * <p>
 * This provides a complete inventory of the OSGi environment, useful for correlating
 * telemetry with specific bundle deployments and framework configurations.
 */
@Component(immediate = true)
public class BundleInventoryComponent {

    private static final Logger LOG = Logger.getLogger(BundleInventoryComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.osgi.core.inventory";

    @Reference
    private OpenTelemetry openTelemetry;

    @Activate
    public void activate(BundleContext context) {
        try {
            io.opentelemetry.api.logs.Logger otelLogger =
                openTelemetry.getLogsBridge().loggerBuilder(INSTRUMENTATION_SCOPE)
                    .setInstrumentationVersion("0.1.0")
                    .build();

            Bundle[] bundles = context.getBundles();
            long activeCount = 0;
            List<BundleInfo> bundleInfos = new ArrayList<>(bundles.length);

            for (Bundle bundle : bundles) {
                int state = bundle.getState();
                if (state == Bundle.ACTIVE) {
                    activeCount++;
                }
                bundleInfos.add(new BundleInfo(
                    bundle.getBundleId(),
                    bundle.getSymbolicName(),
                    bundle.getVersion().toString(),
                    state,
                    BundleStateUtil.bundleStateToString(state),
                    bundle.getLocation()
                ));
            }

            long serviceCount = 0;
            try {
                ServiceReference<?>[] refs = context.getAllServiceReferences(null, null);
                serviceCount = refs != null ? refs.length : 0;
            } catch (Exception e) {
                LOG.log(Level.FINE, "Failed to count services", e);
            }

            String vendor = context.getProperty("org.osgi.framework.vendor");
            String version = context.getProperty("org.osgi.framework.version");

            // Summary log record
            otelLogger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setBody("OSGi bundle inventory: " + bundles.length
                    + " bundles, " + activeCount + " active, "
                    + serviceCount + " services")
                .setAttribute(AttributeKey.stringKey("osgi.framework.vendor"),
                    vendor != null ? vendor : "unknown")
                .setAttribute(AttributeKey.stringKey("osgi.framework.version"),
                    version != null ? version : "unknown")
                .setAttribute(AttributeKey.longKey("osgi.bundle.total"), (long) bundles.length)
                .setAttribute(AttributeKey.longKey("osgi.bundle.active"), activeCount)
                .setAttribute(AttributeKey.longKey("osgi.service.total"), serviceCount)
                .emit();

            // Per-bundle log records
            for (BundleInfo info : bundleInfos) {
                otelLogger.logRecordBuilder()
                    .setSeverity(Severity.DEBUG)
                    .setBody("Bundle: " + info.symbolicName() + " [" + info.stateName() + "]")
                    .setAttribute(AttributeKey.longKey("osgi.bundle.id"), info.bundleId())
                    .setAttribute(AttributeKey.stringKey("osgi.bundle.symbolic_name"),
                        info.symbolicName())
                    .setAttribute(AttributeKey.stringKey("osgi.bundle.version"), info.version())
                    .setAttribute(AttributeKey.stringKey("osgi.bundle.state"), info.stateName())
                    .setAttribute(AttributeKey.stringKey("osgi.bundle.location"), info.location())
                    .emit();
            }

            LOG.info("BundleInventoryComponent activated — logged " + bundles.length + " bundles");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to log bundle inventory", e);
        }
    }
}
