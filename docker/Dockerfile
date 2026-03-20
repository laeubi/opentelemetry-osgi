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

# Copy POM files for dependency caching
COPY pom.xml .
COPY core/pom.xml core/
COPY core/opentelemetry-osgi-runtime/pom.xml core/opentelemetry-osgi-runtime/
COPY integrations/pom.xml integrations/
COPY integrations/opentelemetry-osgi-framework/pom.xml integrations/opentelemetry-osgi-framework/
COPY integrations/opentelemetry-osgi-scr/pom.xml integrations/opentelemetry-osgi-scr/
COPY integrations/opentelemetry-osgi-log/pom.xml integrations/opentelemetry-osgi-log/
COPY demo/pom.xml demo/
COPY demo/opentelemetry-osgi-demo/pom.xml demo/opentelemetry-osgi-demo/
COPY features/pom.xml features/
COPY features/opentelemetry-osgi-karaf-feature/pom.xml features/opentelemetry-osgi-karaf-feature/
COPY features/opentelemetry-osgi-integration-karaf-feature/pom.xml features/opentelemetry-osgi-integration-karaf-feature/
COPY features/opentelemetry-osgi-demo-karaf-feature/pom.xml features/opentelemetry-osgi-demo-karaf-feature/
COPY incubator/pom.xml incubator/
COPY incubator/opentelemetry-osgi-agent/pom.xml incubator/opentelemetry-osgi-agent/
RUN mvn dependency:go-offline -q -B 2>/dev/null || true

# Build the project
COPY . .
RUN mvn clean install -DskipTests -q -B

# Download SPI Fly and ASM JARs (Karaf runtime dependencies, not in our POM)
RUN mvn dependency:copy -q -B \
    -Dartifact=org.apache.aries.spifly:org.apache.aries.spifly.dynamic.bundle:1.3.7:jar \
    -DoutputDirectory=/tmp/deps && \
    mvn dependency:copy -q -B \
    -Dartifact=org.ow2.asm:asm:9.7.1:jar \
    -DoutputDirectory=/tmp/deps && \
    mvn dependency:copy -q -B \
    -Dartifact=org.ow2.asm:asm-util:9.7.1:jar \
    -DoutputDirectory=/tmp/deps && \
    mvn dependency:copy -q -B \
    -Dartifact=org.ow2.asm:asm-tree:9.7.1:jar \
    -DoutputDirectory=/tmp/deps && \
    mvn dependency:copy -q -B \
    -Dartifact=org.ow2.asm:asm-analysis:9.7.1:jar \
    -DoutputDirectory=/tmp/deps && \
    mvn dependency:copy -q -B \
    -Dartifact=org.ow2.asm:asm-commons:9.7.1:jar \
    -DoutputDirectory=/tmp/deps

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

# New groupId path: org/eclipse/osgi-technology/incubator
# Our bundles: org/eclipse/osgi-technology/incubator/<artifact>/<version>/<artifact>-<version>.jar
# OTel JARs:   io/opentelemetry/<artifact>/<version>/<artifact>-<version>.jar

# Copy our project bundle JARs
COPY --from=build /workspace/core/opentelemetry-osgi-runtime/target/opentelemetry-osgi-runtime-${PROJECT_VERSION}.jar \
    ${KARAF_HOME}/system/org/eclipse/osgi-technology/incubator/opentelemetry-osgi-runtime/${PROJECT_VERSION}/opentelemetry-osgi-runtime-${PROJECT_VERSION}.jar
COPY --from=build /workspace/integrations/opentelemetry-osgi-framework/target/opentelemetry-osgi-framework-${PROJECT_VERSION}.jar \
    ${KARAF_HOME}/system/org/eclipse/osgi-technology/incubator/opentelemetry-osgi-framework/${PROJECT_VERSION}/opentelemetry-osgi-framework-${PROJECT_VERSION}.jar
COPY --from=build /workspace/integrations/opentelemetry-osgi-scr/target/opentelemetry-osgi-scr-${PROJECT_VERSION}.jar \
    ${KARAF_HOME}/system/org/eclipse/osgi-technology/incubator/opentelemetry-osgi-scr/${PROJECT_VERSION}/opentelemetry-osgi-scr-${PROJECT_VERSION}.jar
COPY --from=build /workspace/integrations/opentelemetry-osgi-log/target/opentelemetry-osgi-log-${PROJECT_VERSION}.jar \
    ${KARAF_HOME}/system/org/eclipse/osgi-technology/incubator/opentelemetry-osgi-log/${PROJECT_VERSION}/opentelemetry-osgi-log-${PROJECT_VERSION}.jar
