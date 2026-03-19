package io.opentelemetry.osgi.agent;

import java.util.Map;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

/**
 * An OpenTelemetry {@link ResourceProvider} that detects OSGi framework presence
 * and adds OSGi-specific resource attributes to all telemetry signals.
 * <p>
 * When the OpenTelemetry Java Agent is used with an OSGi application, this provider
 * automatically enriches telemetry with framework metadata such as vendor, version,
 * and the number of installed bundles.
 * <p>
 * Registered via SPI in {@code META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider}.
 */
public class OsgiResourceProvider implements ResourceProvider {

    @Override
    public Resource createResource(ConfigProperties config) {
        if (!isOsgiAvailable()) {
            return Resource.empty();
        }

        AttributesBuilder attrs = Attributes.builder();
        attrs.put(AttributeKey.stringKey("osgi.detected"), "true");

        try {
            OsgiFrameworkAccess access = new OsgiFrameworkAccess();

            if (access.isFrameworkRunning()) {
                attrs.put(AttributeKey.stringKey("osgi.framework.vendor"), access.getFrameworkVendor());
                attrs.put(AttributeKey.stringKey("osgi.framework.version"), access.getFrameworkVersion());
                attrs.put(AttributeKey.longKey("osgi.bundle.count"), access.getBundleCount());
                attrs.put(AttributeKey.longKey("osgi.bundle.active_count"), access.getActiveBundleCount());
                attrs.put(AttributeKey.longKey("osgi.service.count"), access.getServiceCount());

                // Add individual bundle names as a comma-separated list
                String bundleList = String.join(", ", access.getBundleSymbolicNames());
                if (!bundleList.isEmpty()) {
                    attrs.put(AttributeKey.stringKey("osgi.bundle.names"), bundleList);
                }
            }
        } catch (Exception e) {
            attrs.put(AttributeKey.stringKey("osgi.error"), e.getMessage());
        }

        return Resource.create(attrs.build());
    }

    private boolean isOsgiAvailable() {
        try {
            Class.forName("org.osgi.framework.Bundle");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
