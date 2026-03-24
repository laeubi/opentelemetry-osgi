# AGENTS.md - Instructions for AI Agents

This file contains instructions and context for AI agents working on this codebase.

## Project Overview

This is a Maven multi-module project integrating OpenTelemetry with OSGi.
The project is organized into six top-level folders, each containing related modules.

## Repository Structure

```
opentelemetry-osgi/
├── pom.xml                          # Parent POM with dependency management
├── docker-compose.yml               # Full Grafana observability stack
├── docker/
│   ├── Dockerfile                   # Multi-stage build (Maven → Karaf runtime)
│   ├── otel-collector-config.yaml   # OTel Collector pipeline config
│   ├── tempo.yaml                   # Grafana Tempo config
│   ├── prometheus.yml               # Prometheus config
│   ├── loki.yaml                    # Grafana Loki config
│   └── grafana/provisioning/        # Grafana auto-provisioned datasources
├── core/                            # Core runtime
│   ├── pom.xml                      # Aggregator POM
│   └── opentelemetry-osgi-runtime/  # OSGi service providing OpenTelemetry SDK
│       └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/runtime/
│           ├── OpenTelemetryService.java               # DS component publishing OpenTelemetry
│           ├── OpenTelemetryProviderRegistration.java   # Registers TracerProvider, MeterProvider, etc.
│           └── OpenTelemetryConfiguration.java          # ConfigAdmin configuration annotation
├── integrations/                    # OSGi subsystem bridges
│   ├── pom.xml                      # Aggregator POM
│   ├── opentelemetry-osgi-framework/  # Framework bridge (bundles, services, events)
│   │   └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/framework/
│   │       ├── FrameworkMetricsComponent.java    # Bundle/service count gauges
│   │       ├── FrameworkEventComponent.java      # Bundle/service event tracing
│   │       ├── BundleInventoryComponent.java     # Live bundle inventory
│   │       ├── ServiceInventoryComponent.java    # Live service inventory
│   │       ├── BundleStateUtil.java              # Bundle state name utility
│   │       └── BundleInfo.java                   # Bundle state record
│   ├── opentelemetry-osgi-scr/      # SCR introspection → OpenTelemetry bridge
│   │   └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/scr/
│   │       ├── ScrMetricsComponent.java         # DS component state gauges
│   │       ├── ScrInventoryComponent.java       # DS inventory as structured logs
│   │       └── ScrHealthCheckComponent.java     # Periodic health trace
│   ├── opentelemetry-osgi-felix-healthcheck/  # Felix Health Check → OpenTelemetry bridge
│   │   └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/healthcheck/
│   │       ├── HealthCheckMetricsComponent.java  # HC count/status/duration gauges
│   │       ├── HealthCheckTracingComponent.java  # Periodic HC execution traces
│   │       └── HealthCheckInventoryComponent.java # HC inventory as structured logs
│   ├── opentelemetry-osgi-cm/          # OSGi Config Admin → OpenTelemetry bridge
│   │   └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/cm/
│   │       ├── ConfigAdminMetricsComponent.java  # Configuration count gauges + event counter
│   │       ├── ConfigAdminEventComponent.java    # Configuration change traces
│   │       └── ConfigAdminInventoryComponent.java # Configuration inventory as structured logs
│   ├── opentelemetry-osgi-mxbeans/     # Java MXBeans → OpenTelemetry bridge
│   │   └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/mxbeans/
│   │       ├── MxBeansMetricsComponent.java      # JVM metrics (memory, CPU, threads, GC, etc.)
│   │       └── MxBeansConfiguration.java         # DS config annotation for metric groups
│   ├── opentelemetry-osgi-typedevent/  # OSGi Typed Event → OpenTelemetry bridge
│   │   └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/typedevent/
│   │       ├── TypedEventMetricsComponent.java   # Event counter + handler count gauges
│   │       ├── TypedEventTracingComponent.java   # Trace spans for each event
│   │       └── TypedEventInventoryComponent.java # Handler inventory as structured logs
│   └── opentelemetry-osgi-log/      # OSGi Log Service → OpenTelemetry bridge
│       └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/log/
│           ├── LogBridgeComponent.java          # Forwards LogEntry to OTel logs
│           └── LogMetricsComponent.java         # Log entry counters as OTel metrics
├── demo/                            # Demonstration bundles
│   ├── pom.xml                      # Aggregator POM
│   └── opentelemetry-osgi-demo/     # Demo bundle consuming OpenTelemetry service
│       └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/demo/
│           ├── TracingDemoComponent.java              # Tracing demos
│           ├── MetricsDemoComponent.java              # Metrics demos
│           ├── LogBridgeDemoComponent.java            # Log bridge demos
│           ├── ContextPropagationDemoComponent.java   # Context propagation demos
│           ├── DemoSchedulerComponent.java            # Periodic telemetry generator
│           ├── HttpDemoServlet.java                   # Demo servlet at /demo/*
│           └── HttpTrafficGeneratorComponent.java     # Periodic HTTP traffic generator
├── features/                        # Karaf feature descriptors
│   ├── pom.xml                      # Aggregator POM
│   ├── opentelemetry-osgi-karaf-feature/              # Runtime + OTel deps feature
│   │   ├── src/main/feature/feature.xml               # opentelemetry-deps + opentelemetry-osgi
│   │   └── src/main/resources/
│   │       └── org.eclipse.osgi.technology.incubator.opentelemetry.runtime.cfg
│   ├── opentelemetry-osgi-integration-karaf-feature/  # Integration bundles feature
│   │   └── src/main/feature/feature.xml               # opentelemetry-osgi-integrations
│   ├── opentelemetry-osgi-demo-karaf-feature/         # Demo feature
│   │   └── src/main/feature/feature.xml               # opentelemetry-osgi-demo
│   └── opentelemetry-osgi-karaf-distribution/         # Pre-built Karaf distribution
│       ├── pom.xml                                    # karaf-assembly packaging
│       └── src/main/resources/assembly/etc/           # Config overlay
├── incubator/                       # Experimental modules
│   ├── pom.xml                      # Aggregator POM
│   └── opentelemetry-osgi-agent/    # Java Agent extension (ByteBuddy)
│       └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/agent/
├── weaving/                         # OSGi WeavingHook based bytecode instrumentation
│   ├── pom.xml                      # Aggregator POM
│   ├── opentelemetry-osgi-weaving/  # Host bundle: WeavingHook, WeaverRegistry, ASM embedded
│   │   └── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/weaving/
│   │       ├── Weaver.java                      # SPI interface for weaver implementations
│   │       ├── WeaverRegistry.java              # ServiceLoader-based weaver discovery
│   │       ├── OpenTelemetryWeavingHook.java    # OSGi WeavingHook delegating to weavers
│   │       ├── WeavingHookActivator.java        # BundleActivator (not DS)
│   │       ├── OpenTelemetryProxy.java          # Graceful proxy with noop fallback
│   │       └── SafeClassWriter.java             # ClassWriter using target bundle classloader
│   ├── opentelemetry-osgi-weaver-servlet/  # Fragment: HttpServlet instrumentation
│   │   ├── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/weaver/servlet/
│   │   │   ├── ServletWeaver.java                   # Weaver targeting HttpServlet subclasses
│   │   │   ├── ServletClassVisitor.java             # ASM ClassVisitor for servlet methods
│   │   │   ├── ServletServiceMethodVisitor.java     # ASM AdviceAdapter for bytecode injection
│   │   │   └── ServletInstrumentationHelper.java    # Static helpers called from woven code
│   │   └── src/main/resources/META-INF/services/
│   │       └── ...opentelemetry.weaving.Weaver      # Java SPI registration
│   ├── opentelemetry-osgi-weaver-jdbc/     # Fragment: JDBC instrumentation
│   │   ├── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/weaver/jdbc/
│   │   │   ├── JdbcWeaver.java                      # Weaver targeting Statement implementations
│   │   │   ├── JdbcClassVisitor.java                # ASM ClassVisitor for execute* methods
│   │   │   ├── JdbcMethodVisitor.java               # ASM AdviceAdapter for bytecode injection
│   │   │   └── JdbcInstrumentationHelper.java       # Static helpers called from woven code
│   │   └── src/main/resources/META-INF/services/
│   │       └── ...opentelemetry.weaving.Weaver      # Java SPI registration
│   └── opentelemetry-osgi-weaver-jaxrs/    # Fragment: JAX-RS resource instrumentation
│       ├── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/weaver/jaxrs/
│       │   ├── JaxRsWeaver.java                     # Weaver targeting @Path-annotated classes
│       │   ├── JaxRsClassVisitor.java               # ASM ClassVisitor reading @Path annotations
│       │   ├── JaxRsMethodVisitor.java              # ASM AdviceAdapter detecting HTTP method annotations
│       │   └── JaxRsInstrumentationHelper.java      # Static helpers called from woven code
│       └── src/main/resources/META-INF/services/
│           └── ...opentelemetry.weaving.Weaver      # Java SPI registration
│   └── opentelemetry-osgi-weaver-scr/      # Fragment: SCR lifecycle instrumentation
│       ├── src/main/java/org/eclipse/osgi/technology/incubator/opentelemetry/weaver/scr/
│       │   ├── ScrWeaver.java                       # Weaver targeting DS component classes via XML parsing
│       │   ├── ComponentDescriptor.java             # Record: lifecycle method names from DS XML
│       │   ├── ScrClassVisitor.java                 # ASM ClassVisitor matching methods by XML descriptor
│       │   ├── ScrMethodVisitor.java                # ASM AdviceAdapter injecting instrumentation bytecode
│       │   └── ScrInstrumentationHelper.java        # Static helpers called from woven code
│       └── src/main/resources/META-INF/services/
│           └── ...opentelemetry.weaving.Weaver      # Java SPI registration
├── doc/
│   └── images/                      # Screenshots for README (generated via Grafana Image Renderer)
├── README.md
├── AGENTS.md                        # This file
└── LICENSE                          # EPL-2.0
```

