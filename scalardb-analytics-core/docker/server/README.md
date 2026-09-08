# ScalarDB Analytics Server Docker Image

Docker image for the ScalarDB Analytics catalog server.

## Image

`ghcr.io/scalar-labs/scalardb-analytics-server`, built from the `server:app` distribution.

## Build Image

### Prerequisites

Building the Docker image requires authentication to GitHub Packages to download private dependencies.

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

#### Creating a GitHub Personal Access Token

If you don't have a token yet:
1. Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Select the `read:packages` scope
4. Generate and copy the token

### Using Makefile (Recommended)

From the `docker/server` directory:

```bash
# Build image with 'latest' tag
make build

# Tag the latest image with git version (e.g., v3.16.0)
make tag

# Push images to GitHub Packages
make push

# Show current version information
make version

# Complete release workflow (build, tag, and push to GitHub Packages)
make release

# Show all available targets
make help
```

The Makefile automatically:

- Builds the server distribution before creating the Docker image
- Uses `git describe` to determine the version tag
- Publishes to `ghcr.io/scalar-labs/scalardb-analytics-server`

### Using Docker directly

From the repository root:

```bash
# Credentials are passed securely using Docker BuildKit secrets
docker build -f docker/server/Dockerfile \
  --secret id=gpr_username,env=GPR_USERNAME \
  --secret id=gpr_password,env=GPR_PASSWORD \
  -t scalardb-analytics-server:latest .
```

**Note:** This uses Docker BuildKit secrets to avoid exposing credentials in image metadata.

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
