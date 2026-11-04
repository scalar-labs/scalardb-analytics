# Build stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:21.0.13_9-jdk-resolute@sha256:7d760d0aecb9f6dc319583802bdb32ed55234e6c79a0e8ae56077ec4da382f22 AS builder

WORKDIR /workspace

# Copy all source files
COPY . .

# Build the CLI fat JAR
RUN ./gradlew :client:cli:shadowJar --no-daemon

# Runtime stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:21.0.13_9-jre-resolute@sha256:f8622d0220c9e218d7a33f24a7b9e1ad6bc726085f8b0ae74e1adf85f67f05c4

LABEL org.opencontainers.image.licenses="Apache-2.0"

RUN export DEBIAN_FRONTEND=noninteractive \
 && apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r --gid 201 scalardb \
 && useradd -r --uid 201 -g scalardb -s /usr/sbin/nologin scalardb

WORKDIR /scalardb-analytics-cli

# Copy the fat JAR from builder
# The application files stay owned by root so that the runtime user cannot modify them.
COPY --from=builder /workspace/client/cli/build/libs/scalardb-analytics-cli-*-all.jar /scalardb-analytics-cli/scalardb-analytics-cli.jar

USER 201:201

ENTRYPOINT ["java", "-jar", "/scalardb-analytics-cli/scalardb-analytics-cli.jar"]

CMD ["--help"]
