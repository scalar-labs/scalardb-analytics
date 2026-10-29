# ScalarDB Analytics CLI Docker Image

This directory contains the Dockerfile and Makefile for building the ScalarDB Analytics CLI Docker image.

## Building the Image

### Build Commands

```bash
# Build the Docker image with 'latest' tag
make build

# Tag the image with git version
make tag

# Push to GitHub Packages (requires authentication)
make push

# Complete release workflow (build, tag, push)
make release

# Show version information
make version

# Clean up local images
make clean
```

## Running the CLI Container

### Basic Usage

```bash
# Show help
docker run --rm ghcr.io/scalar-labs/scalardb-analytics-cli:latest

# Run with a command (will fail without configuration)
docker run --rm ghcr.io/scalar-labs/scalardb-analytics-cli:latest catalog list
```

### With Configuration File

Create a `client.properties` file:

```properties
scalar.db.analytics.client.server.host=your-server-host
scalar.db.analytics.client.server.catalog.port=11051
```

Mount it when running:

```bash
# Using absolute path
docker run --rm \
  -v /path/to/client.properties:/config/client.properties:ro \
  ghcr.io/scalar-labs/scalardb-analytics-cli:latest \
  -c /config/client.properties catalog list

# Using current directory
docker run --rm \
  -v $(pwd)/client.properties:/config/client.properties:ro \
  ghcr.io/scalar-labs/scalardb-analytics-cli:latest \
  -c /config/client.properties catalog list
```

### Debugging and Troubleshooting

If you need to inspect the container or run multiple commands manually:

```bash
# Start a bash shell in the container
docker run --rm -it \
  -v $(pwd)/client.properties:/config/client.properties:ro \
  --entrypoint /bin/bash \
  ghcr.io/scalar-labs/scalardb-analytics-cli:latest

# Inside the container, you can run the CLI manually:
java -jar /scalardb-analytics-cli/scalardb-analytics-cli.jar -c /config/client.properties catalog list
```

Note: The CLI itself does not have an interactive mode. Each invocation runs a single command and exits.

## Image Details

- Base image: `eclipse-temurin:21.0.12.1_1-jre-resolute` (Ubuntu 26.04), pinned by digest in the Dockerfile
- User: `scalardb` (UID/GID 201) with a nologin shell
- Working directory: `/scalardb-analytics-cli`
- JAR location: `/scalardb-analytics-cli/scalardb-analytics-cli.jar`

## Security

- Runs as non-root user (scalardb)
- Minimal JRE-only base image
- Regular security updates via base image
- The application files under `/scalardb-analytics-cli` are owned by root, so the runtime user
  cannot modify them. The CLI reads its configuration from the path given with `-c`,
  `SCALAR_DB_ANALYTICS_CONFIG_PATH`, or the XDG config directory, and writes nothing to its
  working directory

## Temporary mitigation of the emSign CA compromise

As a temporary fix for the [eMudhra CA compromise](https://emudhra.we-have-a-problem.com/)
(CVE-2026-91403 and CVE-2026-114820), the image deletes the emSign root CAs from the Java
`cacerts` at build time. It also ships Oracle's `CheckCerts` in the working directory, so you can
confirm that the actual runtime does not trust the compromised CA certificates:

```console
$ docker run -it --rm --entrypoint bash ghcr.io/scalar-labs/scalardb-analytics-cli:latest
scalardb@7c3a9e1b5d02:/scalardb-analytics-cli$ java CheckCerts
Java runtime version 21.0.12.1+1-LTS at /opt/java/openjdk
No bad root certificates found
```

The mitigation, `CheckCerts`, and the `tmp-fix-ca-compromise/` directory go away once the image
moves to a Java release that no longer ships those roots.
