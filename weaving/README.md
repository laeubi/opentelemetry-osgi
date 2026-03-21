# OpenTelemetry OSGi Weaving

This folder contains the OSGi Weaving Hook based bytecode instrumentation infrastructure for OpenTelemetry.
It provides zero-code instrumentation of application bundles at class-load time using the [OSGi WeavingHook](https://docs.osgi.org/specification/osgi.core/8.0.0/framework.weavinghook.html) mechanism and [ASM](https://asm.ow2.io/) bytecode manipulation.

## Architecture

The weaving system uses a **host bundle + fragment bundle** architecture:

```
opentelemetry-osgi-weaving (host bundle)
├── WeavingHookActivator        — BundleActivator registering the WeavingHook
├── OpenTelemetryWeavingHook    — OSGi WeavingHook delegating to discovered weavers
├── WeaverRegistry              — ServiceLoader-based discovery of Weaver implementations
├── OpenTelemetryProxy          — Graceful proxy for OpenTelemetry service (noop fallback)
├── Weaver                      — SPI interface for weaver implementations
└── ASM 9.7.1 (embedded)       — Private-Package to avoid classloader issues

opentelemetry-osgi-weaver-servlet (fragment bundle)
├── ServletWeaver               — Weaver implementation targeting HttpServlet subclasses
├── ServletClassVisitor         — ASM ClassVisitor identifying servlet handler methods
├── ServletServiceMethodVisitor — ASM AdviceAdapter injecting instrumentation bytecode
├── ServletInstrumentationHelper — Static helper methods called from woven bytecode
└── META-INF/services/...Weaver — Java SPI registration
```

Fragment bundles share the host bundle's classloader, so plain Java `ServiceLoader` works without SPI Fly.
The host discovers all weaver implementations at startup and delegates class transformations to matching weavers.

### Class Load Flow

```
OSGi framework loads a class
    │
    ▼
OpenTelemetryWeavingHook.weave(WovenClass)
    │
    ├── Skip infrastructure bundles (Felix, Karaf, Jetty, Pax, Aries, CXF, XBean)
    │
    ├── For each registered Weaver:
    │     ├── weaver.canWeave(className, wovenClass) → yes/no
    │     └── weaver.weave(wovenClass, telemetryProxy) → transform bytecode
    │
    └── Transformed class continues loading
```

### Why BundleActivator (not Declarative Services)?

The WeavingHook must be registered **before** DS component classes are loaded.
If DS were used, the hook would miss weaving DS-managed classes because those classes load during DS component activation.
Using `BundleActivator` ensures the hook is active from the earliest possible point in the bundle lifecycle.

## Modules

### opentelemetry-osgi-weaving

The host bundle providing the weaving infrastructure.

- **WeavingHook** — Registered as an OSGi service; called by the framework for every class load
- **WeaverRegistry** — Discovers `Weaver` implementations via `ServiceLoader.load(Weaver.class)`
- **OpenTelemetryProxy** — Uses `ServiceTracker` to track the `OpenTelemetry` service; returns noop tracers/meters when the service is unavailable
- **ASM embedded** — ASM bytecode library is included as `Private-Package` to avoid resolution ordering and classloader visibility issues
- **Infrastructure bundle exclusion** — Skips weaving for Felix, Karaf, Jetty, Pax, Aries, CXF, XBean, and Eclipse Equinox bundles

### opentelemetry-osgi-weaver-servlet

A fragment bundle attaching to the weaving host that instruments `javax.servlet.http.HttpServlet` subclasses.

#### Instrumented Methods

`service`, `doGet`, `doPost`, `doPut`, `doDelete`, `doHead`, `doOptions`, `doTrace` — any method with the signature `(HttpServletRequest, HttpServletResponse)`.

#### Generated Telemetry

**Traces** (span kind: `SERVER`):

| Attribute | Description |
|---|---|
| `http.method` | HTTP method (GET, POST, …) |
| `http.url` | Full request URL |
| `http.query_string` | Query string (if present) |
| `http.servlet.class` | Fully qualified servlet class name |
| `http.status_code` | Response status code |

Span status is set to `ERROR` for status codes ≥ 400.
Exceptions are recorded on the span.

**Metrics**:

| Metric | Type | Description |
|---|---|---|
| `http.server.requests` | Counter | Total HTTP request count (with `http_status_code` label) |
| `http.server.duration` | Histogram | Request duration in milliseconds |

#### Bytecode Transformation

The servlet weaver injects instrumentation around each HTTP handler method:

```java
// Injected by ServletServiceMethodVisitor
Object[] spanAndScope = ServletInstrumentationHelper.onServiceEnter(request, "com.example.MyServlet");
try {
    // original method body
} catch (Throwable t) {
    ServletInstrumentationHelper.onServiceError(spanAndScope, t);
    throw t;
} finally {
    ServletInstrumentationHelper.onServiceExit(spanAndScope, response);
}
```

The `ClassWriter` uses `COMPUTE_FRAMES` with a safe `getCommonSuperClass()` override that returns `java/lang/Object` to handle OSGi classloader boundaries where the default implementation would fail.

## Adding New Weavers

To add instrumentation for a new target (e.g., JAX-RS, JDBC):

1. Create a new module as a fragment bundle attaching to `opentelemetry-osgi-weaving`
2. Implement the `Weaver` interface:
   - `name()` — human-readable name for logging
   - `canWeave(className, wovenClass)` — return `true` for classes to instrument
   - `weave(wovenClass, telemetryProxy)` — transform bytecode using ASM
3. Register via Java SPI: `META-INF/services/org.eclipse.osgi.technology.incubator.opentelemetry.weaving.Weaver`
4. Set `Fragment-Host: opentelemetry-osgi-weaving` in the bnd configuration
5. Add the bundle to the integration feature descriptor at start-level 20

## Demo

The [demo module](../demo/README.md) includes HTTP servlet demonstrations:

- **HttpDemoServlet** — Servlet registered at `/demo/*` via OSGi HTTP Whiteboard with three endpoints:
  - `GET /demo` — Returns JSON status response
  - `GET /demo/slow` — Simulates a 500ms delay
  - `GET /demo/error` — Returns HTTP 500 error
- **HttpTrafficGeneratorComponent** — Periodically hits the demo endpoints every 10 seconds to generate telemetry data

The demo servlet is automatically instrumented by the weaving hook — no code changes required.
Access the servlet at [http://localhost:8181/demo](http://localhost:8181/demo) when running the Docker demo.

## Dashboard Preview

The Docker demo ships with a pre-built Grafana dashboard.
Below is the HTTP Servlet (Weaving) section showing live metrics from the demo servlet:

![HTTP Servlet Weaving Dashboard](../doc/images/grafana-http-weaving.png)
