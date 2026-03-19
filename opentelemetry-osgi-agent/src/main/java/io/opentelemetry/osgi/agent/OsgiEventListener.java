package io.opentelemetry.osgi.agent;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

/**
 * Listens to OSGi framework events and creates OpenTelemetry spans for them.
 * <p>
 * This listener tracks:
 * <ul>
 *   <li>Bundle lifecycle events (INSTALLED, STARTED, STOPPED, UPDATED, UNINSTALLED)</li>
 *   <li>Service registration events (REGISTERED, UNREGISTERING, MODIFIED)</li>
 * </ul>
 * <p>
 * Each event generates a span with rich attributes describing the bundle or service
 * involved, making OSGi framework activity visible in distributed traces.
 */
class OsgiEventListener implements BundleListener, ServiceListener {

    private static final Logger LOG = Logger.getLogger(OsgiEventListener.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.osgi.agent.events";

    private final Tracer tracer;

    OsgiEventListener(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE, "0.1.0");
    }

    /**
     * Registers this listener with the OSGi framework.
     *
     * @return true if registration was successful, false if OSGi is not available
     */
    boolean register() {
        try {
            Bundle bundle = FrameworkUtil.getBundle(OsgiEventListener.class);
            if (bundle == null) {
                LOG.info("OSGi framework not available - event listener not registered");
                return false;
            }
            BundleContext context = bundle.getBundleContext();
            if (context == null) {
                LOG.info("Bundle context not available - event listener not registered");
                return false;
            }
            context.addBundleListener(this);
            context.addServiceListener(this);
            LOG.info("OSGi event listener registered for bundle and service events");
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to register OSGi event listener", e);
            return false;
        }
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        String eventType = bundleEventTypeToString(event.getType());

        Span span = tracer.spanBuilder("osgi.bundle." + eventType.toLowerCase())
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute(AttributeKey.stringKey("osgi.event.type"), eventType)
            .setAttribute(AttributeKey.stringKey("osgi.bundle.symbolic_name"),
                bundle.getSymbolicName() != null ? bundle.getSymbolicName() : "null")
            .setAttribute(AttributeKey.longKey("osgi.bundle.id"), bundle.getBundleId())
            .setAttribute(AttributeKey.stringKey("osgi.bundle.version"),
                bundle.getVersion().toString())
            .setAttribute(AttributeKey.stringKey("osgi.bundle.state"),
                OsgiFrameworkAccess.bundleStateToString(bundle.getState()))
            .setAttribute(AttributeKey.stringKey("osgi.bundle.location"), bundle.getLocation())
            .startSpan();

        span.addEvent("Bundle event: " + eventType + " - " + bundle.getSymbolicName());
        span.end();
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference<?> ref = event.getServiceReference();
        String eventType = serviceEventTypeToString(event.getType());
        String[] objectClass = (String[]) ref.getProperty("objectClass");
        String serviceInterfaces = objectClass != null ? String.join(", ", objectClass) : "unknown";

        Bundle providingBundle = ref.getBundle();

        Span span = tracer.spanBuilder("osgi.service." + eventType.toLowerCase())
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute(AttributeKey.stringKey("osgi.event.type"), eventType)
            .setAttribute(AttributeKey.stringKey("osgi.service.interfaces"), serviceInterfaces)
            .setAttribute(AttributeKey.longKey("osgi.service.id"),
                (Long) ref.getProperty("service.id"))
            .setAttribute(AttributeKey.stringKey("osgi.service.scope"),
                ref.getProperty("service.scope") != null
                    ? ref.getProperty("service.scope").toString() : "singleton")
            .startSpan();

        if (providingBundle != null) {
            span.setAttribute(AttributeKey.stringKey("osgi.service.provider.bundle"),
                providingBundle.getSymbolicName());
            span.setAttribute(AttributeKey.longKey("osgi.service.provider.bundle_id"),
                providingBundle.getBundleId());
        }

        span.addEvent("Service event: " + eventType + " - " + serviceInterfaces);
        span.end();
    }

    private static String bundleEventTypeToString(int type) {
        return switch (type) {
            case BundleEvent.INSTALLED -> "INSTALLED";
            case BundleEvent.STARTED -> "STARTED";
            case BundleEvent.STOPPED -> "STOPPED";
            case BundleEvent.UPDATED -> "UPDATED";
            case BundleEvent.UNINSTALLED -> "UNINSTALLED";
            case BundleEvent.RESOLVED -> "RESOLVED";
            case BundleEvent.UNRESOLVED -> "UNRESOLVED";
            case BundleEvent.STARTING -> "STARTING";
            case BundleEvent.STOPPING -> "STOPPING";
            case BundleEvent.LAZY_ACTIVATION -> "LAZY_ACTIVATION";
            default -> "UNKNOWN(" + type + ")";
        };
    }

    private static String serviceEventTypeToString(int type) {
        return switch (type) {
            case ServiceEvent.REGISTERED -> "REGISTERED";
            case ServiceEvent.UNREGISTERING -> "UNREGISTERING";
            case ServiceEvent.MODIFIED -> "MODIFIED";
            case ServiceEvent.MODIFIED_ENDMATCH -> "MODIFIED_ENDMATCH";
            default -> "UNKNOWN(" + type + ")";
        };
    }
}
