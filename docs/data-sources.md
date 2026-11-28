# Data sources

A data source is one external database registered in a catalog. The catalog server stores how to
connect to it and the structure it exposes; the Spark connector later uses the same information to
read its data. This document explains how a data source is described, how that description travels
between components, and what registration does with it.

## Provider payloads

Each data source has a **provider**: a typed description of how to reach the source, such as host,
port, and credentials for a relational database, or the ScalarDB configuration for a ScalarDB data
source. Supported providers are MySQL, PostgreSQL, Oracle, SQL Server, Databricks, Snowflake,
ScalarDB, and DynamoDB. Each is a class implementing `DataSourceProvider` in
`api` (`com.scalar.db.analytics.api.model.datasource.provider`).

Outside the JVM, a provider is a JSON **provider payload**: a single object whose `type` field names
the provider and whose remaining fields are that provider's settings.

```json
{
  "type": "postgresql",
  "host": "db.example.com",
  "port": 5432,
  "username": "analytics",
  "password": "secret",
  "database": "orders"
}
```

The same payload is the representation at every boundary. The CLI accepts it from the user, the
gRPC API carries it as the `provider_payload_json` string, and the metadata store keeps it in the
`provider_payload_json` column of the data source table. No proto message or table is specific to
one provider, so adding a provider does not change the gRPC contract or the metadata schema.

## Codecs

Every boundary converts between payloads and provider objects through the same codecs in
`api` (`com.scalar.db.analytics.api.codec.provider`):

- `ProviderCodec<T>` converts one provider type, and each provider has its own codec.
- `ProviderCodecRegistry` maps each provider type name and class to its codec.
- `DataSourceProviderCodec` reads the `type` field, picks the codec, and delegates.

The CLI, the gRPC mappers (`DataSourceProviderJsonMapper`), and the repository implementation all
use these codecs, so a payload that one boundary accepts is accepted by all of them. Decoding
rejects unknown fields, so a misspelled setting fails at registration instead of being ignored.
Why each provider has its own codec is recorded in
[design/serialize-each-provider-with-its-own-codec.md](design/serialize-each-provider-with-its-own-codec.md).

## Registration

`data-source register` in the CLI takes the catalog name, the data source name, and the provider
payload from inline JSON, a file, or standard input. Before sending the payload, the CLI replaces
every `${file:<path>}` string in it with the referenced file's contents: a `.json` file as its JSON
value, and a `.properties` file as an object of string values. Why this happens in the CLI is
recorded in [design/resolve-file-references-in-the-cli.md](design/resolve-file-references-in-the-cli.md).

On the server, registration runs in one transaction:

1. The use case checks that the caller may write to the catalog
   (see [Authorization](authorization.md)).
2. The payload is decoded into a provider object.
3. The structure of the source is resolved. For providers that can describe themselves, a schema
   resolver connects to the source and discovers its namespaces, tables, and columns: JDBC
   metadata for relational databases (`schema-resolver`) and the ScalarDB admin API for ScalarDB
   (`datasource-scalardb`). DynamoDB cannot describe its columns, so its structure comes from the
   schema JSON supplied with the request.
4. The discovered or declared structure is validated and stored as namespaces, tables, and columns
   under the new data source. Columns whose source type has no ScalarDB Analytics equivalent are
   skipped and reported back to the caller as warnings.

If the server cannot connect to the source, registration fails with `DATA_SOURCE_UNREACHABLE`;
if the supplied structure is invalid, it fails with `INVALID_SCHEMA_METADATA`
(see [Error handling](error-handling.md)).
