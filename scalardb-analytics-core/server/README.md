# ScalarDB Analytics Server

The ScalarDB Analytics server provides the gRPC catalog service used to manage catalogs, data sources, namespaces, and tables.

## Modules

### `server/base`

Shared server implementation, including the gRPC service and dependency-injection configuration.

### `server/app`

The server distribution, packaged as an installable archive.

## Build

Use JDK 21 and the Gradle wrapper in [`scalardb-analytics-core/`](../README.md):

```bash
./gradlew :server:app:build
```

For building the server Docker image, see [docker/server/README.md](../docker/server/README.md).

## Run

From an installed distribution directory, start the server with:

```bash
bin/scalardb-analytics-server start
```

The catalog service listens on port `11051` by default.
For configuration and usage, see the [official documentation](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/configurations).

## License

ScalarDB Analytics Server is dual-licensed under both the Apache 2.0 License
(found in the [LICENSE](../../LICENSE) file) and a commercial license. You may
select, at your option, one of the licenses. For more information about the
commercial license, please [contact us](https://www.scalar-labs.com/contact).
