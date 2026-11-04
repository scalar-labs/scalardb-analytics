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
- Added `configSources` to `ScalarDbAnalyticsClient.Builder` and the `com.scalar.db.analytics.sdk.config` package to the Java SDK, so that an application can read the authentication credential from the same environment variables as the CLI and the Spark connector (`SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME`, `SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD`) and from the CLI's `client.properties` keys, with the same precedence: a credential set on the builder, then the environment, then the file. It is opt-in; a builder without `configSources` behaves as before

### Improvements

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
- The CLI and the Spark connector resolve the authentication credential through the Java SDK, so they apply the same rules: for each of the username and the password, the environment variable takes precedence over the configuration file or the `server.auth.*` catalog option, a blank username is treated as unset and falls through to the next source, and the password is used as is. In the CLI, a blank `SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME` no longer hides `scalar.db.analytics.client.auth.username` in the configuration file
- [BREAKING] The Spark connector no longer trims leading and trailing whitespace from `server.auth.password` or `SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD`, so the password is sent as it is set, as in the CLI

### Bug fixes

- Reject granting an Analytics permission on a namespace or table of a ScalarDB data source when ACL delegation is enabled. Authorization for those resources is delegated to ScalarDB Cluster, so the grant was stored but never took effect; it now fails with `DB-ANALYTICS-10204` (`RESOURCE_UNDER_ACL_DELEGATION`). Revoking such permissions is still allowed
- Fixed an issue where the server failed to start with authentication enabled and `scalar.db.analytics.server.auth.password.backend=scalardb-cluster`. With that backend, `user create --backend-user` now fails with `DB-ANALYTICS-10400` (`INVALID_ARGUMENT`), since it creates an internal-backend user that can't log in; create the user in ScalarDB Cluster and link it with `user link` instead
- Fixed an issue where data cached with `CACHE TABLE` or `cache()` for a DynamoDB table was not used by later queries that load the table again, so they read from DynamoDB instead
- Fixed an issue where the CLI expanded `${...}` in `scalar.db.analytics.client.auth.username` and `scalar.db.analytics.client.auth.password`, whether set in the configuration file or the environment, so that a password containing `${` was altered. The values are now used literally
