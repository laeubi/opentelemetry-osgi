# ==============================================================================
# Dockerfile for the OpenTelemetry OSGi Demo
#
# Uses the pre-built Karaf distribution from the karaf-assembly module.
# All bundles, features, and configuration are already embedded.
#
# Build:
#   mvn clean install -DskipTests
#   docker compose up --build -d
# ==============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build the Maven project (including the Karaf distribution)
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy POM files first for dependency caching
COPY pom.xml .
COPY core/pom.xml core/
COPY core/opentelemetry-osgi-runtime/pom.xml core/opentelemetry-osgi-runtime/
COPY integrations/pom.xml integrations/
COPY integrations/opentelemetry-osgi-framework/pom.xml integrations/opentelemetry-osgi-framework/
COPY integrations/opentelemetry-osgi-scr/pom.xml integrations/opentelemetry-osgi-scr/
COPY integrations/opentelemetry-osgi-log/pom.xml integrations/opentelemetry-osgi-log/
COPY integrations/opentelemetry-osgi-cm/pom.xml integrations/opentelemetry-osgi-cm/
COPY integrations/opentelemetry-osgi-felix-healthcheck/pom.xml integrations/opentelemetry-osgi-felix-healthcheck/
COPY integrations/opentelemetry-osgi-mxbeans/pom.xml integrations/opentelemetry-osgi-mxbeans/
COPY demo/pom.xml demo/
COPY demo/opentelemetry-osgi-demo/pom.xml demo/opentelemetry-osgi-demo/
COPY features/pom.xml features/
COPY features/opentelemetry-osgi-karaf-feature/pom.xml features/opentelemetry-osgi-karaf-feature/
COPY features/opentelemetry-osgi-integration-karaf-feature/pom.xml features/opentelemetry-osgi-integration-karaf-feature/
COPY features/opentelemetry-osgi-demo-karaf-feature/pom.xml features/opentelemetry-osgi-demo-karaf-feature/
COPY features/opentelemetry-osgi-karaf-distribution/pom.xml features/opentelemetry-osgi-karaf-distribution/
COPY incubator/pom.xml incubator/
COPY incubator/opentelemetry-osgi-agent/pom.xml incubator/opentelemetry-osgi-agent/
COPY weaving/pom.xml weaving/
COPY weaving/opentelemetry-osgi-weaving/pom.xml weaving/opentelemetry-osgi-weaving/
COPY weaving/opentelemetry-osgi-weaver-servlet/pom.xml weaving/opentelemetry-osgi-weaver-servlet/
COPY weaving/opentelemetry-osgi-weaver-jdbc/pom.xml weaving/opentelemetry-osgi-weaver-jdbc/
COPY weaving/opentelemetry-osgi-weaver-jaxrs/pom.xml weaving/opentelemetry-osgi-weaver-jaxrs/
RUN mvn dependency:go-offline -q -B 2>/dev/null || true

# Build the entire project including the Karaf distribution assembly
COPY . .
RUN mvn clean install -DskipTests -q -B

# ---------------------------------------------------------------------------
# Stage 2: Lightweight runtime — just the pre-assembled Karaf distribution
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Copy the fully assembled Karaf distribution (includes all bundles, features, config)
COPY --from=build /workspace/features/opentelemetry-osgi-karaf-distribution/target/assembly /opt/karaf

ENV KARAF_HOME=/opt/karaf
ENV PATH="${KARAF_HOME}/bin:${PATH}"

# Disable remote Maven repositories — all artifacts are pre-embedded in system/
RUN sed -i 's|^org.ops4j.pax.url.mvn.repositories=|#org.ops4j.pax.url.mvn.repositories=|' \
    ${KARAF_HOME}/etc/org.ops4j.pax.url.mvn.cfg

# OTLP configuration is injected via environment variables at runtime.
# The distribution includes a default config with exporterType=logging.
# When OTEL_EXPORTER_OTLP_ENDPOINT is set, the runtime auto-switches to OTLP.

EXPOSE 8101 1099 44444 8181

CMD ["karaf", "run"]
