package io.opentelemetry.osgi.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Provides access to OSGi framework information for use by agent extension components.
 * <p>
 * This class encapsulates all direct OSGi API access, allowing the agent extension
 * to gracefully handle scenarios where OSGi is not available at class load time.
 * It obtains a {@link BundleContext} via {@link FrameworkUtil} and provides
 * convenience methods for querying framework state.
 */
class OsgiFrameworkAccess {

    private final BundleContext context;

    OsgiFrameworkAccess() {
        Bundle bundle = FrameworkUtil.getBundle(OsgiFrameworkAccess.class);
        if (bundle != null) {
            this.context = bundle.getBundleContext();
        } else {
            this.context = null;
        }
    }

    boolean isFrameworkRunning() {
        return context != null;
    }

    String getFrameworkVendor() {
        if (context == null) return "unknown";
        String vendor = context.getProperty("org.osgi.framework.vendor");
        return vendor != null ? vendor : "unknown";
    }

    String getFrameworkVersion() {
        if (context == null) return "unknown";
        String version = context.getProperty("org.osgi.framework.version");
        return version != null ? version : "unknown";
    }

    long getBundleCount() {
        if (context == null) return 0;
        return context.getBundles().length;
    }

    long getActiveBundleCount() {
        if (context == null) return 0;
        long count = 0;
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getState() == Bundle.ACTIVE) {
                count++;
            }
        }
        return count;
    }

    long getServiceCount() {
        if (context == null) return 0;
        try {
            ServiceReference<?>[] refs = context.getAllServiceReferences(null, null);
            return refs != null ? refs.length : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    List<String> getBundleSymbolicNames() {
        if (context == null) return Collections.emptyList();
        Bundle[] bundles = context.getBundles();
        List<String> names = new ArrayList<>(bundles.length);
        for (Bundle bundle : bundles) {
            String name = bundle.getSymbolicName();
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Returns a snapshot of all bundle states as a list of {@link BundleInfo} records.
     */
    List<BundleInfo> getBundleInfos() {
        if (context == null) return Collections.emptyList();
        Bundle[] bundles = context.getBundles();
        List<BundleInfo> infos = new ArrayList<>(bundles.length);
        for (Bundle bundle : bundles) {
            infos.add(new BundleInfo(
                bundle.getBundleId(),
                bundle.getSymbolicName(),
                bundle.getVersion().toString(),
                bundle.getState(),
                bundleStateToString(bundle.getState()),
                bundle.getLocation()
            ));
        }
        return infos;
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
