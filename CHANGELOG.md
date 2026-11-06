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

- [BREAKING] Report the columns left out during data source registration because their source types are not supported. `RegisterResponse` carries one `warnings` entry per skipped column, `DataSourceClient.register` in the Java SDK returns a `RegisterDataSourceResult` with the registered data source and the skipped columns, and `data-source register` in the CLI prints them to stderr after the registered data source
- Added a default catalog for the CLI, set with `scalar.db.analytics.client.default.catalog` in the client configuration or `SCALAR_DB_ANALYTICS_CLIENT_DEFAULT_CATALOG`. The `data-source`, `namespace`, and `table` commands and `permission grant`/`revoke` on a data source, namespace, or table use it when `--catalog` is omitted; `--catalog` still takes precedence

### Improvements

- Hardened the server and CLI images: pinned the base images and the `grpc_health_probe` image by digest, and created the `scalardb` user with a nologin shell
- [BREAKING] The application files under `/scalardb-analytics-server` in the server image and `/scalardb-analytics-cli` in the CLI image are now owned by root, so the runtime user can no longer modify the image-provided files or create files in those directories. Deployments that write to the image's own application directories must use a writable volume or another writable location instead. Existing bind mounts and volumes, including those under these directories, retain their own permissions and do not need to be relocated if UID/GID 201 can access them
- Moved the server image from `eclipse-temurin:21-jdk-noble` (Ubuntu 24.04) to `eclipse-temurin:25.0.5_8-jdk-resolute` (Ubuntu 26.04), and the CLI image from `eclipse-temurin:21-jre-noble` to `eclipse-temurin:25.0.5_8-jre-resolute`. Java 25.0.5 addresses CVE-2026-91403 and CVE-2026-114820 (eMudhra CA compromise) by removing the affected emSign root CAs from `cacerts`
- Replaced MySQL Connector/J with MariaDB Connector/J for MySQL data sources
- Added an optional `sslMode` field to the MySQL data source provider. It defaults to `trust` rather than the MariaDB Connector/J default of `disable`, so that MySQL 8.x accounts using the default `caching_sha2_password` plugin can authenticate over the TLS that MySQL 8.0 enables by default. Set `sslMode` to `disable` explicitly for MySQL servers with TLS turned off
- Include consolidated third-party notices in the CLI and server distributions, and the Oracle JDBC redistribution terms in the server distribution
- Include the Apache License 2.0 text in the CLI and server distributions, so the published container images carry the license they declare
- Include consolidated third-party notices and the Oracle JDBC redistribution terms in Spark assembly artifacts, and stop shipping the individual third-party `META-INF/LICENSE` and `NOTICE` files that previously landed there by collision
- Made ScalarDB Analytics available under either the Apache License 2.0 or a
  commercial license.
- Rewrote the repository READMEs around the public project scope, source builds, and links to the official documentation
- [BREAKING] Renamed the `without-licensing` server distribution to `app` and its image from `ghcr.io/scalar-labs/scalardb-analytics-server-without-licensing` to `ghcr.io/scalar-labs/scalardb-analytics-server`
- [BREAKING] `com.scalar.db.analytics.spark.ScalarDbAnalyticsCatalog` no longer requires a usage metering service or the metering Spark listener. The server image exposes port 11051 only
- [BREAKING] `permission list` in the CLI shows each permission in the form that `permission grant` takes, with the resource type, the permission level, and the catalog, data source, namespace, and table names, instead of the raw permission name and resource ID. The gRPC response and `EffectivePermission` in the Java SDK carry the resource names as well
- [BREAKING] Data source registration fails with `DB-ANALYTICS-10401` when a namespace, table, column, or column type name in the schema contains a control character (Unicode category Cc, such as a line break or ESC) or a bidirectional formatting character (U+202A to U+202E, U+2066 to U+2069), instead of registering the name as is. The error names the field and its path, with the characters escaped. Printable Unicode names are accepted as before
- [BREAKING] Raised the JDK of the server-side modules and the core Gradle build from Java 21 to Java 25. The server-side modules are compiled for Java 25, the server and CLI images run on Java 25, and building `scalardb-analytics-core` from source requires JDK 25. The client-facing modules (`api`, `lib`, `grpc`, `grpc-common`, the Java client SDK, and the CLI) still target Java 8

### Bug fixes

- Reject granting an Analytics permission on a namespace or table of a ScalarDB data source when ACL delegation is enabled. Authorization for those resources is delegated to ScalarDB Cluster, so the grant was stored but never took effect; it now fails with `DB-ANALYTICS-10204` (`RESOURCE_UNDER_ACL_DELEGATION`). Revoking such permissions is still allowed
- Fixed an issue where the server failed to start with authentication enabled and `scalar.db.analytics.server.auth.password.backend=scalardb-cluster`. With that backend, `user create --backend-user` now fails with `DB-ANALYTICS-10400` (`INVALID_ARGUMENT`), since it creates an internal-backend user that can't log in; create the user in ScalarDB Cluster and link it with `user link` instead
- Fixed an issue where data cached with `CACHE TABLE` or `cache()` for a DynamoDB table was not used by later queries that load the table again, so they read from DynamoDB instead
