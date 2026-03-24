# OpenTelemetry OSGi Agent Extension

An [OpenTelemetry Java Agent extension](https://opentelemetry.io/docs/zero-code/java/agent/api/) that instruments OSGi framework internals using ByteBuddy bytecode instrumentation.

**Status: Experimental** — This module is complementary to the DS-based [integrations](../../integrations/README.md) and [weaving](../../weaving/README.md) modules.

## How It Works

The module uses the OTel Java Agent Extension API with ByteBuddy to intercept OSGi framework calls.
It is vendor-agnostic and works with Felix, Equinox, or any OSGi R4+ framework.

## Instrumentations

| Target | Methods | Description |
|---|---|---|
| `Framework.init()` | `init` | Captures the system `BundleContext` and registers metrics |
| Bundle lifecycle | `start`, `stop`, `update`, `uninstall` | Traces bundle lifecycle operations |
| `BundleActivator` | `start`, `stop` | Traces activator callbacks |
| `BundleContext` | `registerService`, `installBundle` | Traces service registry and bundle install operations |

## Usage

```bash
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.javaagent.extensions=opentelemetry-osgi-agent-0.1.0-SNAPSHOT.jar \
     -jar your-osgi-application.jar
```

## Important Notes

- This module is **not** an OSGi bundle — it is a plain JAR loaded by the OTel Java Agent
- The Java Agent approach is heavier than the [weaving-based](../../weaving/README.md) approach and less OSGi-aware
- Consider using the weaving modules for production deployments where dynamic on/off switching and OSGi-native class loading semantics are preferred
