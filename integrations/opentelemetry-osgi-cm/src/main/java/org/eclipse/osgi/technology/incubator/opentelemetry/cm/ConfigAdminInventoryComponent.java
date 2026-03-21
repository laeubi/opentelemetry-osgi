package org.eclipse.osgi.technology.incubator.opentelemetry.cm;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;

/**
 * Emits OpenTelemetry log records for all existing OSGi configurations,
 * providing a detailed inventory of the Configuration Admin state.
 * <p>
 * On activation, this component queries {@link ConfigurationAdmin#listConfigurations}
 * and emits structured log records containing:
 * <ul>
 *   <li>Configuration PID and factory PID</li>
 *   <li>Bundle location binding</li>
 *   <li>Number of properties</li>
 *   <li>Property keys (values are omitted for security)</li>
 * </ul>
 */
@Component(immediate = true)
public class ConfigAdminInventoryComponent {

    private static final Logger LOG = Logger.getLogger(ConfigAdminInventoryComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.cm.inventory";

    @Reference
    private OpenTelemetry openTelemetry;

    @Reference
    private ConfigurationAdmin configAdmin;

    @Activate
    public void activate() {
        LOG.info("ConfigAdminInventoryComponent activated — logging configuration inventory");

        try {
            io.opentelemetry.api.logs.Logger otelLogger =
                openTelemetry.getLogsBridge().loggerBuilder(INSTRUMENTATION_SCOPE)
                    .setInstrumentationVersion("0.1.0")
                    .build();

            Configuration[] configs = configAdmin.listConfigurations(null);
            int total = configs != null ? configs.length : 0;

            long factoryCount = 0;
            if (configs != null) {
                for (Configuration config : configs) {
                    if (config.getFactoryPid() != null) {
                        factoryCount++;
                    }
                }
            }

            // Summary log
            otelLogger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setBody("Configuration inventory: " + total + " configurations ("
                    + factoryCount + " factory, " + (total - factoryCount) + " singleton)")
                .setAttribute(AttributeKey.longKey("cm.configuration.count"), (long) total)
                .setAttribute(AttributeKey.longKey("cm.factory.count"), factoryCount)
                .setAttribute(AttributeKey.longKey("cm.singleton.count"), (long) total - factoryCount)
                .emit();

            // Detailed per-configuration logs
            if (configs != null) {
                for (Configuration config : configs) {
                    emitConfigLog(otelLogger, config);
                }
            }

            LOG.info("ConfigAdminInventoryComponent — emitted " + total + " configuration log records");
        } catch (IOException | org.osgi.framework.InvalidSyntaxException e) {
            LOG.log(Level.WARNING, "Failed to emit configuration inventory", e);
        }
    }

    @Deactivate
    public void deactivate() {
        LOG.info("ConfigAdminInventoryComponent deactivated");
    }

    private void emitConfigLog(io.opentelemetry.api.logs.Logger otelLogger, Configuration config) {
        String pid = config.getPid();
        boolean isFactory = config.getFactoryPid() != null;

        var builder = otelLogger.logRecordBuilder()
            .setSeverity(Severity.INFO)
            .setBody("Configuration: " + pid + (isFactory ? " [factory:" + config.getFactoryPid() + "]" : ""))
            .setAttribute(AttributeKey.stringKey("cm.pid"), pid)
            .setAttribute(AttributeKey.booleanKey("cm.is_factory"), isFactory);

        if (config.getFactoryPid() != null) {
            builder.setAttribute(AttributeKey.stringKey("cm.factory.pid"), config.getFactoryPid());
        }

        if (config.getBundleLocation() != null) {
            builder.setAttribute(AttributeKey.stringKey("cm.bundle.location"), config.getBundleLocation());
        }

        // Log property keys (not values — security)
        Dictionary<String, Object> properties = config.getProperties();
        if (properties != null) {
            builder.setAttribute(AttributeKey.longKey("cm.property.count"), (long) properties.size());
            StringBuilder keys = new StringBuilder();
            Enumeration<String> keyEnum = properties.keys();
            while (keyEnum.hasMoreElements()) {
                if (!keys.isEmpty()) {
                    keys.append(", ");
                }
                keys.append(keyEnum.nextElement());
            }
            builder.setAttribute(AttributeKey.stringKey("cm.property.keys"), keys.toString());
        }

        builder.emit();
    }
}
