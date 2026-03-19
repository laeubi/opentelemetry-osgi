# AGENTS.md - Instructions for AI Agents

This file contains instructions and context for AI agents working on this codebase.

## Project Overview

This is a Maven multi-module project integrating OpenTelemetry with OSGi.
It contains six modules that serve different integration approaches.

## Repository Structure

```
opentelemetry-osgi/
├── pom.xml                          # Parent POM with dependency management
├── docker-compose.yml               # Full Grafana observability stack
├── docker/
│   ├── Dockerfile                   # Multi-stage build (Maven → Felix runtime)
│   ├── felix-config.properties      # Felix OSGi config with system package exports
│   ├── otel-collector-config.yaml   # OTel Collector pipeline config
│   ├── tempo.yaml                   # Grafana Tempo config
│   ├── prometheus.yml               # Prometheus config
│   ├── loki.yaml                    # Grafana Loki config
│   └── grafana/provisioning/        # Grafana auto-provisioned datasources
├── opentelemetry-osgi-runtime/      # OSGi service providing OpenTelemetry SDK
│   └── src/main/java/io/opentelemetry/osgi/runtime/
│       ├── OpenTelemetryService.java        # DS component publishing OpenTelemetry
│       └── OpenTelemetryConfiguration.java  # ConfigAdmin configuration annotation
├── opentelemetry-osgi-core/         # Core framework bridge (bundles, services, events)
│   └── src/main/java/io/opentelemetry/osgi/core/
│       ├── FrameworkMetricsComponent.java    # Bundle/service count gauges
│       ├── FrameworkEventComponent.java      # Bundle/service event tracing
│       ├── BundleInventoryComponent.java     # Live bundle inventory (snapshot + changes)
│       ├── ServiceInventoryComponent.java    # Live service inventory (snapshot + changes)
│       ├── BundleStateUtil.java              # Bundle state name utility
│       └── BundleInfo.java                   # Bundle state record
├── opentelemetry-osgi-client/       # Demo bundle consuming OpenTelemetry service
│   └── src/main/java/io/opentelemetry/osgi/client/
│       ├── TracingDemoComponent.java              # Tracing demos
│       ├── MetricsDemoComponent.java              # Metrics demos
│       ├── LogBridgeDemoComponent.java            # Log bridge demos
│       ├── ContextPropagationDemoComponent.java   # Context propagation demos
│       └── DemoSchedulerComponent.java            # Periodic telemetry generator
├── opentelemetry-osgi-agent/        # Java Agent extension (ByteBuddy instrumentation)
│   └── src/main/java/io/opentelemetry/osgi/agent/
│       ├── OsgiInstrumentationModule.java     # SPI entry point, classloader matcher
│       ├── FrameworkInstrumentation.java       # Intercepts Framework.init() for context capture
│       ├── BundleLifecycleInstrumentation.java # Traces Bundle.start/stop/update/uninstall
│       ├── BundleActivatorInstrumentation.java # Traces BundleActivator.start/stop
│       ├── BundleContextInstrumentation.java   # Traces registerService/installBundle
│       └── OsgiSingletons.java                # Static Tracer/Meter/Logger + metrics registration
├── opentelemetry-osgi-scr/          # SCR introspection → OpenTelemetry bridge
│   └── src/main/java/io/opentelemetry/osgi/scr/
│       ├── ScrMetricsComponent.java         # DS component state gauges
│       ├── ScrInventoryComponent.java       # DS inventory as structured logs
│       └── ScrHealthCheckComponent.java     # Periodic health trace for non-active components
├── opentelemetry-osgi-log/          # OSGi Log Service → OpenTelemetry bridge
│   └── src/main/java/io/opentelemetry/osgi/log/
│       ├── LogBridgeComponent.java          # Forwards LogEntry to OTel logs
│       └── LogMetricsComponent.java         # Log entry counters as OTel metrics
├── README.md
├── AGENTS.md                        # This file
└── LICENSE                          # EPL-2.0
```

## Build Commands

```bash
# Full build
mvn clean verify

# Build a single module
mvn clean verify -pl opentelemetry-osgi-runtime

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

The Docker demo uses an embedded Apache Felix 7.0.5 OSGi framework (not Karaf).
OpenTelemetry JARs are placed on the system classpath and their packages are exported to OSGi via `org.osgi.framework.system.packages.extra` in `docker/felix-config.properties`.

**Key environment variables** (set in `docker-compose.yml`):
- `OTEL_EXPORTER_OTLP_ENDPOINT` — Triggers OTLP export mode in the runtime (default: `http://otel-collector:4317`)
- `OTEL_SERVICE_NAME` — Overrides the `service.name` resource attribute

**Data flow**: OSGi App → OTel Collector (OTLP/gRPC) → Tempo + Prometheus + Loki → Grafana

When modifying the OTel Collector pipeline, edit `docker/otel-collector-config.yaml`.
When adding new system packages for OSGi, edit `docker/felix-config.properties`.
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