## Maven Coordinates

- **GroupId**: `org.eclipse.osgi-technology.incubator`
- **Package namespace**: `org.eclipse.osgi.technology.incubator.opentelemetry`
- **Parent artifactId**: `opentelemetry-osgi-parent`

### Module Artifacts

| Module | ArtifactId | Folder |
|---|---|---|
| Runtime | `opentelemetry-osgi-runtime` | `core/` |
| Framework Bridge | `opentelemetry-osgi-framework` | `integrations/` |
| SCR Bridge | `opentelemetry-osgi-scr` | `integrations/` |
| Log Bridge | `opentelemetry-osgi-log` | `integrations/` |
| Health Check Bridge | `opentelemetry-osgi-felix-healthcheck` | `integrations/` |
| Config Admin Bridge | `opentelemetry-osgi-cm` | `integrations/` |
| MXBeans Bridge | `opentelemetry-osgi-mxbeans` | `integrations/` |
| Typed Event Bridge | `opentelemetry-osgi-typedevent` | `integrations/` |
| Demo | `opentelemetry-osgi-demo` | `demo/` |
| Runtime Feature | `opentelemetry-osgi-karaf-feature` | `features/` |
| Integration Feature | `opentelemetry-osgi-integration-karaf-feature` | `features/` |
| Demo Feature | `opentelemetry-osgi-demo-karaf-feature` | `features/` |
| Karaf Distribution | `opentelemetry-osgi-karaf-distribution` | `features/` |
| Agent Extension | `opentelemetry-osgi-agent` | `incubator/` |
| Weaving Host | `opentelemetry-osgi-weaving` | `weaving/` |
| Servlet Weaver | `opentelemetry-osgi-weaver-servlet` | `weaving/` |
| JDBC Weaver | `opentelemetry-osgi-weaver-jdbc` | `weaving/` |
| JAX-RS Weaver | `opentelemetry-osgi-weaver-jaxrs` | `weaving/` |
| SCR Lifecycle Weaver | `opentelemetry-osgi-weaver-scr` | `weaving/` |

### Aggregator POMs

Each subfolder has an aggregator POM that references the root parent:

| Folder | ArtifactId |
|---|---|
| `core/` | `opentelemetry-osgi-core-parent` |
| `integrations/` | `opentelemetry-osgi-integrations-parent` |
| `demo/` | `opentelemetry-osgi-demo-parent` |
| `features/` | `opentelemetry-osgi-features-parent` |
| `incubator/` | `opentelemetry-osgi-incubator-parent` |
| `weaving/` | `opentelemetry-osgi-weaving-parent` |

All module POMs use `<relativePath>../../pom.xml</relativePath>` to reference the root parent directly.

## Build Commands

```bash
# Full build
mvn clean verify

# Build a single module (use relative path)
mvn clean verify -pl core/opentelemetry-osgi-runtime

# Build with dependency resolution logging
mvn clean verify -X
```

## Docker Demo Commands

```bash
# Build and start the full observability stack
docker compose up --build -d

# View OSGi application logs
docker compose logs -f osgi-app

# View OTel Collector logs
docker compose logs -f otel-collector

# Stop and clean up (including volumes)
docker compose down -v

# Rebuild only the OSGi app after code changes
docker compose build osgi-app && docker compose up -d osgi-app
```

