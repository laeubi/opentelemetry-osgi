package io.opentelemetry.osgi.runtime;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter;

/**
 * OSGi Declarative Services component that creates and publishes an
 * {@link OpenTelemetry} SDK instance as an OSGi service.
 * <p>
 * The service is configurable via OSGi ConfigAdmin. When configuration changes,
 * the SDK is rebuilt and the service is updated.
 */
@Component(
    service = OpenTelemetry.class,
    configurationPid = "io.opentelemetry.osgi.runtime",
    immediate = true
)
public class OpenTelemetryService implements OpenTelemetry {

    private static final Logger LOG = Logger.getLogger(OpenTelemetryService.class.getName());

    private volatile OpenTelemetrySdk sdk;

    @Activate
    public void activate(BundleContext context, OpenTelemetryConfiguration config) {
        LOG.info("Activating OpenTelemetry SDK service");
        this.sdk = buildSdk(context, config);
        LOG.info("OpenTelemetry SDK service activated with service.name=" + config.serviceName());
    }

    @Modified
    public void modified(BundleContext context, OpenTelemetryConfiguration config) {
        LOG.info("Reconfiguring OpenTelemetry SDK service");
        OpenTelemetrySdk oldSdk = this.sdk;
        this.sdk = buildSdk(context, config);
        if (oldSdk != null) {
            oldSdk.close();
        }
        LOG.info("OpenTelemetry SDK service reconfigured");
    }

    @Deactivate
    public void deactivate() {
        LOG.info("Deactivating OpenTelemetry SDK service");
        if (sdk != null) {
            sdk.close();
            sdk = null;
        }
    }

    private OpenTelemetrySdk buildSdk(BundleContext context, OpenTelemetryConfiguration config) {
        Resource resource = buildResource(context, config);

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
            .build();

        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(PeriodicMetricReader.create(LoggingMetricExporter.create()))
            .build();

        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(SystemOutLogRecordExporter.create()))
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(meterProvider)
            .setLoggerProvider(loggerProvider)
            .build();
    }

    private Resource buildResource(BundleContext context, OpenTelemetryConfiguration config) {
        AttributesBuilder attrs = Attributes.builder()
            .put(AttributeKey.stringKey("service.name"), config.serviceName())
            .put(AttributeKey.stringKey("service.version"), config.serviceVersion());

        if (!config.serviceNamespace().isEmpty()) {
            attrs.put(AttributeKey.stringKey("service.namespace"), config.serviceNamespace());
        }

        // Add OSGi framework information as resource attributes
        attrs.put(AttributeKey.stringKey("osgi.framework.vendor"),
            context.getProperty("org.osgi.framework.vendor"));
        attrs.put(AttributeKey.stringKey("osgi.framework.version"),
            context.getProperty("org.osgi.framework.version"));
        attrs.put(AttributeKey.stringKey("osgi.framework.uuid"),
            context.getProperty("org.osgi.framework.uuid"));

        // Parse additional resource attributes (format: "key=value")
        for (String attr : config.additionalResourceAttributes()) {
            int eq = attr.indexOf('=');
            if (eq > 0) {
                String key = attr.substring(0, eq).trim();
                String value = attr.substring(eq + 1).trim();
                attrs.put(AttributeKey.stringKey(key), value);
            }
        }

        return Resource.getDefault().merge(Resource.create(attrs.build()));
    }

    // Delegate all OpenTelemetry methods to the underlying SDK

    @Override
    public io.opentelemetry.api.trace.TracerProvider getTracerProvider() {
        return getSdk().getTracerProvider();
    }

    @Override
    public io.opentelemetry.api.metrics.MeterProvider getMeterProvider() {
        return getSdk().getMeterProvider();
    }

    @Override
    public io.opentelemetry.api.logs.LoggerProvider getLogsBridge() {
        return getSdk().getLogsBridge();
    }

    @Override
    public io.opentelemetry.context.propagation.ContextPropagators getPropagators() {
        return getSdk().getPropagators();
    }

    private OpenTelemetrySdk getSdk() {
        OpenTelemetrySdk current = sdk;
        if (current == null) {
            LOG.log(Level.WARNING, "OpenTelemetry SDK not yet initialized, returning noop");
            return (OpenTelemetrySdk) OpenTelemetrySdk.builder().build();
        }
        return current;
    }
}
