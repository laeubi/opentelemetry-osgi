# OpenTelemetry OSGi Runtime

Provides configured [OpenTelemetry](https://opentelemetry.io/) SDK instances as OSGi services using Declarative Services.
This is the core module that all other integration and demo modules depend on.

## Features

- Supports multiple exporter types, each as a separate service with its own configuration PID
- Publishes `io.opentelemetry.api.OpenTelemetry` to the OSGi service registry
- Registers individual provider interfaces as separate OSGi services for direct consumption:
  - `io.opentelemetry.api.trace.TracerProvider`
  - `io.opentelemetry.api.metrics.MeterProvider`
  - `io.opentelemetry.api.logs.LoggerProvider`
  - `io.opentelemetry.context.propagation.ContextPropagators`
- Provider sub-services include an `opentelemetry.name` property for targeting a specific exporter
- Multiple exporters can be active simultaneously (OTLP has higher service ranking)

## Exporter Types

| Exporter | Component Name | Configuration PID | Description |
|---|---|---|---|
| Logging | `logging-opentelemetry` | `...runtime.logging` | Exports to stdout via `java.util.logging` |
| OTLP | `otlp-opentelemetry` | `...runtime.otlp` | Exports via OTLP/HTTP to a collector |

Each exporter activates only when its configuration PID exists in ConfigAdmin (i.e. a `.cfg` file is placed in `${karaf.etc}/`).

## Configuration

### Logging Exporter

PID: `org.eclipse.osgi.technology.incubator.opentelemetry.runtime.logging`

| Property | Default | Description |
|---|---|---|
| `serviceName` | `osgi-application` | Service name in telemetry data |
| `serviceVersion` | `0.1.0` | Service version resource attribute |
| `serviceNamespace` | (empty) | Logical grouping (optional) |
| `additionalResourceAttributes` | (empty) | Extra key=value resource attributes |

### OTLP Exporter

PID: `org.eclipse.osgi.technology.incubator.opentelemetry.runtime.otlp`

| Property | Default | Description |
|---|---|---|
| `serviceName` | `osgi-application` | Service name in telemetry data |
| `serviceVersion` | `0.1.0` | Service version resource attribute |
| `serviceNamespace` | (empty) | Logical grouping (optional) |
| `otlpEndpoint` | `http://localhost:4318` | OTLP/HTTP collector endpoint |
| `additionalResourceAttributes` | (empty) | Extra key=value resource attributes |

The `OTEL_SERVICE_NAME` environment variable overrides `serviceName` for all exporters.
The `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable overrides `otlpEndpoint` for the OTLP exporter.

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
| `AbstractOpenTelemetryService` | Base class with resource building, SDK lifecycle, and delegation |
| `LoggingOpenTelemetryService` | DS component publishing `OpenTelemetry` with logging exporters |
| `OtlpOpenTelemetryService` | DS component publishing `OpenTelemetry` with OTLP/HTTP exporters |
| `OpenTelemetryProviderRegistration` | DS component registering individual provider services per exporter |
| `LoggingOpenTelemetryConfiguration` | ConfigAdmin annotation for the logging exporter |
| `OtlpOpenTelemetryConfiguration` | ConfigAdmin annotation for the OTLP exporter |

## Usage

Other bundles consume the service via Declarative Services:

```java
@Reference
private OpenTelemetry openTelemetry;

// Or consume individual providers directly:
@Reference
private TracerProvider tracerProvider;
```

### Targeting a Specific Exporter

When multiple exporters are active, consumers can target a specific one using the
`opentelemetry.name` service property:

```java
@Reference(target = "(opentelemetry.name=otlp-opentelemetry)")
private TracerProvider tracerProvider;
```

Without a target filter, consumers get the highest-ranked exporter (OTLP by default).

## Adding New Exporters

To add a new exporter type:

1. Create a configuration annotation with `COMPONENT_NAME` and `PID` constants
2. Create a service class extending `AbstractOpenTelemetryService`
3. Use `@Component(name = YourConfig.COMPONENT_NAME, configurationPid = YourConfig.PID, ...)`
4. Add any required exporter dependencies to `pom.xml` and the Karaf feature descriptor
