# OpenTelemetry OSGi Integrations

This folder contains integration bundles that bridge various OSGi subsystems into OpenTelemetry telemetry signals.
Each module consumes the `OpenTelemetry` service published by the [core runtime](../core/README.md) and produces domain-specific traces, metrics, and logs.

## Modules

### opentelemetry-osgi-framework

Bridges the OSGi core framework state into OpenTelemetry:

- **Framework Metrics** — Gauges for total bundle and service counts
- **Framework Events** — Traces for bundle install/start/stop/uninstall and service register/unregister events
- **Bundle Inventory** — Structured log records with a live snapshot of all bundles plus change tracking via `SynchronousBundleListener`
- **Service Inventory** — Structured log records with a live snapshot of all services plus change tracking via `ServiceListener`

### opentelemetry-osgi-scr

Exposes OSGi Declarative Services (SCR) component state as OpenTelemetry telemetry:

- **SCR Metrics** — Gauges for component states (active, satisfied, unsatisfied, failed)
- **SCR Inventory** — Structured log records enumerating all DS component descriptions and configurations
- **SCR Health Check** — Periodic health trace that flags non-active components with their unsatisfied references

Uses the [SCR Introspection API](https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.component.html#service.component-introspection) (`ServiceComponentRuntime`).

### opentelemetry-osgi-log

Forwards OSGi Log Service entries to OpenTelemetry:

- **Log Bridge** — Registers as a `LogListener` on `LogReaderService` and forwards each `LogEntry` as an OpenTelemetry log record with full context (bundle info, severity, exception details, thread info)
- **Log Metrics** — Counters for log entries by level and error counters by bundle name

Uses the [OSGi Log Service](https://docs.osgi.org/specification/osgi.core/8.0.0/service.log.html) (`LogReaderService`).
