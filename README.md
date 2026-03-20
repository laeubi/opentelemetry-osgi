# OpenTelemetry OSGi Integration

Integration of [OpenTelemetry](https://opentelemetry.io/) with the [OSGi](https://www.osgi.org/) service platform.

## What is OpenTelemetry?

[OpenTelemetry](https://opentelemetry.io/) (OTel) is an open-source, vendor-neutral observability framework for generating, collecting, and exporting telemetry data.
It provides a unified set of APIs, SDKs, and tools to instrument applications and infrastructure, enabling deep visibility into distributed systems.

OpenTelemetry defines three core **signals**:

- **Traces** — End-to-end journey of a request through a distributed system, represented as spans with timing, attributes, and parent-child relationships
- **Metrics** — Quantitative measurements (counters, histograms, gauges) about application behavior over time
- **Logs** — Structured log records with automatic trace correlation via the [Log Bridge API](https://opentelemetry.io/docs/specs/otel/logs/bridge-api/)

## Project Structure

```
opentelemetry-osgi/
├── core/                    — Core runtime providing OpenTelemetry SDK as an OSGi service
├── integrations/            — Bridges for OSGi subsystems (framework, SCR, log)
├── demo/                    — Demonstration bundles showcasing the integration
├── features/                — Apache Karaf feature descriptors for deployment
├── incubator/               — Experimental modules (agent extension)
├── docker/                  — Docker demo with full Grafana observability stack
└── docker-compose.yml
```

| Folder | Description | Details |
|---|---|---|
| [`core/`](core/README.md) | OpenTelemetry SDK runtime as an OSGi service | [Read more →](core/README.md) |
| [`integrations/`](integrations/README.md) | Framework, SCR, and Log Service bridges to OpenTelemetry | [Read more →](integrations/README.md) |
| [`demo/`](demo/README.md) | Demo bundle generating sample traces, metrics, and logs | [Read more →](demo/README.md) |
| [`features/`](features/README.md) | Karaf features for runtime, integrations, and demo deployment | [Read more →](features/README.md) |
| [`incubator/`](incubator/README.md) | Java Agent extension for bytecode-level OSGi instrumentation | [Read more →](incubator/README.md) |

## Prerequisites

- Java 21 or later
- Maven 3.9+

## Building

```bash
mvn clean install
```

## Quick Start — Karaf Deployment

After building, deploy into an [Apache Karaf](https://karaf.apache.org/) container:

```bash
# Add all feature repositories
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-karaf-feature/0.1.0-SNAPSHOT/xml/features
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-integration-karaf-feature/0.1.0-SNAPSHOT/xml/features
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-demo-karaf-feature/0.1.0-SNAPSHOT/xml/features

# Install the full demo (includes runtime + integrations + demo client)
feature:install opentelemetry-osgi-demo
```

See [features/README.md](features/README.md) for more deployment options.

## Docker Demo

The easiest way to see the integration in action is the Docker Compose demo.
It spins up the full Grafana observability stack with a single command:

```
OSGi App (Karaf + our features)
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

```bash
# Build and start everything
docker compose up --build -d

# Watch the OSGi application logs
docker compose logs -f osgi-app

# Open Grafana at http://localhost:3000

# Stop and clean up
docker compose down -v
```

### Explore in Grafana

Open [http://localhost:3000](http://localhost:3000) (no login required).

- **Traces** — *Explore → Tempo*: spans for `osgi.bundle.started`, `osgi.service.registered`, `osgi.scr.healthcheck`
- **Metrics** — *Explore → Prometheus*: `osgi_framework_bundles`, `osgi_framework_services`, `osgi_scr_components`, `osgi_log_entries_total`
- **Logs** — *Explore → Loki*: bundle/service inventory, SCR component state, forwarded OSGi log entries

## Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language runtime |
| OpenTelemetry Java | 1.49.0 | Observability framework |
| OSGi Framework | R8 (1.10.0) | Module system |
| OSGi Declarative Services | 1.5.1 | Component model |
| bnd-maven-plugin | 7.1.0 | OSGi metadata generation |
| Apache Karaf | 4.4.7 | OSGi container |
| Apache Aries SPI Fly | 1.3.7 | Cross-bundle ServiceLoader support |
| Grafana | 11.5.2 | Observability UI |
| Grafana Tempo | 2.7.2 | Distributed tracing backend |
| Prometheus | 3.2.1 | Metrics backend |
| Grafana Loki | 3.4.2 | Log aggregation backend |
| OTel Collector | 0.120.0 | Telemetry gateway |

## License

This project is licensed under the [Eclipse Public License v2.0](LICENSE).