- **API vs SDK**: Client bundles depend only on `opentelemetry-api`; only the runtime bundle depends on `opentelemetry-sdk`
- **OTLP export**: The runtime supports both `logging` and `otlp` exporter types. OTLP is auto-selected when `OTEL_EXPORTER_OTLP_ENDPOINT` env var is set.
- **Sender**: Uses `opentelemetry-exporter-sender-jdk` (Java's built-in HttpClient) — no external HTTP library needed
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

## Agent Module Notes

The agent module uses the OTel Java Agent Extension API with ByteBuddy bytecode instrumentation.
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

### Dependencies

| Dependency | Scope | Purpose |
|---|---|---|
| `opentelemetry-javaagent-extension-api` | provided | InstrumentationModule, TypeInstrumentation, AgentElementMatchers |
| `byte-buddy` | provided | @Advice annotations, ElementMatchers |
| `opentelemetry-api` | provided | GlobalOpenTelemetry, Tracer, Meter, Logger |
| `org.osgi.framework` | provided | OSGi types referenced in advice code |

### Build Differences from OSGi Modules

- bnd-maven-plugin is **skipped** (not an OSGi bundle)
- maven-jar-plugin uses default manifest (not bnd-generated)
- No shade plugin (no runtime dependencies to bundle)

### Usage

```bash
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.javaagent.extensions=opentelemetry-osgi-agent-0.1.0-SNAPSHOT.jar \
     -jar your-osgi-application.jar
```

## Core Module Notes

The core module (`opentelemetry-osgi-core`) provides OSGi framework telemetry as proper DS components (complementary to the agent module which provides the same via bytecode instrumentation):

- Uses `@Reference OpenTelemetry` and `BundleContext` (injected via `@Activate`) — no `FrameworkUtil.getBundle()` workaround
- Registers as `BundleListener` and `ServiceListener` in `@Activate`, unregisters in `@Deactivate`
- `BundleInventoryComponent` uses `SynchronousBundleListener` to capture events before the framework proceeds; emits snapshot at activation + change log records for every bundle event
- `ServiceInventoryComponent` uses `ServiceListener` to track registrations/unregistrations/modifications; emits snapshot at activation + change log records with using-bundles info
- Async gauges use `ObservableLongGauge` with proper cleanup via `close()` on deactivate
- `BundleStateUtil` provides the `bundleStateToString()` utility shared across components
- `BundleInfo` record captures immutable bundle state snapshots

## SCR Module Notes

The SCR module uses the OSGi SCR Introspection API from `org.osgi.service.component.runtime`:

- References `ServiceComponentRuntime` to enumerate all DS component descriptions and configurations
- Uses `ComponentConfigurationDTO` state constants: `UNSATISFIED_CONFIGURATION=1`, `UNSATISFIED_REFERENCE=2`, `SATISFIED=4`, `ACTIVE=8`, `FAILED_ACTIVATION=16`
- The `configStateToString()` utility in `ScrMetricsComponent` maps state integers to human-readable names — reused by other SCR components
- Async gauges (via `ObservableLongGauge`) query component state on every metric collection cycle

## Log Module Notes

The Log module uses the OSGi Log Service from `org.osgi.service.log`:

- References `LogReaderService` and registers as a `LogListener` to capture real-time log entries
- Maps `LogLevel` (AUDIT, ERROR, WARN, INFO, DEBUG, TRACE) to OpenTelemetry `Severity`
- Enriches OTel log records with: bundle symbolic name/id/version, logger name, sequence number, thread info, service reference, source code location, exception details
- `LogMetricsComponent` maintains counters by log level and a dedicated error counter by bundle name
- Requires Felix Log Service bundle in the OSGi container (added as `org.apache.felix:org.apache.felix.log:1.3.0` in Docker)

## Common Pitfalls

- **`package-info.java`**: The Javadoc comment must come before the `package` declaration — do not repeat the `package` statement
- **OSGi scope**: OSGi dependencies must be `provided` scope in runtime/client modules (the framework provides them at runtime)
- **bnd-maven-plugin + maven-jar-plugin**: Both are configured in the parent POM; the jar plugin reads the bnd-generated `MANIFEST.MF`. The agent module skips bnd and overrides the jar plugin config.
- **Agent module is NOT an OSGi bundle**: It skips bnd-maven-plugin and uses default manifest. All deps are `provided` scope.
- **System packages in Docker**: When adding new OTel dependencies to the runtime, their packages must also be added to `docker/felix-config.properties` under `org.osgi.framework.system.packages.extra`
- **OTLP exporter**: The runtime auto-detects OTLP mode from the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable — no config change needed
- **Docker multi-stage build**: The `docker/Dockerfile` caches Maven dependencies separately from the source code for faster rebuilds
- **OTel JARs are NOT OSGi bundles**: They lack `Bundle-SymbolicName` headers. In the Docker demo, they are on the system classpath and exported as system packages.
