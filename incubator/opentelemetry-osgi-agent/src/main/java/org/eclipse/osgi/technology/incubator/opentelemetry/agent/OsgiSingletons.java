package org.eclipse.osgi.technology.incubator.opentelemetry.agent;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Static holder for shared OTel instances and framework state.
 *
 * <p>This class is automatically injected as a helper class into the
 * target classloader by the OTel javaagent. It uses
 * {@link GlobalOpenTelemetry#get()} which is available on the bootstrap
 * classloader (placed there by the javaagent).
 *
 * <p>Called from {@code @Advice} code in the instrumentation classes.
 */
public final class OsgiSingletons {

    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.osgi-framework";
    private static final String VERSION = "0.1.0";

    private static volatile BundleContext frameworkContext;
    private static volatile boolean metricsRegistered;

    private OsgiSingletons() {}

    public static Tracer tracer() {
        return GlobalOpenTelemetry.get().getTracer(INSTRUMENTATION_SCOPE, VERSION);
    }

    /**
     * Called from {@link FrameworkInstrumentation} when Framework.init() completes.
     * Captures the system BundleContext and registers framework metrics.
     */
    public static void onFrameworkInit(BundleContext context) {
        frameworkContext = context;
        if (!metricsRegistered) {
            metricsRegistered = true;
            registerMetrics();
        }
    }

    /**
     * Called from {@link BundleLifecycleInstrumentation} when the system bundle (id 0) starts.
     * Logs the full bundle inventory at that point.
     */
    public static void logBundleInventory(BundleContext context) {
        try {
            Logger logger = GlobalOpenTelemetry.get()
                    .getLogsBridge()
                    .loggerBuilder(INSTRUMENTATION_SCOPE)
                    .build();

            Bundle[] bundles = context.getBundles();
            if (bundles == null) {
                return;
            }

            logger.logRecordBuilder()
                    .setSeverity(Severity.INFO)
                    .setBody("OSGi framework started with " + bundles.length + " bundles")
                    .setAttribute(AttributeKey.longKey("osgi.framework.bundle.count"),
                            (long) bundles.length)
                    .emit();

            for (Bundle bundle : bundles) {
                logger.logRecordBuilder()
                        .setSeverity(Severity.INFO)
                        .setBody("Bundle: " + bundle.getSymbolicName()
                                + " [" + bundle.getBundleId() + "] "
                                + bundle.getVersion()
                                + " — " + bundleStateToString(bundle.getState()))
                        .setAttribute(AttributeKey.longKey("osgi.bundle.id"),
                                bundle.getBundleId())
                        .setAttribute(AttributeKey.stringKey("osgi.bundle.symbolic_name"),
                                bundle.getSymbolicName() != null
                                        ? bundle.getSymbolicName() : "unknown")
                        .setAttribute(AttributeKey.stringKey("osgi.bundle.version"),
                                bundle.getVersion().toString())
                        .setAttribute(AttributeKey.stringKey("osgi.bundle.state"),
                                bundleStateToString(bundle.getState()))
                        .emit();
            }
        } catch (Exception ignored) {
            // Advice helper: never propagate exceptions
        }
    }

    private static void registerMetrics() {
        try {
            Meter meter = GlobalOpenTelemetry.get()
                    .getMeter(INSTRUMENTATION_SCOPE);

            meter.gaugeBuilder("osgi.framework.bundle.count")
                    .setDescription("Total number of installed OSGi bundles")
                    .setUnit("{bundles}")
                    .ofLongs()
                    .buildWithCallback(measurement -> {
                        BundleContext ctx = frameworkContext;
                        if (ctx != null) {
                            try {
                                Bundle[] bundles = ctx.getBundles();
                                if (bundles != null) {
                                    measurement.record(bundles.length);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    });

            meter.gaugeBuilder("osgi.framework.bundle.active.count")
                    .setDescription("Number of active OSGi bundles")
                    .setUnit("{bundles}")
                    .ofLongs()
                    .buildWithCallback(measurement -> {
                        BundleContext ctx = frameworkContext;
                        if (ctx != null) {
                            try {
                                Bundle[] bundles = ctx.getBundles();
                                if (bundles != null) {
                                    long active = 0;
                                    for (Bundle b : bundles) {
                                        if (b.getState() == Bundle.ACTIVE) {
                                            active++;
                                        }
                                    }
                                    measurement.record(active);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    });

            meter.gaugeBuilder("osgi.framework.service.count")
                    .setDescription("Total number of registered OSGi services")
                    .setUnit("{services}")
                    .ofLongs()
                    .buildWithCallback(measurement -> {
                        BundleContext ctx = frameworkContext;
                        if (ctx != null) {
                            try {
                                ServiceReference<?>[] refs =
                                        ctx.getAllServiceReferences(null, null);
                                measurement.record(refs != null ? refs.length : 0);
                            } catch (Exception ignored) {
                            }
                        }
                    });

            meter.gaugeBuilder("osgi.framework.bundle.resolved.count")
                    .setDescription("Number of resolved OSGi bundles")
                    .setUnit("{bundles}")
                    .ofLongs()
                    .buildWithCallback(measurement -> {
                        BundleContext ctx = frameworkContext;
                        if (ctx != null) {
                            try {
                                Bundle[] bundles = ctx.getBundles();
                                if (bundles != null) {
                                    long resolved = 0;
                                    for (Bundle b : bundles) {
                                        if (b.getState() == Bundle.RESOLVED) {
                                            resolved++;
                                        }
                                    }
                                    measurement.record(resolved);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    });
        } catch (Exception ignored) {
            // Advice helper: never propagate exceptions
        }
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
