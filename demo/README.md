# OpenTelemetry OSGi Demo

This folder contains demonstration bundles that showcase the OpenTelemetry OSGi integration.

## Modules

| Module | Description | Details |
|---|---|---|
| [opentelemetry-osgi-demo](opentelemetry-osgi-demo/README.md) | Tracing, metrics, log, HTTP, JDBC, and JAX-RS demos with traffic generators | [Read more →](opentelemetry-osgi-demo/README.md) |

The demo module depends only on `opentelemetry-api` (not the SDK), following the recommended practice of separating instrumentation from SDK configuration.

The [module README](opentelemetry-osgi-demo/README.md) includes:
- Docker Demo setup and architecture
- Full dashboard documentation with screenshots
- Drilldown apps, multi-instance comparison, and Explore tips
