# Multi-stage build for GraalVM native image
FROM ghcr.io/graalvm/graalvm-community:25.0.2 AS builder

WORKDIR /build

# Copy Maven wrapper and pom.xml
COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code and build native image.
# NOTE: the micronaut-maven-plugin (added to pom.xml) is what makes
# -Dpackaging=native-image work; there is no Maven profile named "native"
# to activate, so the old "-Pnative" flag was a no-op/error.
COPY src src
RUN ./mvnw package -Dmicronaut.aot.enabled=true -Dpackaging=native-image

# Final stage
FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    zlib1g \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the native executable
COPY --from=builder /build/target/micronaut-demo /app/micronaut-demo

# Expose application port
EXPOSE 8080

# Run the native image
ENTRYPOINT ["/app/micronaut-demo", "-Dorg.jboss.logging.provider=slf4j"]