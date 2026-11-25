# ScalarDB Analytics Spark

ScalarDB Analytics Spark provides the `ScalarDbAnalyticsCatalog` connector.
The connector retrieves catalog metadata over gRPC and reads registered data sources with Apache Spark.
See the [official design](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/design/) for the request and data flow.

## Build and test

The build resolves ScalarDB Analytics client artifacts from Maven Local.
Follow the root [build instructions](../README.md#build-the-core-artifacts-used-by-spark) before running Mill.

```bash
./mill __.compile
./mill __.test
./mill 'sparkAll[2.13,3.5].assembly'
```

Build the assembly from `sparkAll`, not `spark`. Mill can assemble any module, so
`spark[…].assembly` also succeeds, but it produces a jar without the license and notice texts
that is not the published artifact.

Use these commands to format or check Scala sources:

```bash
./mill mill.scalalib.scalafmt/reformatAll
./mill mill.scalalib.scalafmt/checkFormatAll
```

Use these commands to check dependency licenses and generate third-party notices:

```bash
./mill checkLicense
./mill 'sparkAll[2.13,3.5].generateLicenseReport'
```

The notices are generated into the assembly at packaging time and are not committed. See
[`licenses/DEPENDENCY-LICENSE-POLICY.md`](../licenses/DEPENDENCY-LICENSE-POLICY.md).

## Cross-build matrix

| Scala | Spark |
| --- | --- |
| 2.12.20 | 3.4.4 |
| 2.12.20 | 3.5.6 |
| 2.13.16 | 3.4.4 |
| 2.13.16 | 3.5.6 |

## Submodules

| Module | Description |
| --- | --- |
| `compat/` | Compatibility code for supported Spark and Scala versions |
| `lib/` | Shared connector implementation |
| `spark/` | Connector implementation. Published to Maven Local only, for builds that compile against it |
| `sparkAll` | Published assembly, `scalardb-analytics-spark-all-<spark>_<scala>`. No sources of its own: it assembles `spark` and its dependencies |
| `datasource/scalardb/` | ScalarDB data source |
| `datasource/dynamodb/` | DynamoDB data source |

Mill 1.0.6 builds the module with JDK 17.
The assembly relocates selected transitive libraries to avoid conflicts with the Spark runtime.

## License

ScalarDB Analytics Spark is dual-licensed under both the Apache 2.0 License
(found in the [LICENSE](../LICENSE) file) and a commercial license. You may
select, at your option, one of the licenses. For more information about the
commercial license, please [contact us](https://www.scalar-labs.com/contact-us).
