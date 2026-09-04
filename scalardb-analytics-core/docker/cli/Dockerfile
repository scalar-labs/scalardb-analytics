# Build stage
FROM eclipse-temurin:21-jdk-noble AS builder

WORKDIR /workspace

# Copy all source files
COPY . .

# Build the CLI fat JAR
RUN ./gradlew :client:cli:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-noble

LABEL org.opencontainers.image.licenses="Apache-2.0"

RUN apt-get update && apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /scalardb-analytics-cli

RUN groupadd -r --gid 201 scalardb && \
    useradd -r --uid 201 -g scalardb scalardb

# Copy the fat JAR from builder
COPY --from=builder /workspace/client/cli/build/libs/scalardb-analytics-cli-*-all.jar /scalardb-analytics-cli/scalardb-analytics-cli.jar

RUN chown -R scalardb:scalardb /scalardb-analytics-cli

USER 201

ENTRYPOINT ["java", "-jar", "/scalardb-analytics-cli/scalardb-analytics-cli.jar"]

CMD ["--help"]
