# OpenTelemetry OSGi Demo

Demonstration bundle that consumes the `OpenTelemetry` service via Declarative Services and generates sample telemetry to showcase the integration.

## Demo Components

| Component | Description |
|---|---|
| `TracingDemoComponent` | Creates sample spans with attributes, events, and nested child spans |
| `MetricsDemoComponent` | Produces counters, histograms, and up-down counters with varying values |
| `LogBridgeDemoComponent` | Emits structured log records through the OpenTelemetry Logs API |
| `ContextPropagationDemoComponent` | Demonstrates W3C TraceContext propagation across async boundaries |
| `DemoSchedulerComponent` | Periodically triggers all demo components on a configurable interval |
| `HttpDemoServlet` | Servlet registered at `/demo/*` with status, slow, and error endpoints |
| `HttpTrafficGeneratorComponent` | Periodically hits servlet endpoints every 10 seconds |
| `JdbcDemoComponent` | Creates an H2 in-memory database and runs CRUD queries every 10 seconds |
| `JaxRsDemoResource` | JAX-RS resource at `/api/rest/*` with status, items, and detail endpoints |
| `JaxRsDispatcherServlet` | Lightweight annotation-based JAX-RS dispatcher |
| `JaxRsTrafficGeneratorComponent` | Generates traffic to JAX-RS endpoints every 12 seconds |

The demo module depends only on `opentelemetry-api` (not the SDK), following the recommended practice of separating instrumentation from SDK configuration.

## Usage

### In Karaf

```bash
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-demo-karaf-feature/0.1.0-SNAPSHOT/xml/features
feature:install opentelemetry-osgi-demo
```

### In Docker

See the [Docker Demo](../../README.md#docker-demo) section in the root README.

## Endpoints

When running the Docker demo:
- [http://localhost:8181/demo](http://localhost:8181/demo) — HTTP servlet endpoints
- [http://localhost:8181/api/rest/status](http://localhost:8181/api/rest/status) — JAX-RS resource endpoints

## Dashboard

![Demo Operations](../../doc/images/grafana-demo-operations.png)