## Docker Architecture

The Docker demo uses a pre-built Apache Karaf 4.4.7 distribution from the `opentelemetry-osgi-karaf-distribution` module.
The `karaf-assembly` packaging resolves all bundles, features, and dependencies at Maven build time into a self-contained distribution.
The Dockerfile builds the Maven project, then copies the assembled distribution into the runtime image — no manual JAR copying or `sed` configuration hacking needed.
Apache Aries SPI Fly (dynamic weaving) enables cross-bundle `ServiceLoader` discovery required by the OTel SDK.

**Key environment variables** (set in `docker-compose.yml`):
- `OTEL_EXPORTER_OTLP_ENDPOINT` — Triggers OTLP export mode **and** sets the collector endpoint (default: `http://otel-collector:4318`)
- `OTEL_SERVICE_NAME` — Overrides the `service.name` resource attribute

**Data flow**: OSGi App → OTel Collector (OTLP/HTTP) → Tempo + Prometheus + Loki → Grafana

When modifying the OTel Collector pipeline, edit `docker/otel-collector-config.yaml`.
Grafana datasources are auto-provisioned from `docker/grafana/provisioning/datasources/datasources.yaml`.
Grafana dashboards are auto-provisioned from `docker/grafana/provisioning/dashboards/`.

### Grafana Image Renderer

The Docker Compose stack includes a Grafana Image Renderer service (`grafana/grafana-image-renderer:3.12.1`).
This enables server-side PNG rendering of dashboards and panels via the Grafana `/render` API.

**Taking screenshots** (requires the Docker Compose stack to be running):

```bash
# Full dashboard screenshot
curl -o screenshot.png \
  "http://localhost:3000/render/d/osgi-overview/osgi-observability-overview?orgId=1&from=now-30m&to=now&width=1920&height=2800&kiosk"

# Cropped top section only (Framework row)
curl -o framework.png \
  "http://localhost:3000/render/d/osgi-overview/osgi-observability-overview?orgId=1&from=now-30m&to=now&width=1920&height=420&kiosk"

# Single panel by panelId (find panelId in the dashboard JSON)
curl -o panel.png \
  "http://localhost:3000/render/d-solo/osgi-overview/osgi-observability-overview?orgId=1&panelId=2&from=now-30m&to=now&width=800&height=400"
```

**Render URL parameters:**
- `width` / `height` — output image dimensions in pixels
- `kiosk` — hides the Grafana navigation chrome
- `from` / `to` — time range (e.g., `now-30m`, `now-1h`)
- `theme=light` — use the light theme (default is dark)

Screenshots for the README are stored in `doc/images/` and should be regenerated when the dashboard changes.

### Grafana Provisioning Structure

```
docker/grafana/provisioning/
├── datasources/
│   └── datasources.yaml          # Tempo, Prometheus, Loki (with stable UIDs)
├── dashboards/
│   ├── dashboards.yaml           # Dashboard provider config
│   └── osgi-overview.json        # OSGi Observability Overview dashboard
└── plugins/
    └── plugins.yaml              # Auto-enables Drilldown apps (Traces, Metrics, Logs)
```

Datasources use explicit `uid` values (`tempo`, `prometheus`, `loki`) so that dashboard JSON and cross-datasource links remain stable across fresh deployments.

### Grafana Drilldown Apps

The Docker Compose stack installs and auto-enables three Grafana Drilldown apps via `GF_INSTALL_PLUGINS` and plugin provisioning:

| Plugin ID | Name | Purpose |
|---|---|---|
| `grafana-exploretraces-app` | Traces Drilldown | Explore Tempo traces with filtering, breakdown, service structure |
| `grafana-lokiexplore-app` | Logs Drilldown | Explore Loki logs with pattern detection |
| `grafana-metricsdrilldown-app` | Metrics Drilldown | Explore Prometheus metrics with RED aggregation |

Plugins are installed at Grafana startup and enabled via `docker/grafana/provisioning/plugins/plugins.yaml`.

### Tempo Metrics Generator

Tempo's `metrics_generator` is configured to produce span metrics and service graphs from ingested traces:

- **Span metrics** — RED metrics (rate, errors, duration) per span name, written to Prometheus as `traces_spanmetrics_calls_total` and `traces_spanmetrics_duration_seconds_*`
- **Service graphs** — Tracks request flow between services, written as `traces_service_graph_request_total` and `traces_service_graph_request_server_seconds_*`
- **Dimensions** — `http.method`, `http.status_code`, `http.route`, `db.system`, `db.operation`, `jaxrs.resource.class`
- **Remote write** — Metrics are pushed to Prometheus at `http://prometheus:9080/api/v1/write`
- **Processors enabled** via overrides: `service-graphs` and `span-metrics`

These metrics power the Traces Drilldown app's span rate, error rate, and duration visualizations.

## Code Conventions

### Java

- **Java version**: 21 (set via `maven.compiler.release` in parent POM)
- **Code style**: Use public or package-private top-level types instead of inner classes/interfaces/records
- **Records**: Use Java records for immutable data types (e.g. `BundleInfo`)
- **Switch expressions**: Use enhanced switch expressions (Java 21 feature)
- **No inner types**: Prefer separate files for each class, interface, record, and annotation type

### OSGi

- **Metadata generation**: bnd-maven-plugin generates MANIFEST.MF and DS component XML
- **Declarative Services**: Use `@Component`, `@Reference`, `@Activate`, `@Deactivate`, `@Modified` annotations from `org.osgi.service.component.annotations`
- **Configuration**: Use annotation interfaces (not `@ObjectClassDefinition` from metatype — keep it simple with DS config annotation types)
- **Scope**: OSGi annotations are `provided` scope (processed at build time by bnd)
- **Package-info**: Each package has a `package-info.java` with Javadoc (Javadoc comment comes before the package declaration)

### OpenTelemetry

