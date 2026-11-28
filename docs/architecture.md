# Architecture

ScalarDB Analytics runs analytical queries across heterogeneous databases. Two components divide
that work, and they meet only through the catalog's gRPC API:

- The **catalog server** (`scalardb-analytics-core`) owns metadata and access control. It records
  which data sources exist, how to connect to them, and which namespaces, tables, and columns they
  expose, and it decides who may see them.
- The **Spark connector** (`scalardb-analytics-spark`) runs inside Apache Spark. It reads metadata
  from the catalog server through the Java client SDK and reads the data itself directly from the
  registered data sources.

The catalog server never reads table data, and the Spark connector never writes catalog metadata.
For the product-level design, see the
[official design document](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/design/).

## Core concepts

- **Catalog**: the top-level container that users create and that the Spark connector exposes as
  a Spark catalog.
- **Data source**: one external database registered in a catalog, described by a provider payload
  (see [Data sources](data-sources.md)).
- **Namespace** and **table**: the structure discovered from, or declared for, a data source when
  it is registered. Columns carry ScalarDB Analytics data types rather than source-specific types.
- **User**, **role**, and **permission**: the principals and grants that control access to these
  resources (see [Authentication](authentication.md) and [Authorization](authorization.md)).

These resources form one hierarchy, which both the metadata model and authorization follow:

```text
Catalog
  └── Data source
        └── Namespace
              └── Table
                    └── Column
```

## Catalog server structure

The catalog server is a layered Spring Boot application. Each layer depends only on the layers
below it, and infrastructure is reached through interfaces that inner layers define:

```text
server/base   gRPC services, interceptors, configuration, dependency injection
     │
usecase-impl  one use case per operation: authorization, transactions, orchestration
     │        (implements the interfaces in usecase)
service       domain services such as authorization
     │
repository    data-access interfaces (implemented by repository-impl-spring-data-jdbc)
     │
domain        server-internal domain objects
     │
api           models, error codes, and codecs shared with clients
```

- `api` and `lib` are shared with clients. They target Java 8 and must not depend on server
  modules, gRPC, or Spring.
- `grpc` holds the Protocol Buffers contract and the MapStruct mappers between protobuf messages
  and `api` models. Both the server and the client SDK depend on it.
- `schema-resolver` and `datasource-scalardb` discover the structure of a data source during
  registration.
- `repository-impl-spring-data-jdbc` stores catalog metadata, whose tables are defined for
  ScalarDB. ScalarDB has no foreign keys or cascading deletes, so use cases enforce referential
  integrity themselves.

Use cases own authorization: each operation checks the caller's permissions before it touches
metadata, and list operations filter their results the same way. The gRPC layer only translates
messages and errors.

## Clients

The **Java client SDK** (`client-sdk`) is the only supported way to call the catalog server. It
wraps every gRPC service in a blocking client, manages the channel, TLS, authentication headers,
and retries, and converts every failure into an `AnalyticsException`. The **CLI**
(`client/cli`) and the Spark connector are both built on the SDK, so the SDK and its dependencies
(`api`, `lib`, `grpc`, `grpc-common`) target Java 8.

The SDK retries transient gRPC failures (`UNAVAILABLE`, `DEADLINE_EXCEEDED`,
`RESOURCE_EXHAUSTED`) by default, so any RPC may reach the server more than once for one client
call. **Every RPC must therefore be idempotent**: receiving the same request again must produce the
same result.

## Spark connector

The Spark connector exposes each catalog as a Spark catalog. When Spark plans a query, the
connector resolves catalogs, namespaces, and tables through the SDK and builds a reader for the
data source's provider. ScalarDB and DynamoDB have their own readers in the connector's
`datasource/` modules; relational databases are read through Spark's JDBC data source. The connector cross-builds for
several Spark and Scala versions; see the [Spark module README](../scalardb-analytics-spark/README.md).

## Representative flow: register a data source

1. The CLI reads the provider payload, expands `${file:...}` references, and sends it with the
   catalog and data source names (see [Data sources](data-sources.md)).
2. The SDK attaches the access token and calls `DataSourceService.Register`.
3. The authentication interceptor validates the token
   (see [Authentication](authentication.md)).
4. The use case checks that the caller holds `CATALOG_WRITE` or `CATALOG_ADMIN` on the catalog
   (see [Authorization](authorization.md)).
5. The data source service decodes the payload, connects to the source to discover its namespaces,
   tables, and columns (or uses the supplied schema), and stores everything in one transaction.
6. Any failure surfaces as an `AnalyticsException` that the server encodes into the gRPC status
   and the SDK decodes again (see [Error handling](error-handling.md)).

## Architectural units

| Unit | Explains |
| --- | --- |
| [Data sources](data-sources.md) | Provider payloads, their codecs, and how registration discovers structure |
| [Authentication](authentication.md) | Users, identities, password backends, and access tokens |
| [Authorization](authorization.md) | Permissions, roles, hierarchy traversal, and ScalarDB Cluster delegation |
| [Error handling](error-handling.md) | The single error code taxonomy and how errors cross the gRPC boundary |

Design decisions that shape these units are recorded under [`design/`](design/).
