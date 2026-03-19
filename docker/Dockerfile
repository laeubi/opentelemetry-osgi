# ==============================================================================
# Multi-stage Dockerfile for the OpenTelemetry OSGi Demo
#
# Stage 1: Maven build
# Stage 2: Apache Karaf with our features pre-installed
# ==============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build the Maven project
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Cache dependencies first (improves rebuild speed)
COPY pom.xml .
COPY opentelemetry-osgi-runtime/pom.xml opentelemetry-osgi-runtime/
COPY opentelemetry-osgi-core/pom.xml opentelemetry-osgi-core/
COPY opentelemetry-osgi-client/pom.xml opentelemetry-osgi-client/
COPY opentelemetry-osgi-scr/pom.xml opentelemetry-osgi-scr/
COPY opentelemetry-osgi-log/pom.xml opentelemetry-osgi-log/
COPY opentelemetry-osgi-agent/pom.xml opentelemetry-osgi-agent/
COPY opentelemetry-osgi-karaf-feature/pom.xml opentelemetry-osgi-karaf-feature/
RUN mvn dependency:go-offline -q -B 2>/dev/null || true

# Build the project
COPY . .
RUN mvn clean install -DskipTests -q -B

# ---------------------------------------------------------------------------
# Stage 2: Karaf runtime with our feature pre-deployed
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

ARG KARAF_VERSION=4.4.7
ARG PROJECT_VERSION=0.1.0-SNAPSHOT
ARG OTEL_VERSION=1.49.0

# Download and install Apache Karaf
ADD https://archive.apache.org/dist/karaf/${KARAF_VERSION}/apache-karaf-${KARAF_VERSION}.tar.gz /tmp/karaf.tar.gz
RUN tar xzf /tmp/karaf.tar.gz -C /opt && \
    mv /opt/apache-karaf-${KARAF_VERSION} /opt/karaf && \
    rm /tmp/karaf.tar.gz

ENV KARAF_HOME=/opt/karaf
ENV PATH="${KARAF_HOME}/bin:${PATH}"

# ---- Populate Karaf's system Maven repository with our artifacts ----

# Helper: Maven repo layout base paths
# Our bundles: io/opentelemetry/osgi/<artifact>/<version>/<artifact>-<version>.jar
# OTel JARs:   io/opentelemetry/<artifact>/<version>/<artifact>-<version>.jar

