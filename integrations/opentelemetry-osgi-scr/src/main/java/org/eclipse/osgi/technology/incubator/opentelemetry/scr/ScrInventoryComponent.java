package org.eclipse.osgi.technology.incubator.opentelemetry.scr;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;

/**
 * Emits an OpenTelemetry log record for every DS component configuration,
 * providing a detailed inventory of the Declarative Services landscape.
 * <p>
 * On activation, this component queries the {@link ServiceComponentRuntime}
 * and emits structured log records containing:
 * <ul>
 *   <li>Component name, implementation class, and state</li>
 *   <li>Declaring bundle symbolic name and id</li>
 *   <li>Service interfaces provided</li>
 *   <li>Configuration policy and PIDs</li>
 *   <li>Satisfied and unsatisfied references</li>
 *   <li>Failure information for components in FAILED_ACTIVATION state</li>
 * </ul>
 */
@Component(immediate = true)
public class ScrInventoryComponent {

    private static final Logger LOG = Logger.getLogger(ScrInventoryComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.scr.inventory";

    @Reference
    private OpenTelemetry openTelemetry;

    @Reference
    private ServiceComponentRuntime scr;

    @Activate
    public void activate() {
        LOG.info("ScrInventoryComponent activated — logging DS component inventory");

        io.opentelemetry.api.logs.Logger otelLogger =
            openTelemetry.getLogsBridge().loggerBuilder(INSTRUMENTATION_SCOPE)
                .setInstrumentationVersion("0.1.0")
                .build();

        Collection<ComponentDescriptionDTO> descriptions = scr.getComponentDescriptionDTOs();

        // Summary log
        long totalConfigs = 0;
        long activeConfigs = 0;
        for (ComponentDescriptionDTO desc : descriptions) {
            for (ComponentConfigurationDTO config : scr.getComponentConfigurationDTOs(desc)) {
                totalConfigs++;
                if (config.state == ComponentConfigurationDTO.ACTIVE) {
                    activeConfigs++;
                }
            }
        }

        otelLogger.logRecordBuilder()
            .setSeverity(Severity.INFO)
            .setBody("DS component inventory: " + descriptions.size() + " descriptions, "
                + totalConfigs + " configurations (" + activeConfigs + " active)")
            .setAttribute(AttributeKey.longKey("osgi.scr.description.count"), (long) descriptions.size())
            .setAttribute(AttributeKey.longKey("osgi.scr.configuration.count"), totalConfigs)
            .setAttribute(AttributeKey.longKey("osgi.scr.configuration.active"), activeConfigs)
            .emit();

        // Detailed per-component logs
        for (ComponentDescriptionDTO desc : descriptions) {
            Collection<ComponentConfigurationDTO> configs = scr.getComponentConfigurationDTOs(desc);

            for (ComponentConfigurationDTO config : configs) {
                String stateName = ScrMetricsComponent.configStateToString(config.state);
                Severity severity = config.state == ComponentConfigurationDTO.ACTIVE
                    ? Severity.INFO
                    : (config.state == 16 ? Severity.ERROR : Severity.WARN);

                var builder = otelLogger.logRecordBuilder()
                    .setSeverity(severity)
                    .setBody("DS component: " + desc.name + " [" + stateName + "]")
                    .setAttribute(AttributeKey.stringKey("osgi.scr.component.name"), desc.name)
                    .setAttribute(AttributeKey.stringKey("osgi.scr.component.class"), desc.implementationClass)
                    .setAttribute(AttributeKey.stringKey("osgi.scr.component.state"), stateName)
                    .setAttribute(AttributeKey.longKey("osgi.scr.component.config_id"), config.id)
                    .setAttribute(AttributeKey.booleanKey("osgi.scr.component.immediate"), desc.immediate)
                    .setAttribute(AttributeKey.booleanKey("osgi.scr.component.enabled"),
                        scr.isComponentEnabled(desc));

                if (desc.bundle != null) {
                    builder.setAttribute(AttributeKey.stringKey("osgi.scr.bundle.name"), desc.bundle.symbolicName);
                    builder.setAttribute(AttributeKey.longKey("osgi.scr.bundle.id"), desc.bundle.id);
                }

                if (desc.serviceInterfaces != null && desc.serviceInterfaces.length > 0) {
                    builder.setAttribute(AttributeKey.stringKey("osgi.scr.service.interfaces"),
                        String.join(", ", desc.serviceInterfaces));
                }

                if (desc.configurationPid != null && desc.configurationPid.length > 0) {
                    builder.setAttribute(AttributeKey.stringKey("osgi.scr.config.pid"),
                        String.join(", ", desc.configurationPid));
                }

                if (desc.configurationPolicy != null) {
                    builder.setAttribute(AttributeKey.stringKey("osgi.scr.config.policy"),
                        desc.configurationPolicy);
                }

                if (config.satisfiedReferences != null) {
                    builder.setAttribute(AttributeKey.longKey("osgi.scr.reference.satisfied_count"),
                        (long) config.satisfiedReferences.length);
                    for (SatisfiedReferenceDTO ref : config.satisfiedReferences) {
                        int boundCount = ref.boundServices != null ? ref.boundServices.length : 0;
                        builder.setAttribute(
                            AttributeKey.stringKey("osgi.scr.ref." + ref.name + ".bound"),
                            String.valueOf(boundCount));
                    }
                }

                if (config.unsatisfiedReferences != null && config.unsatisfiedReferences.length > 0) {
                    builder.setAttribute(AttributeKey.longKey("osgi.scr.reference.unsatisfied_count"),
                        (long) config.unsatisfiedReferences.length);
                    for (UnsatisfiedReferenceDTO ref : config.unsatisfiedReferences) {
                        builder.setAttribute(
                            AttributeKey.stringKey("osgi.scr.ref." + ref.name + ".target"),
                            ref.target != null ? ref.target : "(none)");
                    }
                }

                if (config.failure != null && !config.failure.isEmpty()) {
                    builder.setAttribute(AttributeKey.stringKey("osgi.scr.failure"), config.failure);
                }

                builder.emit();
            }
        }

        LOG.info("ScrInventoryComponent — emitted " + totalConfigs + " component log records");
    }

    @Deactivate
    public void deactivate() {
        LOG.info("ScrInventoryComponent deactivated");
    }
}
