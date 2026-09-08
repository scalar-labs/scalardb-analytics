FROM ghcr.io/grpc-ecosystem/grpc-health-probe:v0.4.42 AS grpc-health-probe

FROM docker.io/busybox:1.36 AS tools

# Copy grpc_health_probe from official image
COPY --from=grpc-health-probe /ko-app/grpc-health-probe /grpc_health_probe

# Build stage
FROM eclipse-temurin:21-jdk-noble AS builder

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
FROM eclipse-temurin:21-jdk-noble

LABEL org.opencontainers.image.licenses="Apache-2.0"

RUN apt-get update && apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# netcat lets testcontainers check the ports this image exposes
RUN apt-get update && apt-get install -y --no-install-recommends netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

COPY --from=tools grpc_health_probe /usr/local/bin/

WORKDIR /scalardb-analytics-server

RUN groupadd -r --gid 201 scalardb && \
    useradd -r --uid 201 -g scalardb scalardb

COPY --from=builder /workspace/server/app/build/install/scalardb-analytics-server/ /scalardb-analytics-server/

RUN chown -R scalardb:scalardb /scalardb-analytics-server

USER 201

EXPOSE 11051

ENTRYPOINT ["/scalardb-analytics-server/bin/scalardb-analytics-server"]

CMD ["start", "--config", "/scalardb-analytics-server/server.properties"]
