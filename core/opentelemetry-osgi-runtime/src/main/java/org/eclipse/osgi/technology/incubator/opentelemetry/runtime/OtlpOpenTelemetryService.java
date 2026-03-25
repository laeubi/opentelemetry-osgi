package org.eclipse.osgi.technology.incubator.opentelemetry.runtime;

import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

/**
 * OpenTelemetry SDK service that exports telemetry via OTLP/HTTP to an
 * OpenTelemetry Collector or compatible backend.
 * <p>
 * This service has a higher service ranking than the logging exporter,
 * ensuring it is preferred by consumers when both are active.
 * <p>
 * The OTLP endpoint can be overridden via the {@code OTEL_EXPORTER_OTLP_ENDPOINT}
 * environment variable.
 * Activates when a configuration with PID {@value OtlpOpenTelemetryConfiguration#PID}
 * exists.
 */
@Component(
    name = OtlpOpenTelemetryConfiguration.COMPONENT_NAME,
    service = OpenTelemetry.class,
    configurationPid = OtlpOpenTelemetryConfiguration.PID,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    immediate = true,
    property = "service.ranking:Integer=100"
)
public class OtlpOpenTelemetryService extends AbstractOpenTelemetryService {

    private static final Logger LOG = Logger.getLogger(OtlpOpenTelemetryService.class.getName());

    @Activate
    public void activate(BundleContext context, OtlpOpenTelemetryConfiguration config) {
        LOG.info("Activating OTLP OpenTelemetry service");
        setSdk(buildSdk(context, config));
        LOG.info("OTLP OpenTelemetry service activated with service.name="
            + resolveServiceName(config.serviceName()) + ", endpoint=" + resolveEndpoint(config));
    }

    @Modified
    public void modified(BundleContext context, OtlpOpenTelemetryConfiguration config) {
        LOG.info("Reconfiguring OTLP OpenTelemetry service");
        setSdk(buildSdk(context, config));
        LOG.info("OTLP OpenTelemetry service reconfigured");
    }

    @Deactivate
    public void deactivate() {
        LOG.info("Deactivating OTLP OpenTelemetry service");
        closeSdk();
    }

    private OpenTelemetrySdk buildSdk(BundleContext context, OtlpOpenTelemetryConfiguration config) {
        String endpoint = resolveEndpoint(config);
        Resource resource = buildResource(context, resolveServiceName(config.serviceName()),
            config.serviceVersion(), config.serviceNamespace(), config.additionalResourceAttributes());

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(BatchSpanProcessor.builder(
                OtlpHttpSpanExporter.builder()
                    .setEndpoint(endpoint + "/v1/traces")
                    .build()
            ).build())
            .build();

        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(PeriodicMetricReader.create(
                OtlpHttpMetricExporter.builder()
                    .setEndpoint(endpoint + "/v1/metrics")
                    .build()
            ))
            .build();

        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(BatchLogRecordProcessor.builder(
                OtlpHttpLogRecordExporter.builder()
                    .setEndpoint(endpoint + "/v1/logs")
                    .build()
            ).build())
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(meterProvider)
            .setLoggerProvider(loggerProvider)
            .build();
    }

    private String resolveEndpoint(OtlpOpenTelemetryConfiguration config) {
        String env = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        return (env != null && !env.isEmpty()) ? env : config.otlpEndpoint();
    }
}