- **API vs SDK**: Integration and demo bundles depend only on `opentelemetry-api`; only the runtime bundle depends on `opentelemetry-sdk`
- **OTLP export**: The runtime supports both `logging` and `otlp` exporter types. OTLP is auto-selected when `OTEL_EXPORTER_OTLP_ENDPOINT` env var is set.
- **Sender**: Uses `opentelemetry-exporter-sender-jdk` (Java's built-in HttpClient) with OTLP/HTTP protocol — no gRPC or external HTTP library needed
- **Instrumentation scopes**: Use fully qualified package names as instrumentation scope names
- **BOM**: Dependency versions managed via `opentelemetry-bom` import in parent POM

### Markdown

- Start each sentence on a new line (one sentence per line for easier diffing)

## Dependency Versions

All versions are centralized in the parent POM properties:

| Property | Value | Artifact |
|---|---|---|
| `opentelemetry.version` | `1.49.0` | OpenTelemetry BOM |
| `osgi.framework.version` | `1.10.0` | OSGi Framework API |
| `osgi.service.component.annotations.version` | `1.5.1` | DS annotations |
| `osgi.service.component.version` | `1.5.1` | DS runtime |
| `osgi.service.log.version` | `1.5.0` | OSGi Log Service |
| `osgi.annotation.bundle.version` | `2.0.0` | Bundle annotations |
| `felix.healthcheck.api.version` | `2.0.4` | Felix Health Check API |
| `felix.healthcheck.core.version` | `2.0.8` | Felix Health Check Core |
| `felix.healthcheck.generalchecks.version` | `3.0.8` | Felix Health Check General Checks |
| `osgi.service.cm.version` | `1.6.1` | OSGi Configuration Admin API |
| `osgi.service.typedevent.version` | `1.0.0` | OSGi Typed Event API |
| `aries.typedevent.version` | `1.0.1` | Apache Aries TypedEvent Bus |
| `aries.component.dsl.version` | `1.2.2` | Aries Component DSL |
| `osgi.util.converter.version` | `1.0.9` | OSGi Converter |
| `osgi.util.pushstream.version` | `1.1.0` | OSGi PushStream |
| `osgi.util.promise.version` | `1.3.0` | OSGi Promise |
| `osgi.util.function.version` | `1.2.0` | OSGi Function |
| `bnd.version` | `7.1.0` | bnd-maven-plugin |

When updating OpenTelemetry version, update the `opentelemetry.version` property — all module dependencies are managed via the BOM.

## Feature Architecture

The project uses three separate Karaf feature modules, each producing its own feature descriptor:

### opentelemetry-osgi-karaf-feature (Runtime)

- Defines `opentelemetry-deps` (14 wrapped OTel SDK JARs + SPI Fly) and `opentelemetry-osgi` (runtime bundle + config)
- Inline `<config>` element provides default configuration for PID `org.eclipse.osgi.technology.incubator.opentelemetry.runtime`
- Example `.cfg` file in `src/main/resources/` for manual deployment to `${karaf.etc}/`

### opentelemetry-osgi-integration-karaf-feature (Integrations)

- Defines `opentelemetry-osgi-integrations` (framework + scr + log + healthcheck + cm + weaving bundles)
- Weaving bundles are installed at start-level 20 to activate before application bundles
- Depends on `opentelemetry-osgi` feature

### opentelemetry-osgi-demo-karaf-feature (Demo)

- Defines `opentelemetry-osgi-demo` (demo bundle)
- Depends on `opentelemetry-osgi-integrations` feature

### Feature Dependency Graph

```
opentelemetry-osgi-demo
  └── opentelemetry-osgi-integrations
        └── opentelemetry-osgi
              ├── opentelemetry-deps (14 wrapped OTel JARs + SPI Fly)
              └── scr (Karaf built-in)
```

### Karaf Deployment

```bash
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-karaf-feature/0.1.0-SNAPSHOT/xml/features
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-integration-karaf-feature/0.1.0-SNAPSHOT/xml/features
feature:repo-add mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-demo-karaf-feature/0.1.0-SNAPSHOT/xml/features
feature:install opentelemetry-osgi-demo
```

### Adding New OTel Dependencies

When adding new OTel JARs, add a `<bundle>wrap:mvn:...</bundle>` entry to the `opentelemetry-deps` feature in `features/opentelemetry-osgi-karaf-feature/src/main/feature/feature.xml`.

### Karaf Distribution Module

The `opentelemetry-osgi-karaf-distribution` module (`features/opentelemetry-osgi-karaf-distribution/`) uses `karaf-assembly` packaging:

- Dependencies: `framework` KAR (base), `standard` + `specs` feature repos, our 3 feature descriptor XMLs
- `bootFeatures` lists essential Karaf features (wrap, bundle, config, shell, ssh, etc.) + `opentelemetry-osgi-demo`
- The assembly resolves ALL transitive dependencies into `target/assembly/system/` at build time
- Output: `target/assembly/` (ready-to-run directory) + `.tar.gz`/`.zip` archives
- bnd-maven-plugin and maven-jar-plugin are skipped (not an OSGi bundle)
- Build takes ~40 seconds after initial dependency resolution
- Config overlay: `src/main/resources/assembly/etc/` files are copied into the distribution's `etc/`

## Agent Module Notes

The agent module (`incubator/opentelemetry-osgi-agent`) uses the OTel Java Agent Extension API with ByteBuddy bytecode instrumentation.
It is **not** an OSGi bundle — it is a plain JAR loaded via `-Dotel.javaagent.extensions=`.

### Architecture

- **`OsgiInstrumentationModule`** extends `InstrumentationModule` and registers via SPI (`META-INF/services/io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule`)
- `classLoaderMatcher()` checks for `org.osgi.framework.launch.Framework` — zero overhead for non-OSGi apps
- Four `TypeInstrumentation` implementations instrument standard OSGi interfaces (vendor-agnostic)

### Key Design Decisions

- **Instruments interfaces, not vendor classes**: Uses `implementsInterface(named("org.osgi.framework.Bundle"))` etc. to work with Felix, Equinox, or any OSGi framework
- **`Framework.init()` as entry point**: Per the OSGi spec, `getBundleContext()` returns a valid system BundleContext after init — this is where metrics are registered
- **`GlobalOpenTelemetry.get()`**: The javaagent places the OTel API on the bootstrap classloader; safe to call from any classloader
- **Helper class injection**: `OsgiSingletons` is automatically injected by the agent into the target classloader (referenced from advice code)
- **`@Advice.Origin("#m")`**: ByteBuddy provides the method name at instrumentation time, reducing duplicate advice classes
- **`Span.current()` in exit advice**: The scope from enter is still active, so `Span.current()` returns our span
- **All dependencies are `provided` scope**: The javaagent supplies OTel API, SDK, and ByteBuddy at runtime

### Build Differences from OSGi Modules

- bnd-maven-plugin is **skipped** (not an OSGi bundle)
- maven-jar-plugin uses default manifest (not bnd-generated)
- No shade plugin (no runtime dependencies to bundle)

## Framework Module Notes

The framework module (`integrations/opentelemetry-osgi-framework`) provides OSGi framework telemetry as proper DS components:

- Uses `@Reference OpenTelemetry` and `BundleContext` (injected via `@Activate`) — no `FrameworkUtil.getBundle()` workaround
- Registers as `BundleListener` and `ServiceListener` in `@Activate`, unregisters in `@Deactivate`
- `BundleInventoryComponent` uses `SynchronousBundleListener` to capture events before the framework proceeds; emits snapshot at activation + change log records for every bundle event
- `ServiceInventoryComponent` uses `ServiceListener` to track registrations/unregistrations/modifications; emits snapshot at activation + change log records with using-bundles info
- Async gauges use `ObservableLongGauge` with proper cleanup via `close()` on deactivate
- `BundleStateUtil` provides the `bundleStateToString()` utility shared across components
- `BundleInfo` record captures immutable bundle state snapshots

## SCR Module Notes

The SCR module (`integrations/opentelemetry-osgi-scr`) uses the OSGi SCR Introspection API from `org.osgi.service.component.runtime`:

- References `ServiceComponentRuntime` to enumerate all DS component descriptions and configurations
- Uses `ComponentConfigurationDTO` state constants: `UNSATISFIED_CONFIGURATION=1`, `UNSATISFIED_REFERENCE=2`, `SATISFIED=4`, `ACTIVE=8`, `FAILED_ACTIVATION=16`
- The `configStateToString()` utility in `ScrMetricsComponent` maps state integers to human-readable names — reused by other SCR components
- Async gauges (via `ObservableLongGauge`) query component state on every metric collection cycle

## Log Module Notes

The Log module (`integrations/opentelemetry-osgi-log`) uses the OSGi Log Service from `org.osgi.service.log`:

- References `LogReaderService` and registers as a `LogListener` to capture real-time log entries
- Maps `LogLevel` (AUDIT, ERROR, WARN, INFO, DEBUG, TRACE) to OpenTelemetry `Severity`
- Enriches OTel log records with: bundle symbolic name/id/version, logger name, sequence number, thread info, service reference, source code location, exception details
- `LogMetricsComponent` maintains counters by log level and a dedicated error counter by bundle name
- Requires OSGi Log Service in the container (Karaf provides via Pax Logging; in standalone Felix add `org.apache.felix:org.apache.felix.log:1.3.0`)

## Health Check Module Notes

The Health Check module (`integrations/opentelemetry-osgi-felix-healthcheck`) uses the [Apache Felix Health Check](https://felix.apache.org/documentation/subprojects/apache-felix-healthcheck.html) API:

- References `HealthCheckExecutor` to execute all registered health checks periodically (every 30 seconds)
- Uses `HealthCheckSelector.empty()` which, combined with the executor config (`defaultTags=` empty), selects ALL registered health checks regardless of tags
- `HealthCheckMetricsComponent` uses a `ScheduledExecutorService` for periodic execution and caches results in `volatile lastResults` for async gauge callbacks
- Status mapping: `Result.Status` values (OK, WARN, CRITICAL, TEMPORARILY_UNAVAILABLE, HEALTH_CHECK_ERROR) are used as metric attributes
- `HealthCheckTracingComponent` creates a parent span `osgi.hc.execution` with child spans for each individual health check result
- `HealthCheckInventoryComponent` queries `BundleContext` for all `HealthCheck` service references and emits log records with name, tags, and bundle info
- The executor config file (`org.apache.felix.hc.core.impl.executor.HealthCheckExecutorImpl.cfg`) must have `defaultTags=` (empty) to avoid the executor's default tag filter `["default"]` which would miss checks tagged with other values like `systemalive`
- Felix Health Check bundles are proper OSGi bundles (they have `Bundle-SymbolicName`) — no `wrap:` protocol needed in feature descriptors
- General checks (CPU, Memory, ThreadUsage, DiskSpace, BundlesStarted) require `.cfg` files to activate (`configurationPolicy=REQUIRE`); FrameworkStartCheck has `configurationPolicy=OPTIONAL` and auto-activates

## Config Admin Module Notes

The Config Admin module (`integrations/opentelemetry-osgi-cm`) uses the OSGi Configuration Admin service:

- `ConfigAdminMetricsComponent` and `ConfigAdminEventComponent` both implement `ConfigurationListener` to receive configuration change events
- `ConfigurationEvent` does NOT include property values (security by design) — only PID, factory PID, event type, and service reference
- Event types: `CM_UPDATED=1`, `CM_DELETED=2`, `CM_LOCATION_CHANGED=3`
- `ConfigAdminMetricsComponent` uses async gauges for config counts and a `LongCounter` for event counting by type
- `ConfigAdminEventComponent` creates traces for each configuration change with PID and event type as span attributes
- `ConfigAdminInventoryComponent` queries `configAdmin.listConfigurations(null)` for all configs — returns `null` (not empty array) when none exist
- Inventory log records include: PID, factory PID, bundle location, property count, and property keys (but not values, for security)

## MXBeans Module Notes

The MXBeans module (`integrations/opentelemetry-osgi-mxbeans`) uses `java.lang.management.ManagementFactory` to expose JVM runtime metrics:

- Single component `MxBeansMetricsComponent` with `@Modified` support for dynamic reconfiguration
- Configurable via `MxBeansConfiguration` annotation — 7 metric groups can be individually enabled/disabled: `memoryEnabled`, `cpuEnabled`, `threadsEnabled`, `gcEnabled`, `classLoadingEnabled`, `bufferPoolsEnabled`, `memoryPoolsEnabled`
- Uses `com.sun.management.OperatingSystemMXBean` for process/system CPU load and physical memory (optional import; degrades gracefully on non-HotSpot JVMs)
- All metrics are registered as OpenTelemetry async gauges (callbacks) — no background threads or polling
- GC, memory pool, and buffer pool metrics are per-instance with name attributes
- `Import-Package: com.sun.management;resolution:=optional` in bnd config to avoid mandatory resolution of JVM-internal package
- The module has a separate Grafana dashboard (`jvm-mxbeans.json`) with 7 sections: Memory, CPU, Threads, GC, Class Loading, Memory Pools, Buffer Pools

## Typed Event Module Notes

The Typed Event module (`integrations/opentelemetry-osgi-typedevent`) uses the [OSGi Typed Event Service](https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.typedevent.html) to bridge event bus activity into OpenTelemetry:

- Both `TypedEventMetricsComponent` and `TypedEventTracingComponent` implement `UntypedEventHandler` with `event.topics=*` to observe ALL events flowing through the bus
- `TypedEventMetricsComponent` maintains a `LongCounter` (`osgi.typedevent.events.total`) with `typedevent.topic` and `typedevent.topic_prefix` attributes, plus async gauges for handler counts
- `TypedEventTracingComponent` creates `osgi.typedevent.deliver` spans with topic and event data as span attributes (capped at 20 data attributes per event)
- `TypedEventInventoryComponent` queries `BundleContext.getAllServiceReferences()` for `TypedEventHandler`, `UntypedEventHandler`, and `UnhandledEventHandler` services at activation time
- `extractTopicPrefix()` utility extracts the first two segments of a topic path (e.g., `org/eclipse/osgi/demo/heartbeat` → `org/eclipse`) for broader metric grouping
- The handler count gauge for untyped handlers includes the two monitoring handlers themselves (metrics + tracing)
- Requires Apache Aries TypedEvent Bus implementation + 5 transitive dependencies (Component DSL, Converter, PushStream, Promise, Function)

### Design Trade-off: UntypedEventHandler vs TypedEventMonitor

The integration uses `UntypedEventHandler` rather than `TypedEventMonitor` because:
- Simpler API — follows the same listener pattern as other integrations (ConfigurationListener, LogListener, BundleListener)
- TypedEventMonitor would require PushStream reactive API with backpressure handling
- **Trade-off**: All events are considered "handled" by the monitoring handlers, so `UnhandledEventHandler` services will never fire
- This is documented in `package-info.java` and should be noted by users who depend on unhandled event detection

## Weaving Module Notes

The weaving module (`weaving/`) uses the [OSGi WeavingHook](https://docs.osgi.org/specification/osgi.core/8.0.0/framework.weavinghook.html) for lightweight bytecode instrumentation at class-load time.
It consists of a host bundle and fragment bundles discovered via Java SPI.

### Architecture

- **Host bundle** (`opentelemetry-osgi-weaving`): Contains the `WeavingHook`, `BundleActivator`, `ServiceTracker`-based `OpenTelemetryProxy`, and `WeaverRegistry` (Java SPI discovery)
- **Fragment bundles** (e.g., `opentelemetry-osgi-weaver-servlet`): Attach to the host via `Fragment-Host`, providing `Weaver` implementations registered in `META-INF/services`
- Fragments share the host's classloader, so plain `ServiceLoader.load(Weaver.class)` works without SPI Fly
- ASM 9.7.1 is embedded in the host bundle via `Private-Package` (org.objectweb.asm.*) to avoid resolution ordering and classloader visibility issues

### Key Design Decisions

- **BundleActivator, not DS**: The WeavingHook must be active before DS component classes load — using DS would miss weaving those classes
- **Fragment bundles for weavers**: Fragments share the host's classloader, enabling plain Java SPI without SPI Fly
- **ASM embedded**: Avoiding a separate ASM bundle eliminates resolution ordering issues and ensures ASM classes are always visible to the weaving code
- **SafeClassWriter**: Uses `COMPUTE_FRAMES` with the target bundle's classloader for frame computation. The default `ClassWriter.getCommonSuperClass()` calls `Class.forName()` which fails across OSGi classloader boundaries. `SafeClassWriter` overrides `getClassLoader()` to use `WovenClass.getBundleWiring().getClassLoader()`, falling back to `java/lang/Object` only when the target classloader cannot resolve a type. This is critical for instrumenting complex classes (e.g., H2 `JdbcPreparedStatement`) where incorrect frame merging causes `VerifyError`.
- **Infrastructure bundle exclusion**: Skips weaving for Felix, Karaf, Jetty, Pax, Aries, CXF, XBean, and Eclipse Equinox bundles to prevent instrumenting container internals
- **OpenTelemetryProxy**: Uses `ServiceTracker` to gracefully handle the OpenTelemetry service not being available yet; falls back to noop tracers/meters
- **Start-level 20**: Weaving bundles are installed at start-level 20 in the Karaf feature to ensure they activate before application bundles

### Servlet Weaver Details

- Instruments `javax.servlet.http.HttpServlet` subclasses (skips `javax.servlet.*` classes themselves)
- Targets methods: `service`, `doGet`, `doPost`, `doPut`, `doDelete`, `doHead`, `doOptions`, `doTrace`
- Creates `SERVER` spans with `http.method`, `http.url`, `http.status_code`, `http.servlet.class`, `http.query_string` attributes
- Records `http.server.requests` counter and `http.server.duration` histogram metrics
- `ServletInstrumentationHelper` provides static methods called from woven bytecode (`onServiceEnter`, `onServiceExit`, `onServiceError`)
- The instrumentation scope is `org.eclipse.osgi.technology.incubator.opentelemetry.weaver.servlet`

### JDBC Weaver Details

- Instruments classes implementing `java.sql.Statement`, `java.sql.PreparedStatement`, or `java.sql.CallableStatement`
- Skips JDBC API classes themselves (packages starting with `java.sql` or `javax.sql`)
- Targets methods: `execute`, `executeQuery`, `executeUpdate`, `executeBatch`, `executeLargeUpdate`, `executeLargeBatch`
- Creates `CLIENT` spans with `db.system`, `db.operation`, `db.statement`, `db.jdbc.driver_class` attributes
- Span names derived from SQL statement type (SELECT, INSERT, UPDATE, DELETE) or fall back to `JDBC <operation>`
- Records `db.client.operations` counter and `db.client.duration` histogram metrics
- `JdbcInstrumentationHelper` provides static methods called from woven bytecode (`onExecuteEnter`, `onExecuteExit`, `onExecuteError`)
- The instrumentation scope is `org.eclipse.osgi.technology.incubator.opentelemetry.weaver.jdbc`

### JAX-RS Weaver Details

- Instruments classes annotated with `@javax.ws.rs.Path` (detected via ASM `AnnotationVisitor` — no JAX-RS compile-time dependency)
- Only instruments methods annotated with HTTP method annotations (`@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH`, `@HEAD`, `@OPTIONS`)
- Combines class-level and method-level `@Path` values to compute the full route
- Creates `INTERNAL` spans (not `SERVER` — the outer servlet span already provides `SERVER` kind)
- Span attributes: `http.method`, `http.route`, `jaxrs.resource.class`, `jaxrs.resource.method`
- Records `jaxrs.server.requests` counter and `jaxrs.server.duration` histogram metrics
- `JaxRsInstrumentationHelper` provides static methods called from woven bytecode (`onMethodEnter`, `onMethodExit`, `onMethodError`)
- The instrumentation scope is `org.eclipse.osgi.technology.incubator.opentelemetry.weaver.jaxrs`

### SCR Lifecycle Weaver Details

- Inspired by [biz.aQute.trace](https://github.com/aQute-os/biz.aQute.osgi.util), uses DS XML parsing to identify component classes and their lifecycle methods
- Reads the `Service-Component` manifest header, parses XML files, extracts `<implementation class="...">` FQNs and `<component>` element attributes
- Parses lifecycle method names from XML attributes with DS spec defaults: `activate` (default "activate"), `deactivate` (default "deactivate"), `modified` (no default), `init` (default 0)
- **No annotation scanning** — the DS XML descriptor is the single source of truth, as annotations are not mandatory for DS components
- Caches parsed `ComponentDescriptor` records per bundle ID in a `ConcurrentHashMap` for efficient `canWeave()` checks
- Handles wildcard patterns in `Service-Component` header (e.g., `OSGI-INF/*.xml`) via `Bundle.findEntries()`
- Matches methods by name from the XML descriptor; matches constructors when `init > 0` (constructor injection)
- Creates `INTERNAL` spans with `scr.component.name`, `scr.component.class`, `scr.lifecycle.action`, `scr.method.name` attributes
- Span names: `scr.<action> <SimpleClassName>` (e.g., `scr.activate HealthCheckInventoryComponent`)
- Records `scr.lifecycle.operations` counter and `scr.lifecycle.duration` histogram metrics
- `ScrInstrumentationHelper` provides static methods called from woven bytecode (`onLifecycleEnter`, `onLifecycleExit`, `onLifecycleError`)
- The instrumentation scope is `org.eclipse.osgi.technology.incubator.opentelemetry.weaver.scr`
- **Note**: Early component activations (before the OpenTelemetry service is registered) produce noop spans — this is the expected behavior of the `OpenTelemetryProxy`

### Build Differences from DS Modules

- Uses `Bundle-Activator` header instead of DS annotations
- Host bundle: bnd `Private-Package` includes ASM classes; `Import-Package` uses optional resolution for OpenTelemetry API
- Fragment bundle: bnd `Fragment-Host: opentelemetry-osgi-weaving` header; dependencies are `provided` scope (resolved via host)

### Adding New Weavers

1. Create a new module as a fragment bundle (`Fragment-Host: opentelemetry-osgi-weaving`)
2. Implement the `Weaver` interface (`name()`, `canWeave()`, `weave()`)
3. Use `SafeClassWriter` (from the host bundle) instead of plain `ClassWriter` when creating the ASM `ClassWriter`
4. Register via `META-INF/services/org.eclipse.osgi.technology.incubator.opentelemetry.weaving.Weaver`
5. Add the bundle to the integration feature descriptor at start-level 20

## Screenshots and Documentation

### Screenshot Generation

Per-section dashboard screenshots are stored in `doc/images/` and referenced from READMEs.
They are generated from the Grafana image renderer running in the Docker Compose stack.

To regenerate screenshots:

```bash
# Ensure the Docker stack is running
docker compose up --build -d

# Wait for data to populate (~2 minutes)

# Render the full dashboard
curl -s 'http://localhost:3000/render/d/osgi-overview/osgi-observability-overview?orgId=1&width=1800&height=4500&from=now-15m&to=now&kiosk=true' -o /tmp/dashboard-full.png

# Crop per-section images using Python PIL (see doc/crop-screenshots.py or use manual crops)
```

### Screenshot Files

| File | Dashboard Section |
|---|---|
| `grafana-dashboard-overview.png` | Full dashboard (may be truncated at ~3000px) |
| `grafana-osgi-framework.png` | 🧩 OSGi Framework (bundles, services) |
| `grafana-scr.png` | ⚙️ Declarative Services (component states) |
| `grafana-log-service.png` | 📋 OSGi Log Service (log entries by level) |
| `grafana-demo-operations.png` | 🚀 Demo Operations (operation rates, durations) |
| `grafana-health-checks.png` | 🏥 Felix Health Checks (status, durations) |
| `grafana-config-admin.png` | 🔧 Config Admin (configs, events) |
| `grafana-typed-events.png` | 📨 Typed Events (events by topic, handler counts) |
| `grafana-http-weaving.png` | 🌐 HTTP Servlet / Weaving (requests, latency) |
| `grafana-scr-lifecycle.png` | ⚙️ SCR Lifecycle / Weaving (activations, durations) |
| `grafana-recent-traces.png` | 🔍 Recent Traces (trace table) |
| `grafana-live-logs.png` | 📝 Live Logs (Loki stream) |
| `grafana-jvm-mxbeans-overview.png` | Full JVM MXBeans dashboard |
| `grafana-jvm-memory.png` | 💾 JVM Memory (heap, non-heap, uptime) |
| `grafana-jvm-cpu.png` | 🖥️ JVM CPU (process load, system load, processors) |
| `grafana-jvm-threads.png` | 🧵 JVM Threads (live, daemon, peak, started) |
| `grafana-jvm-gc.png` | ♻️ GC (collection count, time per collector) |
| `grafana-jvm-classloading.png` | 📦 Class Loading (loaded, unloaded, total) |
| `grafana-jvm-memory-pools.png` | 🏊 Memory Pools (per-pool usage) |
| `grafana-jvm-buffer-pools.png` | 📋 Buffer Pools (count, memory, capacity) |

### README Screenshot Policy

- The main `README.md` includes per-section screenshots in the dashboard section.
- Subfolder READMEs (`integrations/`, `weaving/`, `demo/`) include context-specific dashboard screenshots relevant to their modules.
- Screenshots use relative paths (`../doc/images/grafana-*.png` from subfolders, `doc/images/grafana-*.png` from root).
- When adding new dashboard rows or integrations, regenerate and update the relevant screenshots.
- When modifying dashboard panel layouts, update the crop coordinates in the screenshot generation script.

## Common Pitfalls

- **`package-info.java`**: The Javadoc comment must come before the `package` declaration — do not repeat the `package` statement
- **OSGi scope**: OSGi dependencies must be `provided` scope in runtime/integration/demo modules (the framework provides them at runtime)
- **bnd-maven-plugin + maven-jar-plugin**: Both are configured in the parent POM; the jar plugin reads the bnd-generated `MANIFEST.MF`. The agent and karaf-feature modules skip bnd.
- **Non-bundle modules**: Agent (`<packaging>jar</packaging>` with bnd disabled) and karaf-features (`<packaging>feature</packaging>`) are not OSGi bundles.
- **OTel JARs are NOT OSGi bundles**: They lack `Bundle-SymbolicName` headers. In Karaf, they are wrapped via the `wrap:` protocol in the feature descriptor with SPI Fly headers.
- **SPI Fly**: OpenTelemetry uses `ServiceLoader` internally. In OSGi, cross-bundle SPI requires Apache Aries SPI Fly. Add `SPI-Consumer=*` / `SPI-Provider=*` headers to wrapped bundles and depend on the `spifly` Karaf feature.
- **OTLP exporter**: The runtime auto-detects OTLP mode from the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable and uses its value as the endpoint. When not set, falls back to `config.otlpEndpoint()` (default `http://localhost:4318`).
- **Prometheus listen port**: Prometheus is configured to listen on port 9080 (`--web.listen-address=0.0.0.0:9080`) instead of the default 9090, matching the OTel Collector's remote-write target and the docker-compose port mapping.
- **Grafana datasource UIDs**: Datasources use explicit stable UIDs (`tempo`, `prometheus`, `loki`) in the provisioning config. Always reference these UIDs in dashboard JSON — do not use auto-generated UIDs.
- **Docker uses pre-built distribution**: The `docker/Dockerfile` builds with Maven, then copies the assembled Karaf distribution from `features/opentelemetry-osgi-karaf-distribution/target/assembly` — all features, bundles, and config are pre-embedded
- **No sed hacking in Docker**: The karaf-assembly plugin handles `featuresBoot` and `featuresRepositories` configuration — no manual `sed` manipulation needed
- **bnd osgi.service requirements**: The `-dsannotations-options: norequirements` bnd setting is required to suppress `Require-Capability: osgi.service` headers that break Karaf's feature resolver
- **Relative paths**: Module POMs use `<relativePath>../../pom.xml</relativePath>` since modules are two levels deep (e.g. `core/opentelemetry-osgi-runtime/pom.xml`)
- **Maven groupId path**: The new groupId `org.eclipse.osgi-technology.incubator` maps to `org/eclipse/osgi-technology/incubator/` in Maven repository layout (note: hyphen in path)
- **Felix HC executor defaultTags**: The `HealthCheckExecutorImpl` defaults to `defaultTags=["default"]` when `HealthCheckSelector.empty()` is used. This silently filters out checks tagged differently (e.g., `systemalive`). Deploy `org.apache.felix.hc.core.impl.executor.HealthCheckExecutorImpl.cfg` with `defaultTags=` (empty) to select ALL checks.
- **Felix HC general checks configurationPolicy**: Most general checks use `configurationPolicy=REQUIRE` — they will NOT activate without a `.cfg` file in `${karaf.etc}/`. Only `FrameworkStartCheck` has `OPTIONAL` policy. Factory checks (BundlesStartedCheck, DiskSpaceCheck) need `<PID>-<instance>.cfg` naming.
- **ConfigurationEvent has no properties**: `ConfigurationEvent.getReference()` returns the CM `ServiceReference`, not the configuration's properties. To get property values, fetch via `ConfigurationAdmin.getConfiguration(pid)`.
- **ConfigAdmin listConfigurations null**: `configAdmin.listConfigurations(null)` returns `null` when no configurations exist, not an empty array. Always null-check the result.
- **Weaving hook activation order**: The weaving host bundle uses `BundleActivator` (not DS) because the `WeavingHook` must be registered before DS component classes load. Do not convert it to DS.
- **Weaving ASM embedded**: ASM is included via `Private-Package` in the weaving host bundle. Do not add a separate ASM bundle — it would cause classloader visibility issues.
- **Weaving fragment SPI**: Fragment bundles register weavers via `META-INF/services`, not OSGi service registry. This works because fragments share the host's classloader, making `ServiceLoader.load()` discover them without SPI Fly.
- **Weaving SafeClassWriter**: All weavers must use `SafeClassWriter` (from the weaving host) instead of plain `ClassWriter` with `COMPUTE_FRAMES`. `SafeClassWriter` uses the target bundle's classloader (via `WovenClass.getBundleWiring().getClassLoader()`) for frame computation. Without this, complex classes (e.g., H2 `JdbcPreparedStatement`) cause `VerifyError: Bad type on operand stack` because incorrect type merging corrupts stack map frames in non-instrumented methods.
- **Weaving start-level**: Weaving bundles must be at start-level 20 (before application bundles) in Karaf feature descriptors. Higher start-levels would cause application classes to load before the WeavingHook is registered.
- **Weaving infrastructure exclusion**: The `OpenTelemetryWeavingHook` skips bundles from Felix, Karaf, Jetty, Pax, Aries, CXF, XBean, and Eclipse Equinox. When adding new infrastructure exclusions, update the `shouldSkipBundle()` method.
- **SCR weaver DS XML parsing**: The SCR lifecycle weaver parses the `Service-Component` manifest header and DS XML files to identify component implementation classes and lifecycle method names. It uses XML attributes with DS spec defaults (`activate`="activate", `deactivate`="deactivate", `modified`=none, `init`=0) — no annotation scanning is performed. Results are cached per bundle ID as `ComponentDescriptor` records.
- **SCR weaver noop early activations**: Components that activate before the OpenTelemetry service is registered will have their lifecycle methods instrumented but produce noop spans (the `OpenTelemetryProxy` returns noop tracers/meters until the real service arrives). This is expected — the OTel runtime is itself a DS component, so there's an inherent chicken-and-egg ordering.
- **MXBeans com.sun.management**: The MXBeans module uses `com.sun.management.OperatingSystemMXBean` for process/system CPU load and physical memory. This must be imported with `resolution:=optional` in bnd config, as it's a JVM-internal package that the OSGi resolver cannot satisfy. The code uses `instanceof` to degrade gracefully on non-HotSpot JVMs.
- **MXBeans OTel unit naming**: OTel metrics with unit `By` (bytes) become `_bytes` in Prometheus, and `ms` (milliseconds) becomes `_milliseconds`. Dashboard queries must use the Prometheus-converted names (e.g., `jvm_memory_used_bytes` not `jvm_memory_used_By`).
- **TypedEvent UntypedEventHandler marks events handled**: The Typed Event integration registers `UntypedEventHandler` services with `event.topics=*`. Per the spec, this makes ALL events "handled", so `UnhandledEventHandler` services registered by other bundles will never fire. This is a conscious trade-off for simpler implementation over `TypedEventMonitor`.
- **TypedEvent Aries Bus uses Component DSL**: The Apache Aries TypedEvent Bus implementation (`org.apache.aries.typedevent.bus`) does NOT use Declarative Services — it uses Aries Component DSL. This means 5 additional transitive dependencies are needed: Component DSL, Converter, PushStream, Promise, Function.
- **TypedEvent runtime deps are all OSGi bundles**: Unlike OTel JARs, all TypedEvent dependencies (Aries bus, OSGi util packages) have proper `Bundle-SymbolicName` headers. No `wrap:` protocol needed in feature descriptors.
