# OpenTelemetry OSGi Runtime

Provides a configured [OpenTelemetry](https://opentelemetry.io/) SDK instance as an OSGi service using Declarative Services.
This is the core module that all other integration and demo modules depend on.

## Features

- Creates and manages the OpenTelemetry SDK lifecycle
- Publishes `io.opentelemetry.api.OpenTelemetry` to the OSGi service registry
- Registers individual provider interfaces as separate OSGi services for direct consumption:
  - `io.opentelemetry.api.trace.TracerProvider`
  - `io.opentelemetry.api.metrics.MeterProvider`
  - `io.opentelemetry.api.logs.LoggerProvider`
  - `io.opentelemetry.context.propagation.ContextPropagators`
- Supports OTLP/HTTP and logging exporters
- Auto-detects OTLP mode from `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable

## Configuration

PID: `org.eclipse.osgi.technology.incubator.opentelemetry.runtime`

| Property | Default | Description |
|---|---|---|
| `serviceName` | `osgi-application` | Service name in telemetry data |
| `serviceVersion` | `0.1.0` | Service version resource attribute |
| `exporterType` | `logging` | `logging` (stdout) or `otlp` (OTLP/HTTP) |
| `otlpEndpoint` | `http://localhost:4318` | OTLP/HTTP collector endpoint |
| `additionalResourceAttributes` | (empty) | Extra key=value resource attributes |

When the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable is set, the runtime automatically switches to OTLP export mode and uses its value as the endpoint.

## Resource Attributes

The runtime automatically enriches all telemetry with OSGi framework information as [OpenTelemetry Resource](https://opentelemetry.io/docs/concepts/resources/) attributes.
Resource attributes are attached to **every** trace, metric, and log record — they are the primary mechanism for identifying and correlating telemetry from different service instances.

| Attribute | Source | Description |
|---|---|---|
| `service.name` | `OTEL_SERVICE_NAME` env or `serviceName` config | Identifies the logical service |
| `service.version` | `serviceVersion` config | Service version |
| `service.namespace` | `serviceNamespace` config | Logical grouping (optional) |
| `service.instance.id` | `org.osgi.framework.uuid` | Unique instance identifier (framework UUID) |
| `osgi.framework.uuid` | `org.osgi.framework.uuid` | OSGi framework UUID |
| `osgi.framework.vendor` | `org.osgi.framework.vendor` | Framework implementation vendor |
| `osgi.framework.version` | `org.osgi.framework.version` | Framework specification version |

The `service.instance.id` is the standard OpenTelemetry semantic convention for distinguishing multiple instances of the same service.
It is automatically set to the OSGi framework UUID, which is unique per framework launch.
In Prometheus, `service.name` maps to the `job` label and `service.instance.id` maps to the `instance` label.

Additional resource attributes can be configured via `additionalResourceAttributes` (format: `key=value`).

## Components

| Class | Description |
|---|---|
| `OpenTelemetryService` | DS component creating and publishing the `OpenTelemetry` SDK instance |
| `OpenTelemetryProviderRegistration` | DS component registering individual provider services |
| `OpenTelemetryConfiguration` | ConfigAdmin configuration annotation |

## Usage

Other bundles consume the service via Declarative Services:

```java
@Reference
private OpenTelemetry openTelemetry;

// Or consume individual providers directly:
@Reference
private TracerProvider tracerProvider;
```
