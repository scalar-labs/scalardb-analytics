# ScalarDB Analytics Server Docker Image

Docker image for the ScalarDB Analytics catalog server.

## Image

`ghcr.io/scalar-labs/scalardb-analytics-server`, built from the `server:app` distribution.

## Build Image

### Credentials for ScalarDB SQL (temporary)

> [!NOTE]
> Until ScalarDB SQL is published under an open license, building this image needs credentials for a private GitHub Packages registry that only Scalar can access.
> This section goes away when that constraint does; see [#17](https://github.com/scalar-labs/scalardb-analytics/issues/17).

Provide a GitHub user and a token with the `read:packages` scope that can read the `scalar-labs/scalardb-sql` packages, in one of the following ways.

#### Option 1: Using `~/.gradle/gradle.properties` (Recommended for local development)

If you already have Gradle credentials configured, the Makefile will automatically use them:

```properties
# ~/.gradle/gradle.properties
gprUsername=your-github-username
gprPassword=your-github-token
```

#### Option 2: Using environment variables

Set the following environment variables:

```bash
export GPR_USERNAME=your-github-username
export GPR_PASSWORD=your-github-token
```

**Note:** Environment variables take precedence over `gradle.properties` if both are set.

To create the token, go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic), and generate a token with the `read:packages` scope.

Both build methods below pass the credentials to Gradle as Docker BuildKit secrets, so they are not stored in the image metadata.

### Using Makefile (Recommended)

From the `docker/server` directory:

```bash
# Build image with 'latest' tag
make build

# Tag the latest image with git version (e.g., v3.16.0)
make tag

# Push images to GitHub Container Registry (maintainers only)
make push

# Show current version information
make version

# Build, tag, and push in one step (maintainers only)
make release

# Show all available targets
make help
```

The Makefile automatically:

- Builds the server distribution before creating the Docker image
- Uses `git describe` to determine the version tag
- Tags the image as `ghcr.io/scalar-labs/scalardb-analytics-server`

`make push` and `make release` need write access to the `ghcr.io/scalar-labs/scalardb-analytics-server` package.
Release images are pushed by the `release-to-ghcr` job of [`.github/workflows/release.yaml`](../../../.github/workflows/release.yaml), so you do not normally run them by hand.

### Using Docker directly

From the `scalardb-analytics-core` directory:

```bash
docker build -f docker/server/Dockerfile \
  --secret id=gpr_username,env=GPR_USERNAME \
  --secret id=gpr_password,env=GPR_PASSWORD \
  -t scalardb-analytics-server:latest .
```

The `--secret` options pass the credentials described in [Credentials for ScalarDB SQL (temporary)](#credentials-for-scalardb-sql-temporary).

## Usage

### Basic Usage

```bash
docker run -p 11051:11051 scalardb-analytics-server:latest
```

### Custom Port

```bash
docker run -p 9090:9090 -e SCALAR_DB_ANALYTICS_SERVER_CATALOG_PORT=9090 scalardb-analytics-server:latest
```

### Background Mode

```bash
docker run -d -p 11051:11051 --name analytics-server scalardb-analytics-server:latest
```

### Show Help

```bash
docker run --rm scalardb-analytics-server:latest --help
docker run --rm scalardb-analytics-server:latest start --help
```

## Configuration

The server runs as a non-root user (`scalardb`) and exposes port 11051 by default. Additional configuration can be passed through command-line arguments or environment variables as supported by the server application.

The application files under `/scalardb-analytics-server` are owned by root, so the runtime user
cannot modify them. Anything the server has to write must point at a mounted volume.
