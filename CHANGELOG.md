# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Note: This changelog uses custom section names (Enhancements, Improvements, Bug fixes)
instead of the standard Keep a Changelog types (Added, Changed, Fixed, etc.).

Releases up to and including v3.19.0 predate this repository. This file records the
changes released from here, starting with the entries below.

## [Unreleased]

### Enhancements

- Added Spark 4.1 and Spark 4.0 support. The Spark connector is published for Spark 4.1.4 and 4.0.5 with Scala 2.13, as `scalardb-analytics-spark-all-4.1_2.13` and `scalardb-analytics-spark-all-4.0_2.13`. Spark 4 is not published for Scala 2.12, so there are no Scala 2.12 artifacts for these versions. On Spark 4, the connector uses the Databricks JDBC dialect built into Spark (#85)
- Added `configSources` to `ScalarDbAnalyticsClient.Builder` in the Java SDK, so that an application can read the authentication credential from the same environment variables and `client.properties` keys as the CLI (#78)

### Improvements

- [BREAKING] Raised the JDK of the server-side modules and the core Gradle build from Java 21 to Java 25. The server-side modules are compiled for Java 25, the server and CLI images run on Java 25 (`eclipse-temurin:25.0.5_8-jdk-resolute` and `eclipse-temurin:25.0.5_8-jre-resolute` instead of `eclipse-temurin:21.0.13_9-jdk-resolute` and `eclipse-temurin:21.0.13_9-jre-resolute`), and building `scalardb-analytics-core` from source requires JDK 25. The client-facing modules (`api`, `lib`, `grpc`, `grpc-common`, the Java client SDK, and the CLI) still target Java 8 (#84)
- The Spark 3.5 connector is now compiled with Scala 2.12.18 and 2.13.8, the versions that Spark 3.5 is built with and runs on, instead of 2.12.20 and 2.13.16, so it cannot call `scala-library` methods that the Spark 3.5 runtime lacks (#85)
- [BREAKING] Dropped Spark 3.4 support. The `scalardb-analytics-spark-all-3.4_2.12` and `scalardb-analytics-spark-all-3.4_2.13` artifacts are no longer published; use Spark 3.5, 4.0, or 4.1 instead (#85)
- [BREAKING] The server now always runs the transactions on its database at the `SERIALIZABLE` isolation level instead of the ScalarDB default, `SNAPSHOT`. An isolation level set through `scalar.db.analytics.server.db.parameters` (`scalar.db.consensus_commit.isolation_level`, or the deprecated `scalar.db.isolation_level`) is ignored, and the server logs a warning at startup. Concurrent requests that touch the same entities can now fail with the retryable `DB-ANALYTICS-30003` described below. The Java client SDK, the CLI, and the Spark connector retry it automatically; clients that disable retries or call the gRPC API directly need to retry it themselves (#95)
- A transaction conflict on the server's database is now reported as `DB-ANALYTICS-30003` (`ANALYTICS_DB_TRANSACTION_CONFLICT`) with gRPC `UNAVAILABLE`, which the client SDK retries. Before, a conflict at commit was reported as `DB-ANALYTICS-30103` (`INTERNAL_ERROR`) with gRPC `INTERNAL`, and a conflict during an operation as `DB-ANALYTICS-30000` (`ANALYTICS_DB_CONNECTION_FAILED`) or `DB-ANALYTICS-30104` (`ANALYTICS_DB_OPERATION_FAILED`) (#95)
- [BREAKING] Operations that refer to a catalog, data source, namespace, table, role, user, backend user, or backend user link that does not exist now fail with gRPC `NOT_FOUND` and a dedicated error code (`DB-ANALYTICS-10500` to `DB-ANALYTICS-10507`) that carries the names or IDs in its metadata, instead of `INVALID_ARGUMENT` with `DB-ANALYTICS-10400` and no metadata. This covers registering a data source, granting and revoking roles and permissions by name, listing the permissions of a user or role, linking and unlinking backend users, and the role in by-ID role grants and revocations and the data source, namespace, or table in by-ID permission grants and revocations (#79)
- [BREAKING] `user link` fails with `DB-ANALYTICS-10108` (`BACKEND_USER_ALREADY_LINKED`) when the backend user is already linked or the user is already linked on the same backend, instead of `DB-ANALYTICS-10400`. The `reason` metadata (`backend_user_linked` or `user_linked_on_backend`) tells the two apart (#79)
- Validation failures in data source registration (a manual schema given for a provider that resolves schemas, or missing for one that does not), role creation, and user creation (a blank name) now carry `field` and `reason` in the metadata of `DB-ANALYTICS-10400` (#79)
- Requests with a field that the server cannot decode, such as a malformed ID in a `*ById` call, an unspecified or unrecognized grantee type, permission, or password backend, or provider or schema JSON in data source registration or catalog initialization that does not decode, now fail with `DB-ANALYTICS-10400` carrying `field` and `reason` in its metadata (and, in catalog initialization, `data_source_index`, the 0-based index of the failed entry), instead of `DB-ANALYTICS-10400` without metadata (or `DB-ANALYTICS-30103` and gRPC `INTERNAL` for a registration without provider JSON) (#79)
- [BREAKING] The server no longer reports an `IllegalArgumentException` that it does not expect as `DB-ANALYTICS-10400` (`INVALID_ARGUMENT`) without metadata. Invalid input is reported with `field` and `reason` metadata as listed above, so such an exception indicates a server-side defect and is reported as `DB-ANALYTICS-30103` (`INTERNAL`). A data source configuration that ScalarDB or a JDBC driver rejects with an `IllegalArgumentException`, such as an unsupported `scalar.db.storage`, is still reported as `DB-ANALYTICS-10400`, now with `field` set to `provider_payload_json` and `reason` to `invalid_configuration` (#79)
- [BREAKING] Internal user directory operations (`internal-backend create` and `internal-backend delete` in the CLI) on a server that uses a password backend other than `internal` fail with gRPC `FAILED_PRECONDITION` and `DB-ANALYTICS-10205` (`INTERNAL_PASSWORD_BACKEND_REQUIRED`) instead of a bare `UNIMPLEMENTED`. Callers other than a superadmin get `DB-ANALYTICS-10003` (`ACCESS_DENIED`), as with the internal backend (#79)
- [BREAKING] With `scalar.db.analytics.server.auth.password.backend=scalardb-cluster`, `user create --backend-user` fails with gRPC `FAILED_PRECONDITION` and `DB-ANALYTICS-10205` (`INTERNAL_PASSWORD_BACKEND_REQUIRED`) instead of `INVALID_ARGUMENT` and `DB-ANALYTICS-10400` (#79)
- The CLI and the Spark connector now resolve the authentication credential with the same rules (#78)

### Bug fixes

- Fixed an issue where a user who had ended up with more than one access token, which concurrent logins of a user who held no token could cause, could no longer log in, be deleted, be unlinked from a backend user, or have their internal backend user deleted with cascade; these operations failed with `DB-ANALYTICS-30104` (`ANALYTICS_DB_OPERATION_FAILED`). They now remove every token of the user (#93)
- Wait for the gRPC server and its cancelled call handlers to finish after a forced shutdown, so that the resources the handlers use are not released while they are still running, and stop the already started gRPC servers when server startup fails partway (#76)
- Fixed an issue where two concurrent requests that created a catalog, data source, namespace, table, or role with the same name, or that linked the same user to the same password backend, could both succeed and leave a duplicate. Every later operation that looked the entity up by that name or link then failed with `DB-ANALYTICS-30104`. Now at most one of the requests succeeds and the others fail with the retryable `DB-ANALYTICS-30003`; a retry either creates the entity or reports its `*_ALREADY_EXISTS` error. Duplicates created before this fix are not removed (#95)
- Fixed an issue where catalog initialization registered each data source in the catalog named by `catalog_name` of its `data_sources` entry rather than the catalog being initialized, so an entry without it failed with `DB-ANALYTICS-10400` (`INVALID_ARGUMENT`) and an entry naming another catalog was registered there. The `catalog_name` of each entry is now ignored. Errors for an entry also carry `data_source_index` in their metadata (#79)
- Fixed an issue where registering a data source, or initializing a catalog, with provider JSON that the provider rejects, such as a DynamoDB provider without a region or an endpoint or a field of the wrong type, failed with gRPC `INTERNAL` and `DB-ANALYTICS-30103`. It now fails with `INVALID_ARGUMENT` and `DB-ANALYTICS-10400`, with `field` set to `provider_payload_json` and `reason` to `invalid` (#79)
- Fixed an issue where the Spark connector trimmed leading and trailing whitespace from the password, contrary to its intent. The password is now used as is (#78)

## [v3.20.0] - 2026-11-06

### Enhancements

- [BREAKING] Report the columns left out during data source registration because their source types are not supported. `RegisterResponse` carries one `warnings` entry per skipped column, `DataSourceClient.register` in the Java SDK returns a `RegisterDataSourceResult` with the registered data source and the skipped columns, and `data-source register` in the CLI prints them to stderr after the registered data source (#56)
- Added a default catalog for the CLI, set with `scalar.db.analytics.client.default.catalog` in the client configuration or `SCALAR_DB_ANALYTICS_CLIENT_DEFAULT_CATALOG`. The `data-source`, `namespace`, and `table` commands and `permission grant`/`revoke` on a data source, namespace, or table use it when `--catalog` is omitted; `--catalog` still takes precedence (#62)

### Improvements

- Hardened the server and CLI images: pinned the base images and the `grpc_health_probe` image by digest, and created the `scalardb` user with a nologin shell (#73)
- [BREAKING] The application files under `/scalardb-analytics-server` in the server image and `/scalardb-analytics-cli` in the CLI image are now owned by root, so the runtime user can no longer modify the image-provided files or create files in those directories. Deployments that write to the image's own application directories must use a writable volume or another writable location instead. Existing bind mounts and volumes, including those under these directories, retain their own permissions and do not need to be relocated if UID/GID 201 can access them (#73)
- Moved the server image from `eclipse-temurin:21-jdk-noble` (Ubuntu 24.04) to `eclipse-temurin:21.0.13_9-jdk-resolute` (Ubuntu 26.04), and the CLI image from `eclipse-temurin:21-jre-noble` to `eclipse-temurin:21.0.13_9-jre-resolute`. Eclipse Temurin 21.0.13+9 contains the eMudhra fix; its updated trust store removes the root certificates that signed the compromised intermediates (#73)
- Replaced MySQL Connector/J with MariaDB Connector/J for MySQL data sources (#2)
- Added an optional `sslMode` field to the MySQL data source provider. It defaults to `trust` rather than the MariaDB Connector/J default of `disable`, so that MySQL 8.x accounts using the default `caching_sha2_password` plugin can authenticate over the TLS that MySQL 8.0 enables by default. Set `sslMode` to `disable` explicitly for MySQL servers with TLS turned off (#2)
- Include consolidated third-party notices in the CLI and server distributions, and the Oracle JDBC redistribution terms in the server distribution (#2)
- Include the Apache License 2.0 text in the CLI and server distributions, so the published container images carry the license they declare (#2)
- Include consolidated third-party notices and the Oracle JDBC redistribution terms in Spark assembly artifacts, and stop shipping the individual third-party `META-INF/LICENSE` and `NOTICE` files that previously landed there by collision (#2)
- Made ScalarDB Analytics available under either the Apache License 2.0 or a
  commercial license. (#2)
- Rewrote the repository READMEs around the public project scope, source builds, and links to the official documentation (#2)
- [BREAKING] Renamed the `without-licensing` server distribution to `app` and its image from `ghcr.io/scalar-labs/scalardb-analytics-server-without-licensing` to `ghcr.io/scalar-labs/scalardb-analytics-server` (#2)
- [BREAKING] `com.scalar.db.analytics.spark.ScalarDbAnalyticsCatalog` no longer requires a usage metering service or the metering Spark listener. The server image exposes port 11051 only (#2)
- [BREAKING] `permission list` in the CLI shows each permission in the form that `permission grant` takes, with the resource type, the permission level, and the catalog, data source, namespace, and table names, instead of the raw permission name and resource ID. The gRPC response and `EffectivePermission` in the Java SDK carry the resource names as well (#58)
- [BREAKING] Data source registration fails with `DB-ANALYTICS-10401` when a namespace, table, column, or column type name in the schema contains a control character (Unicode category Cc, such as a line break or ESC) or a bidirectional formatting character (U+202A to U+202E, U+2066 to U+2069), instead of registering the name as is. The error names the field and its path, with the characters escaped. Printable Unicode names are accepted as before (#60)

### Bug fixes

- Reject granting an Analytics permission on a namespace or table of a ScalarDB data source when ACL delegation is enabled. Authorization for those resources is delegated to ScalarDB Cluster, so the grant was stored but never took effect; it now fails with `DB-ANALYTICS-10204` (`RESOURCE_UNDER_ACL_DELEGATION`). Revoking such permissions is still allowed (#57)
- Fixed an issue where the server failed to start with authentication enabled and `scalar.db.analytics.server.auth.password.backend=scalardb-cluster`. With that backend, `user create --backend-user` now fails with `DB-ANALYTICS-10400` (`INVALID_ARGUMENT`), since it creates an internal-backend user that can't log in; create the user in ScalarDB Cluster and link it with `user link` instead (#72)
- Fixed an issue where data cached with `CACHE TABLE` or `cache()` for a DynamoDB table was not used by later queries that load the table again, so they read from DynamoDB instead (#61)

[Unreleased]: https://github.com/scalar-labs/scalardb-analytics/compare/v3.20.0...HEAD
[v3.20.0]: https://github.com/scalar-labs/scalardb-analytics/releases/tag/v3.20.0
