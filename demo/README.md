# OpenTelemetry OSGi Demo

This folder contains demonstration bundles that showcase the OpenTelemetry OSGi integration.

## Modules

### opentelemetry-osgi-demo

Demo components that consume the `OpenTelemetry` service via Declarative Services and periodically generate telemetry to verify the integration is working:

- **Tracing Demo** — Creates sample spans with attributes, events, and nested child spans
- **Metrics Demo** — Produces counters, histograms, and up-down counters with varying values
- **Log Bridge Demo** — Emits structured log records through the OpenTelemetry Logs API
- **Context Propagation Demo** — Demonstrates W3C TraceContext propagation across async boundaries
- **Demo Scheduler** — Periodically triggers all demo components on a configurable interval

The demo module depends only on the `opentelemetry-api` (not the SDK), following the recommended practice of separating instrumentation from SDK configuration.

## Usage

### In Karaf

```bash
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-demo-karaf-feature/0.1.0-SNAPSHOT/xml/features
feature:install opentelemetry-osgi-demo
```

### In Docker

See the [Docker Demo](../README.md#docker-demo) section in the root README for instructions on running the complete observability stack.

## Dashboard Preview

When running the Docker demo, the Grafana dashboard shows live telemetry from the demo components:

![Demo Operations Dashboard](../doc/images/grafana-demo-operations.png)
