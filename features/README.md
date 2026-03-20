# OpenTelemetry OSGi Karaf Features

This folder contains Apache Karaf feature descriptors for deploying the OpenTelemetry OSGi integration.

## Modules

### opentelemetry-osgi-karaf-feature

The base feature providing the OpenTelemetry runtime for OSGi:

- **`opentelemetry-deps`** — Wraps 14 OpenTelemetry Java SDK JARs as OSGi bundles using Karaf's `wrap:` protocol with SPI Fly headers for cross-bundle `ServiceLoader` discovery
- **`opentelemetry-osgi`** — Deploys the runtime bundle and provides a default ConfigAdmin configuration

### opentelemetry-osgi-integration-karaf-feature

Deploys the integration bundles that bridge OSGi subsystems to OpenTelemetry:

- **`opentelemetry-osgi-integrations`** — Includes framework bridge, SCR introspection, and log bridge bundles; depends on `opentelemetry-osgi`

### opentelemetry-osgi-demo-karaf-feature

Deploys the demo client for demonstration purposes:

- **`opentelemetry-osgi-demo`** — Includes the demo bundle; depends on `opentelemetry-osgi-integrations` (which transitively pulls in the runtime)

## Feature Dependency Graph

```
opentelemetry-osgi-demo
  └── opentelemetry-osgi-integrations
        └── opentelemetry-osgi
              ├── opentelemetry-deps (14 wrapped OTel JARs + SPI Fly)
              └── scr (Karaf built-in)
```

## Deployment

```bash
# Add all feature repositories
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-karaf-feature/0.1.0-SNAPSHOT/xml/features
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-integration-karaf-feature/0.1.0-SNAPSHOT/xml/features
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-demo-karaf-feature/0.1.0-SNAPSHOT/xml/features

# Install just the runtime
feature:install opentelemetry-osgi

# Or install with integrations
feature:install opentelemetry-osgi-integrations

# Or install the full demo
feature:install opentelemetry-osgi-demo
```
