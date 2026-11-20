# Build stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:25.0.5_8-jdk-resolute@sha256:1ba05eab8e59bad9cbc9c6294b5c4a1d71150cfd00a5cdf04e67bb36a0a10916 AS builder

WORKDIR /workspace

# Copy all source files
COPY . .

# Build the CLI fat JAR
RUN ./gradlew :client:cli:shadowJar --no-daemon

# Runtime stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:25.0.5_8-jre-resolute@sha256:480ef64e4540d45b0301e8afb7873bd3fa61ce93c5f66273eb569420718f38e2

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