COPY --from=build /workspace/demo/opentelemetry-osgi-demo/target/opentelemetry-osgi-demo-${PROJECT_VERSION}.jar \
    ${KARAF_HOME}/system/org/eclipse/osgi-technology/incubator/opentelemetry-osgi-demo/${PROJECT_VERSION}/opentelemetry-osgi-demo-${PROJECT_VERSION}.jar

# Copy all three feature descriptors
COPY --from=build /workspace/features/opentelemetry-osgi-karaf-feature/target/feature/feature.xml \
    ${KARAF_HOME}/system/org/eclipse/osgi-technology/incubator/opentelemetry-osgi-karaf-feature/${PROJECT_VERSION}/opentelemetry-osgi-karaf-feature-${PROJECT_VERSION}-features.xml
COPY --from=build /workspace/features/opentelemetry-osgi-integration-karaf-feature/target/feature/feature.xml \
    ${KARAF_HOME}/system/org/eclipse/osgi-technology/incubator/opentelemetry-osgi-integration-karaf-feature/${PROJECT_VERSION}/opentelemetry-osgi-integration-karaf-feature-${PROJECT_VERSION}-features.xml
COPY --from=build /workspace/features/opentelemetry-osgi-demo-karaf-feature/target/feature/feature.xml \
    ${KARAF_HOME}/system/org/eclipse/osgi-technology/incubator/opentelemetry-osgi-demo-karaf-feature/${PROJECT_VERSION}/opentelemetry-osgi-demo-karaf-feature-${PROJECT_VERSION}-features.xml

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

# Copy SPI Fly and ASM dependencies (required by the spifly feature, not bundled in Karaf)
ARG SPIFLY_VERSION=1.3.7
ARG ASM_VERSION=9.7.1

COPY --from=build /root/.m2/repository/org/apache/aries/spifly/org.apache.aries.spifly.dynamic.bundle/${SPIFLY_VERSION}/org.apache.aries.spifly.dynamic.bundle-${SPIFLY_VERSION}.jar \
    ${KARAF_HOME}/system/org/apache/aries/spifly/org.apache.aries.spifly.dynamic.bundle/${SPIFLY_VERSION}/org.apache.aries.spifly.dynamic.bundle-${SPIFLY_VERSION}.jar
COPY --from=build /root/.m2/repository/org/ow2/asm/asm/${ASM_VERSION}/asm-${ASM_VERSION}.jar \
    ${KARAF_HOME}/system/org/ow2/asm/asm/${ASM_VERSION}/asm-${ASM_VERSION}.jar
COPY --from=build /root/.m2/repository/org/ow2/asm/asm-util/${ASM_VERSION}/asm-util-${ASM_VERSION}.jar \
    ${KARAF_HOME}/system/org/ow2/asm/asm-util/${ASM_VERSION}/asm-util-${ASM_VERSION}.jar
COPY --from=build /root/.m2/repository/org/ow2/asm/asm-tree/${ASM_VERSION}/asm-tree-${ASM_VERSION}.jar \
    ${KARAF_HOME}/system/org/ow2/asm/asm-tree/${ASM_VERSION}/asm-tree-${ASM_VERSION}.jar
COPY --from=build /root/.m2/repository/org/ow2/asm/asm-analysis/${ASM_VERSION}/asm-analysis-${ASM_VERSION}.jar \
    ${KARAF_HOME}/system/org/ow2/asm/asm-analysis/${ASM_VERSION}/asm-analysis-${ASM_VERSION}.jar
COPY --from=build /root/.m2/repository/org/ow2/asm/asm-commons/${ASM_VERSION}/asm-commons-${ASM_VERSION}.jar \
    ${KARAF_HOME}/system/org/ow2/asm/asm-commons/${ASM_VERSION}/asm-commons-${ASM_VERSION}.jar

# ---- Configure Karaf to install our features on boot ----

# Add all three feature repositories and add demo feature to boot list
RUN sed -i \
    '/^featuresRepositories/s|$|,mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-karaf-feature/'${PROJECT_VERSION}'/xml/features,mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-integration-karaf-feature/'${PROJECT_VERSION}'/xml/features,mvn:org.eclipse.osgi-technology.incubator/opentelemetry-osgi-demo-karaf-feature/'${PROJECT_VERSION}'/xml/features|' \
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
