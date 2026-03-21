# AGENTS.md - Instructions for AI Agents

This file contains instructions and context for AI agents working on this codebase.

## Project Overview

This is a Maven multi-module project integrating OpenTelemetry with OSGi.
The project is organized into five top-level folders, each containing related modules.

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
│           ├── OpenTelemetryService.java        # DS component publishing OpenTelemetry
│           └── OpenTelemetryConfiguration.java  # ConfigAdmin configuration annotation
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
│           └── DemoSchedulerComponent.java            # Periodic telemetry generator
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
| Demo | `opentelemetry-osgi-demo` | `demo/` |
| Runtime Feature | `opentelemetry-osgi-karaf-feature` | `features/` |
| Integration Feature | `opentelemetry-osgi-integration-karaf-feature` | `features/` |
| Demo Feature | `opentelemetry-osgi-demo-karaf-feature` | `features/` |
| Karaf Distribution | `opentelemetry-osgi-karaf-distribution` | `features/` |
| Agent Extension | `opentelemetry-osgi-agent` | `incubator/` |

### Aggregator POMs

Each subfolder has an aggregator POM that references the root parent:

| Folder | ArtifactId |
|---|---|
| `core/` | `opentelemetry-osgi-core-parent` |
| `integrations/` | `opentelemetry-osgi-integrations-parent` |
| `demo/` | `opentelemetry-osgi-demo-parent` |
| `features/` | `opentelemetry-osgi-features-parent` |
| `incubator/` | `opentelemetry-osgi-incubator-parent` |

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
- `OTEL_EXPORTER_OTLP_ENDPOINT` — Triggers OTLP export mode in the runtime (default: `http://otel-collector:4318`)
- `OTEL_SERVICE_NAME` — Overrides the `service.name` resource attribute

**Data flow**: OSGi App → OTel Collector (OTLP/HTTP) → Tempo + Prometheus + Loki → Grafana

When modifying the OTel Collector pipeline, edit `docker/otel-collector-config.yaml`.
Grafana datasources are auto-provisioned from `docker/grafana/provisioning/datasources/datasources.yaml`.

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
| `bnd.version` | `7.1.0` | bnd-maven-plugin |

When updating OpenTelemetry version, update the `opentelemetry.version` property — all module dependencies are managed via the BOM.

## Feature Architecture

The project uses three separate Karaf feature modules, each producing its own feature descriptor:

### opentelemetry-osgi-karaf-feature (Runtime)

- Defines `opentelemetry-deps` (14 wrapped OTel SDK JARs + SPI Fly) and `opentelemetry-osgi` (runtime bundle + config)
- Inline `<config>` element provides default configuration for PID `org.eclipse.osgi.technology.incubator.opentelemetry.runtime`
- Example `.cfg` file in `src/main/resources/` for manual deployment to `${karaf.etc}/`

### opentelemetry-osgi-integration-karaf-feature (Integrations)

- Defines `opentelemetry-osgi-integrations` (framework + scr + log bundles)
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

## Common Pitfalls

- **`package-info.java`**: The Javadoc comment must come before the `package` declaration — do not repeat the `package` statement
- **OSGi scope**: OSGi dependencies must be `provided` scope in runtime/integration/demo modules (the framework provides them at runtime)
- **bnd-maven-plugin + maven-jar-plugin**: Both are configured in the parent POM; the jar plugin reads the bnd-generated `MANIFEST.MF`. The agent and karaf-feature modules skip bnd.
- **Non-bundle modules**: Agent (`<packaging>jar</packaging>` with bnd disabled) and karaf-features (`<packaging>feature</packaging>`) are not OSGi bundles.
- **OTel JARs are NOT OSGi bundles**: They lack `Bundle-SymbolicName` headers. In Karaf, they are wrapped via the `wrap:` protocol in the feature descriptor with SPI Fly headers.
- **SPI Fly**: OpenTelemetry uses `ServiceLoader` internally. In OSGi, cross-bundle SPI requires Apache Aries SPI Fly. Add `SPI-Consumer=*` / `SPI-Provider=*` headers to wrapped bundles and depend on the `spifly` Karaf feature.
- **OTLP exporter**: The runtime auto-detects OTLP mode from the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable — no config change needed
- **Docker uses pre-built distribution**: The `docker/Dockerfile` builds with Maven, then copies the assembled Karaf distribution from `features/opentelemetry-osgi-karaf-distribution/target/assembly` — all features, bundles, and config are pre-embedded
- **No sed hacking in Docker**: The karaf-assembly plugin handles `featuresBoot` and `featuresRepositories` configuration — no manual `sed` manipulation needed
- **bnd osgi.service requirements**: The `-dsannotations-options: norequirements` bnd setting is required to suppress `Require-Capability: osgi.service` headers that break Karaf's feature resolver
- **Relative paths**: Module POMs use `<relativePath>../../pom.xml</relativePath>` since modules are two levels deep (e.g. `core/opentelemetry-osgi-runtime/pom.xml`)
- **Maven groupId path**: The new groupId `org.eclipse.osgi-technology.incubator` maps to `org/eclipse/osgi-technology/incubator/` in Maven repository layout (note: hyphen in path)
