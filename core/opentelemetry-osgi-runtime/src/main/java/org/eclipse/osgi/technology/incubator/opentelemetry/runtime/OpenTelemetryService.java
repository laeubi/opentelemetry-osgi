package org.eclipse.osgi.technology.incubator.opentelemetry.runtime;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * OSGi Declarative Services component that creates and publishes an
 * {@link OpenTelemetry} SDK instance as an OSGi service.
 * <p>
 * The service is configurable via OSGi ConfigAdmin. When configuration changes,
 * the SDK is rebuilt and the service is updated.
 * <p>
 * Supported exporter types:
 * <ul>
 *   <li>{@code logging} (default) — exports telemetry to stdout via java.util.logging</li>
 *   <li>{@code otlp} — exports telemetry via OTLP/HTTP to a collector endpoint</li>
 * </ul>
 */
@Component(
    service = OpenTelemetry.class,
    configurationPid = OpenTelemetryConfiguration.PID,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class OpenTelemetryService implements OpenTelemetry {

	private static final Logger LOG = Logger.getLogger(OpenTelemetryService.class.getName());

    private volatile OpenTelemetrySdk sdk;

    @Activate
    public void activate(BundleContext context, OpenTelemetryConfiguration config) {
        LOG.info("Activating OpenTelemetry SDK service");
        this.sdk = buildSdk(context, config);
        LOG.info("OpenTelemetry SDK service activated with service.name=" + config.serviceName()
            + ", exporter=" + resolveExporterType(config)
            + ", endpoint=" + resolveOtlpEndpoint(config));
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
        String exporterType = resolveExporterType(config);

        SdkTracerProvider tracerProvider = buildTracerProvider(resource, exporterType, config);
        SdkMeterProvider meterProvider = buildMeterProvider(resource, exporterType, config);
        SdkLoggerProvider loggerProvider = buildLoggerProvider(resource, exporterType, config);

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(meterProvider)
            .setLoggerProvider(loggerProvider)
            .build();
    }

    private SdkTracerProvider buildTracerProvider(Resource resource, String exporterType,
            OpenTelemetryConfiguration config) {
        SpanExporter spanExporter;
        if ("otlp".equals(exporterType)) {
            String endpoint = resolveOtlpEndpoint(config);
            spanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint + "/v1/traces")
                .build();
            return SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();
        }
        spanExporter = LoggingSpanExporter.create();
        return SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();
    }

    private SdkMeterProvider buildMeterProvider(Resource resource, String exporterType,
            OpenTelemetryConfiguration config) {
        MetricExporter metricExporter;
        if ("otlp".equals(exporterType)) {
            String endpoint = resolveOtlpEndpoint(config);
            metricExporter = OtlpHttpMetricExporter.builder()
                .setEndpoint(endpoint + "/v1/metrics")
                .build();
        } else {
            metricExporter = LoggingMetricExporter.create();
        }
        return SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(PeriodicMetricReader.create(metricExporter))
            .build();
    }

    private SdkLoggerProvider buildLoggerProvider(Resource resource, String exporterType,
            OpenTelemetryConfiguration config) {
        LogRecordExporter logExporter;
        if ("otlp".equals(exporterType)) {
            String endpoint = resolveOtlpEndpoint(config);
            logExporter = OtlpHttpLogRecordExporter.builder()
                .setEndpoint(endpoint + "/v1/logs")
                .build();
            return SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
                .build();
        }
        logExporter = SystemOutLogRecordExporter.create();
        return SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
            .build();
    }

    /**
     * Resolves the exporter type from config or environment.
     * The {@code OTEL_EXPORTER_OTLP_ENDPOINT} env var forces OTLP mode.
     */
    private String resolveExporterType(OpenTelemetryConfiguration config) {
        String envEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (envEndpoint != null && !envEndpoint.isEmpty()) {
            return "otlp";
        }
        return config.exporterType();
    }

    /**
     * Resolves the OTLP endpoint from environment or config.
     * The {@code OTEL_EXPORTER_OTLP_ENDPOINT} env var takes precedence over the
     * ConfigAdmin configuration.
     */
    private String resolveOtlpEndpoint(OpenTelemetryConfiguration config) {
        String envEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (envEndpoint != null && !envEndpoint.isEmpty()) {
            return envEndpoint;
        }
        return config.otlpEndpoint();
    }

    private Resource buildResource(BundleContext context, OpenTelemetryConfiguration config) {
        String envServiceName = System.getenv("OTEL_SERVICE_NAME");

        AttributesBuilder attrs = Attributes.builder()
            .put(AttributeKey.stringKey("service.name"),
                envServiceName != null ? envServiceName : config.serviceName())
            .put(AttributeKey.stringKey("service.version"), config.serviceVersion());

        if (!config.serviceNamespace().isEmpty()) {
            attrs.put(AttributeKey.stringKey("service.namespace"), config.serviceNamespace());
        }

        // Add OSGi framework information as resource attributes
        String vendor = context.getProperty("org.osgi.framework.vendor");
        if (vendor != null) {
            attrs.put(AttributeKey.stringKey("osgi.framework.vendor"), vendor);
        }
        String version = context.getProperty("org.osgi.framework.version");
        if (version != null) {
            attrs.put(AttributeKey.stringKey("osgi.framework.version"), version);
        }
        String uuid = context.getProperty("org.osgi.framework.uuid");
        if (uuid != null) {
            attrs.put(AttributeKey.stringKey("osgi.framework.uuid"), uuid);
        }

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
            return OpenTelemetrySdk.builder().build();
        }
        return current;
    }
}
