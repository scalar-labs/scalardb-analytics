# gRPC Module

This module holds the gRPC contract between the catalog server and its clients: the Protocol
Buffers definitions, the generated Java stubs, and the MapStruct mappers that convert between
protobuf messages and the models in `api`. Both `server/base` and `client-sdk` depend on it.

For how the contract fits into the architecture, including the rule that every RPC must be
idempotent, see the [developer documentation](../../docs/architecture.md).

## Layout

- `proto/`: Protocol Buffers definitions, one directory per service area (`auth`, `catalog`,
  `datasource`, `namespace`, `table`, `error`), each versioned under `v1/`. `scalardb/` holds the
  ScalarDB Cluster definitions the server calls.
- `src/main/java/.../grpc/mapper/`: MapStruct mappers, grouped by service area. Data source
  providers travel as JSON (`provider_payload_json`) and are converted by
  `DataSourceProviderJsonMapper` with the shared provider codecs; see
  [Data sources](../../docs/data-sources.md).

## Conventions

- Mappers share `MapStructConfig` and convert UUIDs through `UuidMapper`. Enum mappers handle the
  protobuf `UNSPECIFIED` and `UNRECOGNIZED` values explicitly.
- `@IgnoreProtobufBuilderDefaults` and `@IgnoreProtobufDefaults` group the builder-generated
  properties that mappers must ignore, such as `*Bytes` fields and `mergeFrom`.

## Build

```bash
./gradlew :grpc:build
```

MapStruct generates the mapper implementations during compilation.
