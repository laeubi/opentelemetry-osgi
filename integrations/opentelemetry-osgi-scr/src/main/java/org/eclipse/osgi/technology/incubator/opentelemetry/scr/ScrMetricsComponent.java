package org.eclipse.osgi.technology.incubator.opentelemetry.scr;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;

/**
 * Exposes OSGi Declarative Services component state as OpenTelemetry metrics.
 * <p>
 * Uses the SCR Introspection API ({@link ServiceComponentRuntime}) to query
 * component descriptions and configurations, then publishes asynchronous gauge
 * metrics reporting:
 * <ul>
 *   <li>{@code osgi.scr.component.count} — total number of registered component descriptions</li>
 *   <li>{@code osgi.scr.component.states} — number of component configurations per state
 *       (ACTIVE, SATISFIED, UNSATISFIED_REFERENCE, UNSATISFIED_CONFIGURATION, FAILED_ACTIVATION)</li>
 *   <li>{@code osgi.scr.component.active} — number of active component configurations</li>
 *   <li>{@code osgi.scr.reference.satisfied} — count of satisfied references across all components</li>
 *   <li>{@code osgi.scr.reference.unsatisfied} — count of unsatisfied references across all components</li>
 * </ul>
 */
@Component(immediate = true)
public class ScrMetricsComponent {

    private static final Logger LOG = Logger.getLogger(ScrMetricsComponent.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "org.eclipse.osgi.technology.incubator.opentelemetry.scr";

    @Reference
    private OpenTelemetry openTelemetry;

    @Reference
    private ServiceComponentRuntime scr;

    private ObservableLongGauge componentCountGauge;
    private ObservableLongGauge componentStatesGauge;
    private ObservableLongGauge activeGauge;
    private ObservableLongGauge satisfiedRefGauge;
    private ObservableLongGauge unsatisfiedRefGauge;

    @Activate
    public void activate() {
        LOG.info("ScrMetricsComponent activated — registering SCR metrics");
        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        componentCountGauge = meter.gaugeBuilder("osgi.scr.component.count")
            .setDescription("Total number of registered DS component descriptions")
            .setUnit("{components}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    measurement.record(scr.getComponentDescriptionDTOs().size());
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read component count", e);
                }
            });

        componentStatesGauge = meter.gaugeBuilder("osgi.scr.component.states")
            .setDescription("Number of DS component configurations per state")
            .setUnit("{components}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    Map<String, Long> stateCounts = new HashMap<>();
                    for (ComponentDescriptionDTO desc : scr.getComponentDescriptionDTOs()) {
                        for (ComponentConfigurationDTO config : scr.getComponentConfigurationDTOs(desc)) {
                            String stateName = configStateToString(config.state);
                            stateCounts.merge(stateName, 1L, Long::sum);
                        }
                    }
                    stateCounts.forEach((state, count) ->
                        measurement.record(count, Attributes.of(
                            AttributeKey.stringKey("osgi.scr.state"), state
                        ))
                    );
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read component states", e);
                }
            });

        activeGauge = meter.gaugeBuilder("osgi.scr.component.active")
            .setDescription("Number of active DS component configurations")
            .setUnit("{components}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    long active = 0;
                    for (ComponentDescriptionDTO desc : scr.getComponentDescriptionDTOs()) {
                        for (ComponentConfigurationDTO config : scr.getComponentConfigurationDTOs(desc)) {
                            if (config.state == ComponentConfigurationDTO.ACTIVE) {
                                active++;
                            }
                        }
                    }
                    measurement.record(active);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read active component count", e);
                }
            });

        satisfiedRefGauge = meter.gaugeBuilder("osgi.scr.reference.satisfied")
            .setDescription("Total number of satisfied service references across all components")
            .setUnit("{references}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    long count = 0;
                    for (ComponentDescriptionDTO desc : scr.getComponentDescriptionDTOs()) {
                        for (ComponentConfigurationDTO config : scr.getComponentConfigurationDTOs(desc)) {
                            if (config.satisfiedReferences != null) {
                                count += config.satisfiedReferences.length;
                            }
                        }
                    }
                    measurement.record(count);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read satisfied references", e);
                }
            });

        unsatisfiedRefGauge = meter.gaugeBuilder("osgi.scr.reference.unsatisfied")
            .setDescription("Total number of unsatisfied service references across all components")
            .setUnit("{references}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                try {
                    long count = 0;
                    for (ComponentDescriptionDTO desc : scr.getComponentDescriptionDTOs()) {
                        for (ComponentConfigurationDTO config : scr.getComponentConfigurationDTOs(desc)) {
                            if (config.unsatisfiedReferences != null) {
                                count += config.unsatisfiedReferences.length;
                            }
                        }
                    }
                    measurement.record(count);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Failed to read unsatisfied references", e);
                }
            });

        LOG.info("ScrMetricsComponent — SCR metrics registered");
    }

    @Deactivate
    public void deactivate() {
        closeQuietly(componentCountGauge);
        closeQuietly(componentStatesGauge);
        closeQuietly(activeGauge);
        closeQuietly(satisfiedRefGauge);
        closeQuietly(unsatisfiedRefGauge);
        LOG.info("ScrMetricsComponent deactivated");
    }

    static String configStateToString(int state) {
        return switch (state) {
            case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION -> "UNSATISFIED_CONFIGURATION";
            case ComponentConfigurationDTO.UNSATISFIED_REFERENCE -> "UNSATISFIED_REFERENCE";
            case ComponentConfigurationDTO.SATISFIED -> "SATISFIED";
            case ComponentConfigurationDTO.ACTIVE -> "ACTIVE";
            case 16 -> "FAILED_ACTIVATION"; // ComponentConfigurationDTO.FAILED_ACTIVATION (1.4+)
            default -> "UNKNOWN(" + state + ")";
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