# Copy our project bundle JARs
COPY --from=build /workspace/opentelemetry-osgi-runtime/target/opentelemetry-osgi-runtime-${PROJECT_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/osgi/opentelemetry-osgi-runtime/${PROJECT_VERSION}/opentelemetry-osgi-runtime-${PROJECT_VERSION}.jar
COPY --from=build /workspace/opentelemetry-osgi-core/target/opentelemetry-osgi-core-${PROJECT_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/osgi/opentelemetry-osgi-core/${PROJECT_VERSION}/opentelemetry-osgi-core-${PROJECT_VERSION}.jar
COPY --from=build /workspace/opentelemetry-osgi-client/target/opentelemetry-osgi-client-${PROJECT_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/osgi/opentelemetry-osgi-client/${PROJECT_VERSION}/opentelemetry-osgi-client-${PROJECT_VERSION}.jar
COPY --from=build /workspace/opentelemetry-osgi-scr/target/opentelemetry-osgi-scr-${PROJECT_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/osgi/opentelemetry-osgi-scr/${PROJECT_VERSION}/opentelemetry-osgi-scr-${PROJECT_VERSION}.jar
COPY --from=build /workspace/opentelemetry-osgi-log/target/opentelemetry-osgi-log-${PROJECT_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/osgi/opentelemetry-osgi-log/${PROJECT_VERSION}/opentelemetry-osgi-log-${PROJECT_VERSION}.jar

# Copy the feature descriptor
COPY --from=build /workspace/opentelemetry-osgi-karaf-feature/target/feature/feature.xml \
    ${KARAF_HOME}/system/io/opentelemetry/osgi/opentelemetry-osgi-karaf-feature/${PROJECT_VERSION}/opentelemetry-osgi-karaf-feature-${PROJECT_VERSION}-features.xml

# Copy OpenTelemetry SDK JARs (needed by wrap: protocol at feature install time)
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-api/${OTEL_VERSION}/opentelemetry-api-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-api/${OTEL_VERSION}/opentelemetry-api-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-context/${OTEL_VERSION}/opentelemetry-context-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-context/${OTEL_VERSION}/opentelemetry-context-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-sdk/${OTEL_VERSION}/opentelemetry-sdk-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-sdk/${OTEL_VERSION}/opentelemetry-sdk-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-sdk-common/${OTEL_VERSION}/opentelemetry-sdk-common-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-sdk-common/${OTEL_VERSION}/opentelemetry-sdk-common-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-sdk-trace/${OTEL_VERSION}/opentelemetry-sdk-trace-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-sdk-trace/${OTEL_VERSION}/opentelemetry-sdk-trace-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-sdk-metrics/${OTEL_VERSION}/opentelemetry-sdk-metrics-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-sdk-metrics/${OTEL_VERSION}/opentelemetry-sdk-metrics-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-sdk-logs/${OTEL_VERSION}/opentelemetry-sdk-logs-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-sdk-logs/${OTEL_VERSION}/opentelemetry-sdk-logs-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-exporter-common/${OTEL_VERSION}/opentelemetry-exporter-common-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-exporter-common/${OTEL_VERSION}/opentelemetry-exporter-common-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-exporter-logging/${OTEL_VERSION}/opentelemetry-exporter-logging-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-exporter-logging/${OTEL_VERSION}/opentelemetry-exporter-logging-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-exporter-otlp/${OTEL_VERSION}/opentelemetry-exporter-otlp-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-exporter-otlp/${OTEL_VERSION}/opentelemetry-exporter-otlp-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-exporter-otlp-common/${OTEL_VERSION}/opentelemetry-exporter-otlp-common-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-exporter-otlp-common/${OTEL_VERSION}/opentelemetry-exporter-otlp-common-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-exporter-sender-jdk/${OTEL_VERSION}/opentelemetry-exporter-sender-jdk-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-exporter-sender-jdk/${OTEL_VERSION}/opentelemetry-exporter-sender-jdk-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-sdk-extension-autoconfigure-spi/${OTEL_VERSION}/opentelemetry-sdk-extension-autoconfigure-spi-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-sdk-extension-autoconfigure-spi/${OTEL_VERSION}/opentelemetry-sdk-extension-autoconfigure-spi-${OTEL_VERSION}.jar
COPY --from=build /root/.m2/repository/io/opentelemetry/opentelemetry-sdk-extension-autoconfigure/${OTEL_VERSION}/opentelemetry-sdk-extension-autoconfigure-${OTEL_VERSION}.jar \
    ${KARAF_HOME}/system/io/opentelemetry/opentelemetry-sdk-extension-autoconfigure/${OTEL_VERSION}/opentelemetry-sdk-extension-autoconfigure-${OTEL_VERSION}.jar

# ---- Configure Karaf to install our feature on boot ----

# Add our feature repository and add demo feature to boot list
RUN sed -i \
    '/^featuresRepositories/s|$|,mvn:io.opentelemetry.osgi/opentelemetry-osgi-karaf-feature/'${PROJECT_VERSION}'/xml/features|' \
    ${KARAF_HOME}/etc/org.apache.karaf.features.cfg && \
    sed -i \
    '/^featuresBoot/s|$|,opentelemetry-osgi-demo|' \
    ${KARAF_HOME}/etc/org.apache.karaf.features.cfg

# Karaf's Pax URL should only use the local system repo (no network in runtime)
RUN sed -i 's|^org.ops4j.pax.url.mvn.repositories=|#org.ops4j.pax.url.mvn.repositories=|' \
    ${KARAF_HOME}/etc/org.ops4j.pax.url.mvn.cfg

# OTLP configuration is injected via environment variables at runtime.
# The feature includes a default config with exporterType=logging.
# When OTEL_EXPORTER_OTLP_ENDPOINT is set, the runtime auto-switches to OTLP.

EXPOSE 8101 1099 44444 8181

CMD ["karaf", "run"]
