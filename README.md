# ScalarDB Analytics

[![CI](https://github.com/scalar-labs/scalardb-analytics/actions/workflows/ci.yaml/badge.svg)](https://github.com/scalar-labs/scalardb-analytics/actions/workflows/ci.yaml)
[![CI Spark](https://github.com/scalar-labs/scalardb-analytics/actions/workflows/ci-spark.yaml/badge.svg)](https://github.com/scalar-labs/scalardb-analytics/actions/workflows/ci-spark.yaml)

ScalarDB Analytics is an analytics component of [ScalarDB](https://github.com/scalar-labs/scalardb) that runs analytical queries across heterogeneous databases, including databases managed by [ScalarDB](https://github.com/scalar-labs/scalardb).
It combines a catalog service, which manages metadata and access control, with an Apache Spark connector that reads data from registered data sources.

See the [ScalarDB Analytics documentation](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/design/) to understand the system and run analytical queries.

## Repository contents

| Directory | Description |
| --- | --- |
| [`scalardb-analytics-core/`](scalardb-analytics-core/README.md) | Catalog server, Java client SDK, CLI, and shared APIs |
| [`scalardb-analytics-spark/`](scalardb-analytics-spark/README.md) | Apache Spark connector |

## Build from source

The core build publishes its artifacts to Maven Local, and the Spark build resolves them from there.

### Prerequisites

- JDK 25 for the core Gradle build

The repository includes the Gradle wrapper.
Mill downloads its configured JDK 17 toolchain and build tool version when needed.

### Build the core artifacts used by Spark

```bash
cd scalardb-analytics-core
./gradlew publishToMavenLocal
```

These tasks publish the artifacts to Maven Local, where the Spark build resolves them.

### Build the Spark connector

```bash
cd ../scalardb-analytics-spark
./mill __.compile
./mill __.test
./mill 'sparkAll[2.13,4.1].assembly'
```

The last command builds the Spark 4.1 and Scala 2.13 assembly.
The connector is built for Spark 4.1 and 4.0 with Scala 2.13, and for Spark 3.5 with Scala 2.12 and 2.13.
See the [Spark module README](scalardb-analytics-spark/README.md) for the complete cross-build matrix.

## Documentation

| Goal | Documentation |
| --- | --- |
| Understand the architecture | [Design](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/design/) |
| Run analytical queries | [Run analytical queries](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/run-analytical-queries) |
| Configure authentication and authorization | [Authentication and authorization](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/authentication-and-authorization) |
| Inspect release changes | [Changelog](CHANGELOG.md) |

## Contributing

This project is maintained by the Scalar Engineering Team.
To report a bug or request a feature, open an issue; to ask a question, use [GitHub Discussions](https://github.com/scalar-labs/scalardb-analytics/discussions).
We do not accept pull requests from external contributors yet; see [CONTRIBUTING.md](CONTRIBUTING.md) for the reason and our plans.

Report security vulnerabilities privately as described in [SECURITY.md](SECURITY.md).
Everyone who participates in this project is expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

ScalarDB Analytics is dual-licensed under both the Apache 2.0 License (found in
the [LICENSE](LICENSE) file) and a commercial license. You may select, at your
option, one of the licenses. For more information about the commercial license,
please [contact us](https://www.scalar-labs.com/contact-us).
