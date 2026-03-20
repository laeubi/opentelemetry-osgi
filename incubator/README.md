# OpenTelemetry OSGi Incubator

This folder contains experimental and in-development modules that are not yet ready for production use.

## Modules

### opentelemetry-osgi-agent

An [OpenTelemetry Java Agent extension](https://opentelemetry.io/docs/zero-code/java/agent/api/) that instruments OSGi framework internals using ByteBuddy bytecode instrumentation.

**Status: Experimental** — This module uses the OTel Java Agent Extension API with ByteBuddy to intercept OSGi framework calls.
It is vendor-agnostic and works with Felix, Equinox, or any OSGi R4+ framework.

#### Instrumentations

- **Framework.init()** — Captures the system `BundleContext` when the OSGi framework initializes and registers metrics
- **Bundle lifecycle** — Traces `Bundle.start()`, `stop()`, `update()`, `uninstall()` calls
- **BundleActivator** — Traces `BundleActivator.start()` and `stop()` callbacks
- **BundleContext operations** — Traces `registerService()` and `installBundle()` calls

#### Usage

```bash
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.javaagent.extensions=opentelemetry-osgi-agent-0.1.0-SNAPSHOT.jar \
     -jar your-osgi-application.jar
```

> **Note:** This module is not an OSGi bundle.
> It is a plain JAR loaded by the OTel Java Agent via `-Dotel.javaagent.extensions=`.
> The agent approach is complementary to the DS-based [integrations](../integrations/README.md) modules.
