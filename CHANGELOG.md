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

### Bug fixes

- Reject granting an Analytics permission on a namespace or table of a ScalarDB data source when ACL delegation is enabled. Authorization for those resources is delegated to ScalarDB Cluster, so the grant was stored but never took effect; it now fails with `DB-ANALYTICS-10204` (`RESOURCE_UNDER_ACL_DELEGATION`). Revoking such permissions is still allowed
