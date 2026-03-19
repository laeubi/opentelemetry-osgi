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

## Docker Demo (Quick Start)

The easiest way to see the integration in action is the Docker Compose demo.
It spins up the full Grafana observability stack with a single command:

```
OSGi App (Felix + our bundles)
    │ OTLP/gRPC
    ▼
OTel Collector (Gateway)
    │
    ├──→ Tempo     (Traces)
    ├──→ Prometheus (Metrics)
    └──→ Loki      (Logs)
          │
          ▼
       Grafana (UI + Explore + Dashboards)
```

### Prerequisites

- Docker and Docker Compose installed
- Java 21 and Maven 3.9+ (for local builds)

### Start the Demo

```bash
# Build and start everything
docker compose up --build -d

# Watch the OSGi application logs
docker compose logs -f osgi-app
```

### Explore in Grafana

Open [http://localhost:3000](http://localhost:3000) (no login required — anonymous admin is enabled).

**Traces** — Go to *Explore → Tempo* and search for traces.
You will see spans for simulated OSGi operations like `osgi.bundle.resolve`, `osgi.service.lookup`, and `osgi.config.update`, each with child spans showing validation and execution phases.

**Metrics** — Go to *Explore → Prometheus* and query:
- `osgi_demo_operations_total` — Count of simulated OSGi operations
- `osgi_demo_operation_duration_milliseconds` — Duration histogram
- `osgi_client_requests_total` — Request counter from the metrics demo
- `osgi_client_jvm_memory_used_bytes` — JVM heap memory gauge

**Logs** — Go to *Explore → Loki* and browse log streams.
You will see structured log records for each OSGi operation, bundle discovery events, and warning/error messages — all with attributes you can filter on.

### Architecture

The demo uses an embedded [Apache Felix](https://felix.apache.org/) OSGi framework.
OpenTelemetry SDK and exporter JARs are on the system classpath, and their packages are exported to the OSGi framework via `org.osgi.framework.system.packages.extra`.
Felix SCR (Service Component Runtime) provides Declarative Services support.

Our bundles are auto-deployed into Felix:
- **`opentelemetry-osgi-runtime`** activates and creates the SDK with OTLP export configured via environment variables
- **`opentelemetry-osgi-client`** activates its demo components, including a periodic scheduler that generates traces, metrics, and logs every 5 seconds

### Stop the Demo

```bash
docker compose down -v
```

### Exposed Ports

| Port | Service | URL |
|---|---|---|
| 3000 | Grafana | [http://localhost:3000](http://localhost:3000) |
| 9090 | Prometheus | [http://localhost:9090](http://localhost:9090) |
| 4317 | OTel Collector (gRPC) | — |
| 4318 | OTel Collector (HTTP) | — |

## Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language runtime |
| OpenTelemetry Java | 1.49.0 | Observability framework |
| OSGi Framework | R8 (1.10.0) | Module system |
| OSGi Declarative Services | 1.5.1 | Component model |
| bnd-maven-plugin | 7.1.0 | OSGi metadata generation |
| Apache Felix | 7.0.5 | OSGi runtime (Docker demo) |
| Grafana | 11.5.2 | Observability UI |
| Grafana Tempo | 2.7.2 | Distributed tracing backend |
| Prometheus | 3.2.1 | Metrics backend |
| Grafana Loki | 3.4.2 | Log aggregation backend |
| OTel Collector | 0.120.0 | Telemetry gateway |

## License

This project is licensed under the [Eclipse Public License v2.0](LICENSE).
