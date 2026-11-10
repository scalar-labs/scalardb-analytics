# grpc_health_probe for Kubernetes:
# - https://kubernetes.io/blog/2018/10/01/health-checking-grpc-servers-on-kubernetes/
# The official image is a multi-platform index, so it serves both platforms that the Makefile
# builds for. You can confirm the digest with:
#   docker buildx imagetools inspect ghcr.io/grpc-ecosystem/grpc-health-probe:<tag>
FROM ghcr.io/grpc-ecosystem/grpc-health-probe:v0.4.60@sha256:2d433b978644e40eb2b2887dbd0af3a19e470514a2176a97cad1971f13bdb171 AS grpc-health-probe

# Build stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:21.0.13_9-jdk-resolute@sha256:7d760d0aecb9f6dc319583802bdb32ed55234e6c79a0e8ae56077ec4da382f22 AS builder

WORKDIR /workspace

# Copy all source files
COPY . .

# Build the server distribution
# Mount secrets to avoid exposing credentials in image metadata
RUN --mount=type=secret,id=gpr_username \
    --mount=type=secret,id=gpr_password \
    GPR_USERNAME=$(cat /run/secrets/gpr_username 2>/dev/null || echo "") \
    GPR_PASSWORD=$(cat /run/secrets/gpr_password 2>/dev/null || echo "") \
    ./gradlew :server:app:installDist --no-daemon

# Runtime stage
# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:21.0.13_9-jdk-resolute@sha256:7d760d0aecb9f6dc319583802bdb32ed55234e6c79a0e8ae56077ec4da382f22

LABEL org.opencontainers.image.licenses="Apache-2.0"

# netcat lets testcontainers check the ports this image exposes
RUN export DEBIAN_FRONTEND=noninteractive \
 && apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && apt-get install -y --no-install-recommends netcat-openbsd \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r --gid 201 scalardb \
 && useradd -r --uid 201 -g scalardb -s /usr/sbin/nologin scalardb

COPY --from=grpc-health-probe /ko-app/grpc-health-probe /usr/local/bin/grpc_health_probe

WORKDIR /scalardb-analytics-server

# The application files stay owned by root so that the runtime user cannot modify them.
COPY --from=builder /workspace/server/app/build/install/scalardb-analytics-server/ /scalardb-analytics-server/

USER 201:201

EXPOSE 11051

ENTRYPOINT ["/scalardb-analytics-server/bin/scalardb-analytics-server"]

CMD ["start", "--config", "/scalardb-analytics-server/server.properties"]
