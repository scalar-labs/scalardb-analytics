# Build stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:25.0.5_8-jdk-resolute@sha256:422bf3f42dc7f967f36eb4196f6d7b683c415cb541583571833d865c807c2ea2 AS builder

WORKDIR /workspace

# Copy all source files
COPY . .

# Build the CLI fat JAR
RUN ./gradlew :client:cli:shadowJar --no-daemon

# Runtime stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:25.0.5_8-jre-resolute@sha256:b6e00330337143a2c1a20f1c1c693d4b6ce9a3c30ade8b9d03fc68cc9e3871e7

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
