# OpenTelemetry OSGi Demo

This folder contains demonstration bundles that showcase the OpenTelemetry OSGi integration.

## Modules

| Module | Description | Details |
|---|---|---|
| [opentelemetry-osgi-demo](opentelemetry-osgi-demo/README.md) | Tracing, metrics, log, HTTP, JDBC, and JAX-RS demos with traffic generators | [Read more →](opentelemetry-osgi-demo/README.md) |

The demo module depends only on `opentelemetry-api` (not the SDK), following the recommended practice of separating instrumentation from SDK configuration.
See the [module README](opentelemetry-osgi-demo/README.md) for component details, endpoints, and dashboard screenshots.
