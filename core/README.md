# OpenTelemetry OSGi Core

This folder contains the core runtime module that provides the OpenTelemetry SDK as an OSGi service.

## Modules

### opentelemetry-osgi-runtime

Provides a configured [OpenTelemetry](https://opentelemetry.io/) SDK instance as an OSGi service using Declarative Services.
The runtime creates and manages the OpenTelemetry SDK lifecycle, including:

- **Tracer Provider** with configurable span exporters
- **Meter Provider** with configurable metric exporters
- **Logger Provider** with configurable log exporters
- **OTLP and Logging exporters** selectable via ConfigAdmin or environment variables

The runtime publishes the `io.opentelemetry.api.OpenTelemetry` interface to the OSGi service registry.
Other bundles can consume it via `@Reference` to produce traces, metrics, and logs.

In addition, the individual provider interfaces are registered as separate OSGi services for direct consumption:

- `io.opentelemetry.api.trace.TracerProvider`
- `io.opentelemetry.api.metrics.MeterProvider`
- `io.opentelemetry.api.logs.LoggerProvider`
- `io.opentelemetry.context.propagation.ContextPropagators`

This allows consumers to depend on exactly the provider they need without fetching the full `OpenTelemetry` service first.

### Configuration

The runtime is configured via OSGi ConfigAdmin with PID `org.eclipse.osgi.technology.incubator.opentelemetry.runtime`:

| Property | Default | Description |
|---|---|---|
| `serviceName` | `osgi-application` | Service name in telemetry data |
| `serviceVersion` | `0.1.0` | Service version resource attribute |
| `exporterType` | `logging` | `logging` (stdout) or `otlp` (OTLP/gRPC) |
| `otlpEndpoint` | `http://localhost:4317` | OTLP collector endpoint |
| `additionalResourceAttributes` | (empty) | Extra key=value resource attributes |

When the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable is set, the runtime automatically switches to OTLP export mode.
