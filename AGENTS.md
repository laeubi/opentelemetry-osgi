# AGENTS.md - Instructions for AI Agents

This file contains instructions and context for AI agents working on this codebase.

## Project Overview

This is a Maven multi-module project integrating OpenTelemetry with OSGi.
It contains three modules that serve different integration approaches.

## Repository Structure

```
opentelemetry-osgi/
├── pom.xml                          # Parent POM with dependency management
├── opentelemetry-osgi-runtime/      # OSGi service providing OpenTelemetry SDK
│   └── src/main/java/io/opentelemetry/osgi/runtime/
│       ├── OpenTelemetryService.java        # DS component publishing OpenTelemetry
│       └── OpenTelemetryConfiguration.java  # ConfigAdmin configuration annotation
├── opentelemetry-osgi-client/       # Demo bundle consuming OpenTelemetry service
│   └── src/main/java/io/opentelemetry/osgi/client/
│       ├── TracingDemoComponent.java              # Tracing demos
│       ├── MetricsDemoComponent.java              # Metrics demos
│       ├── LogBridgeDemoComponent.java            # Log bridge demos
│       └── ContextPropagationDemoComponent.java   # Context propagation demos
├── opentelemetry-osgi-agent/        # Java Agent extension for OSGi instrumentation
│   └── src/main/java/io/opentelemetry/osgi/agent/
│       ├── OsgiAgentExtension.java          # Main extension entry point
│       ├── OsgiResourceProvider.java        # Resource attributes from OSGi
│       ├── OsgiMetricsProvider.java         # Framework metrics
│       ├── OsgiEventListener.java           # Bundle/service event tracing
│       ├── OsgiBundleInventoryLogger.java   # Bundle inventory logging
│       ├── OsgiFrameworkAccess.java         # OSGi API access helper
│       └── BundleInfo.java                  # Bundle state record
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
| `osgi.annotation.bundle.version` | `2.0.0` | Bundle annotations |
| `bnd.version` | `7.1.0` | bnd-maven-plugin |

When updating OpenTelemetry version, update the `opentelemetry.version` property — all module dependencies are managed via the BOM.

## Agent Module Notes

The agent module is fundamentally different from the runtime/client modules:

- It is **not** an OSGi bundle — it is a Java Agent extension JAR
- It uses `maven-shade-plugin` to create an uber-JAR with all dependencies
- It registers providers via SPI files in `META-INF/services/`
- It accesses OSGi via `FrameworkUtil.getBundle()` and handles cases where OSGi is not available
- All OSGi access is isolated in `OsgiFrameworkAccess` to avoid `ClassNotFoundException` at load time

## Common Pitfalls

- **`package-info.java`**: The Javadoc comment must come before the `package` declaration — do not repeat the `package` statement
- **OSGi scope**: OSGi dependencies must be `provided` scope in runtime/client modules (the framework provides them at runtime)
- **bnd-maven-plugin + maven-jar-plugin**: Both are configured in the parent POM; the jar plugin reads the bnd-generated `MANIFEST.MF`
- **Shading in agent module**: The shade plugin runs after the regular jar plugin and replaces the artifact
