# ScalarDB Analytics Core

ScalarDB Analytics Core contains the catalog server, Java client SDK, CLI, and shared APIs.
For product concepts and usage, see the [official documentation](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/design/).

## Build

Use JDK 21 and the included Gradle wrapper:

```bash
./gradlew check
```

Publish the same artifacts to Maven Local before building the Spark connector:

```bash
./gradlew publishToMavenLocal
```

## Architecture

The core modules follow dependency inversion: the server depends on use cases and repository interfaces, while infrastructure modules implement those interfaces.

```text
server/base (gRPC + DI) --> usecase-impl --> service --> repository
                                  |             |           |
                                  v             v           v
                               usecase       domain       repository-impl-*
                                  |             |
                                  +------+------+
                                         v
                                        api
```

## Modules

| Module | Description |
| --- | --- |
| `api/` | Models and types shared by the server and clients |
| `lib/` | Utilities and annotations shared across core modules |
| `domain/` | Server domain objects |
| `service/` | Domain services, including authorization logic |
| `usecase/` | Interfaces for application operations |
| `usecase-impl/` | Implementations that coordinate domain and repository operations |
| `repository/` | Data-access interfaces |
| `repository-impl-spring-data-jdbc/` | Spring Data JDBC repository implementation |
| [`grpc/`](grpc/README.md) | Protocol Buffers definitions and generated gRPC stubs |
| `grpc-common/` | Shared gRPC utilities |
| [`server/`](server/README.md) | Catalog server distributions |
| `client-sdk/` | Java client SDK |
| [`client/cli/`](client/cli/README.md) | Command-line client |
| `schema-resolver/` | Database schema discovery |
| `datasource-scalardb/` | ScalarDB data-source integration |

## Ports and configuration

- Catalog gRPC port: `11051` (`scalar.db.analytics.server.catalog.port`)

For authentication and authorization settings, see the [official guide](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/authentication-and-authorization).

## License

ScalarDB Analytics Core is dual-licensed under both the Apache 2.0 License
(found in the [LICENSE](../LICENSE) file) and a commercial license. You may
select, at your option, one of the licenses. For more information about the
commercial license, please [contact us](https://www.scalar-labs.com/contact).
