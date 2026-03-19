# ==============================================================================
# Multi-stage Dockerfile for the OpenTelemetry OSGi Demo
#
# Stage 1: Maven build
# Stage 2: Apache Felix OSGi runtime with our bundles + OTel on system classpath
# ==============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build the Maven project
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Cache dependencies first (improves rebuild speed)
COPY pom.xml .
COPY opentelemetry-osgi-runtime/pom.xml opentelemetry-osgi-runtime/
COPY opentelemetry-osgi-client/pom.xml opentelemetry-osgi-client/
COPY opentelemetry-osgi-scr/pom.xml opentelemetry-osgi-scr/
COPY opentelemetry-osgi-log/pom.xml opentelemetry-osgi-log/
COPY opentelemetry-osgi-agent/pom.xml opentelemetry-osgi-agent/
RUN mvn dependency:go-offline -q -B 2>/dev/null || true

# Build the project
COPY . .
RUN mvn clean package -DskipTests -q -B

# Copy all runtime dependencies to a flat lib directory
RUN mvn dependency:copy-dependencies -pl opentelemetry-osgi-runtime \
    -DoutputDirectory=/workspace/lib \
    -DincludeScope=runtime \
    -DexcludeGroupIds=org.osgi \
    -q -B

# Also get Felix framework, SCR, and ConfigAdmin
RUN mvn dependency:copy -q -B \
    -Dartifact=org.apache.felix:org.apache.felix.framework:7.0.5 \
    -DoutputDirectory=/workspace/felix && \
    mvn dependency:copy -q -B \
    -Dartifact=org.apache.felix:org.apache.felix.scr:2.2.14 \
    -DoutputDirectory=/workspace/osgi-bundles && \
    mvn dependency:copy -q -B \
    -Dartifact=org.apache.felix:org.apache.felix.configadmin:1.9.22 \
    -DoutputDirectory=/workspace/osgi-bundles && \
    mvn dependency:copy -q -B \
    -Dartifact=org.apache.felix:org.apache.felix.log:1.3.0 \
    -DoutputDirectory=/workspace/osgi-bundles

# ---------------------------------------------------------------------------
# Stage 2: Runtime image with Apache Felix
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Felix framework
COPY --from=build /workspace/felix/ /app/

# OpenTelemetry and other libs on the system classpath
COPY --from=build /workspace/lib/ /app/lib/

# OSGi infrastructure bundles (SCR, ConfigAdmin)
COPY --from=build /workspace/osgi-bundles/ /app/bundle/

# Our bundles to auto-deploy
COPY --from=build /workspace/opentelemetry-osgi-runtime/target/opentelemetry-osgi-runtime-*.jar /app/bundle/
COPY --from=build /workspace/opentelemetry-osgi-client/target/opentelemetry-osgi-client-*.jar /app/bundle/
COPY --from=build /workspace/opentelemetry-osgi-scr/target/opentelemetry-osgi-scr-*.jar /app/bundle/
COPY --from=build /workspace/opentelemetry-osgi-log/target/opentelemetry-osgi-log-*.jar /app/bundle/

# Felix configuration
COPY docker/felix-config.properties /app/conf/config.properties

# Start Felix with OTel libs on the classpath
# The glob for lib/*.jar is expanded by the shell
CMD ["sh", "-c", "java -cp '/app/lib/*:/app/org.apache.felix.framework-7.0.5.jar' org.apache.felix.main.Main"]
