# OpenTelemetry OSGi Integration

Integration of [OpenTelemetry](https://opentelemetry.io/) with the [OSGi](https://www.osgi.org/) service platform.

## What is OpenTelemetry?

[OpenTelemetry](https://opentelemetry.io/) (OTel) is an open-source, vendor-neutral observability framework for generating, collecting, and exporting telemetry data.
It provides a unified set of APIs, SDKs, and tools to instrument applications and infrastructure, enabling deep visibility into distributed systems.

OpenTelemetry defines three core **signals**:

### Traces

Traces represent the end-to-end journey of a request through a distributed system.
A trace consists of one or more **spans**, each representing a unit of work (e.g. an HTTP request, a database query, or an OSGi service call).
Spans have:

- A name and duration (start/end timestamps)
- Parent-child relationships forming a directed acyclic graph
- Attributes (key-value pairs) describing the operation
- Events (timestamped annotations within a span)
- Status codes (OK, ERROR, UNSET)

The [`Tracer`](https://opentelemetry.io/docs/specs/otel/trace/api/#tracer) API creates and manages spans, while **context propagation** ensures trace continuity across process and service boundaries.

### Metrics

Metrics capture quantitative measurements about application behavior over time.
OpenTelemetry provides several metric **instruments**:

| Instrument | Description | Example |
|---|---|---|
| **Counter** | Monotonically increasing sum | Request count |
| **UpDownCounter** | Sum that can increase and decrease | Active connections |
| **Histogram** | Distribution of values | Request latency |
| **Gauge** | Instantaneous value (async callback) | CPU usage, memory |

The [`Meter`](https://opentelemetry.io/docs/specs/otel/metrics/api/#meter) API creates instruments, and metric data is periodically exported to backends like Prometheus or OTLP collectors.

### Logs

The [Log Bridge API](https://opentelemetry.io/docs/specs/otel/logs/bridge-api/) is designed to bridge existing logging frameworks (SLF4J, JUL, Log4j) into OpenTelemetry.
It is **not** a replacement logging API but provides:

- Structured log records with severity levels
- Automatic correlation with traces (trace ID, span ID)
- Attribute-rich log data for better searchability

### Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Application Code                       │
│  (Instrumented with OpenTelemetry API)                   │
├──────────────────────────────────────────────────────────┤
│                   OpenTelemetry API                       │
│  TracerProvider │ MeterProvider │ LoggerProvider          │
├──────────────────────────────────────────────────────────┤
│                   OpenTelemetry SDK                       │
│  SpanProcessors │ MetricReaders │ LogRecordProcessors    │
├──────────────────────────────────────────────────────────┤
│                      Exporters                            │
│  OTLP │ Logging │ Zipkin │ Prometheus │ Custom           │
└──────────────────────────────────────────────────────────┘
```

The API defines the interfaces that application code uses.
The SDK provides the implementation including processing pipelines and exporters.
This separation allows libraries to instrument against the API without coupling to a specific SDK.

## Project Scope

This project brings OpenTelemetry to the OSGi ecosystem through three independent modules:

### `opentelemetry-osgi-runtime`

An OSGi bundle that creates and configures an [`OpenTelemetrySdk`](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk/all/src/main/java/io/opentelemetry/sdk/OpenTelemetrySdk.java) instance and publishes it as an OSGi service implementing the [`OpenTelemetry`](https://github.com/open-telemetry/opentelemetry-java/blob/main/api/all/src/main/java/io/opentelemetry/api/OpenTelemetry.java) interface.

Key features:

- Managed as a Declarative Services (DS) component
- Configurable via OSGi ConfigAdmin (service name, exporter type, resource attributes)
- Enriches telemetry resources with OSGi framework metadata (vendor, version, UUID)
- Proper lifecycle management (SDK shutdown on deactivation)
- Supports reconfiguration without restart

Configuration properties (PID `io.opentelemetry.osgi.runtime`):

| Property | Default | Description |
|---|---|---|
| `serviceName` | `osgi-application` | The `service.name` resource attribute |
| `serviceVersion` | `0.1.0` | The `service.version` resource attribute |
| `serviceNamespace` | (empty) | Optional `service.namespace` |
| `exporterType` | `logging` | Exporter type (currently `logging`) |
| `additionalResourceAttributes` | (empty) | Extra attributes as `key=value` pairs |

### `opentelemetry-osgi-client`

A demo bundle that consumes the `OpenTelemetry` service via DS and demonstrates all three telemetry signals:

- **`TracingDemoComponent`** — Creates basic spans, nested parent-child spans, span attributes/events, and error recording
- **`MetricsDemoComponent`** — Demonstrates counters, up-down counters, histograms, and async gauges
- **`LogBridgeDemoComponent`** — Emits structured log records with OSGi bundle inventory data
- **`ContextPropagationDemoComponent`** — Shows context injection/extraction for cross-boundary trace propagation

### `opentelemetry-osgi-agent`

An OpenTelemetry Java Agent extension that makes OSGi framework internals visible as telemetry.
This module is independent from the runtime/client approach — it works with the [OpenTelemetry Java Agent](https://opentelemetry.io/docs/zero-code/java/agent/) and extends its auto-instrumentation capabilities as described in the [Agent Extension API](https://opentelemetry.io/docs/zero-code/java/agent/api/).

The extension provides:

- **`OsgiResourceProvider`** — Detects OSGi presence and adds framework metadata (vendor, version, bundle list) as resource attributes to all telemetry
- **`OsgiMetricsProvider`** — Registers async gauge metrics for bundle count, active bundles, service count, and per-state bundle distribution
- **`OsgiEventListener`** — Listens to `BundleEvent` and `ServiceEvent` and creates spans for each lifecycle change (install, start, stop, register, unregister)
- **`OsgiBundleInventoryLogger`** — Emits a complete bundle inventory as structured log records at startup

## Prerequisites

- Java 21 or later
- Maven 3.9+

## Building

```bash
mvn clean verify
```

This builds all three modules.
The agent extension module produces a shaded uber-JAR suitable for use as a Java Agent extension.

## Usage

### Runtime + Client (OSGi Service Approach)

Deploy `opentelemetry-osgi-runtime` and `opentelemetry-osgi-client` bundles into your OSGi container.
The runtime bundle will automatically register an `OpenTelemetry` service, and the client bundle's demo components will activate and produce telemetry.

### Agent Extension Approach

Use with the OpenTelemetry Java Agent:

```bash
java -javaagent:path/to/opentelemetry-javaagent.jar \
     -Dotel.javaagent.extensions=path/to/opentelemetry-osgi-agent-0.1.0-SNAPSHOT.jar \
     -jar your-osgi-application.jar
```

The agent extension will automatically detect the OSGi framework and begin instrumenting it.

## Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language runtime |
| OpenTelemetry Java | 1.49.0 | Observability framework |
| OSGi Framework | R8 (1.10.0) | Module system |
| OSGi Declarative Services | 1.5.1 | Component model |
| bnd-maven-plugin | 7.1.0 | OSGi metadata generation |

## License

This project is licensed under the [Eclipse Public License v2.0](LICENSE).
