# ScalarDB Analytics

ScalarDB Analytics enables analytical queries across heterogeneous databases managed by ScalarDB. It consists of two decoupled components: a **Universal Data Catalog** (hierarchical metadata management for catalogs, data sources, namespaces, and tables) and a **Query Engine** (Apache Spark connector that fetches metadata from the catalog and reads data from sources). For detailed design, see the [official design document](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/design/).

## Repository Layout

| Directory | Description |
|-----------|-------------|
| [`scalardb-analytics-core/`](scalardb-analytics-core/README.md) | Java server, CLI, client SDK (Gradle) |
| [`scalardb-analytics-spark/`](scalardb-analytics-spark/README.md) | Scala Spark connector (Mill) |

## Where to Find Information

| Topic | Reference |
|-------|-----------|
| Project design & concepts | [Official design document](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/design/) |
| Running analytical queries | [Official query guide](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/run-analytical-queries) |
| Authentication & authorization | [Official auth guide](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/authentication-and-authorization) |
| Core architecture & modules | [scalardb-analytics-core/README.md](scalardb-analytics-core/README.md) |
| Spark build & cross-build | [scalardb-analytics-spark/README.md](scalardb-analytics-spark/README.md) |
| Server startup & Docker | [server/README.md](scalardb-analytics-core/server/README.md), [docker/server/README.md](scalardb-analytics-core/docker/server/README.md) |
| CLI usage & commands | [client/cli/README.md](scalardb-analytics-core/client/cli/README.md) |
| Third-party dependency licenses & notices | [licenses/DEPENDENCY-LICENSE-POLICY.md](licenses/DEPENDENCY-LICENSE-POLICY.md) |

## Development Rules

### Commits & PRs

- Conventional Commit style: `type(module): description`
  - `feat`, `fix`, `refactor`, `build`, `docs`, `test`, `chore`
  - Module name optional for project-wide changes
- PR titles follow the same format
- **CHANGELOG**: Every PR must include its corresponding update in `CHANGELOG.md` under the `[Unreleased]` section
  - Use the project's custom sections: `Enhancements` (new features), `Improvements` (changes to existing behavior), `Bug fixes`
  - Prefix breaking changes with `[BREAKING]`
  - Omit only when there is no user-facing change (e.g. internal refactors, CI, tests, dependency bumps); regressions in not-yet-released changes are also omitted

### Documentation

- **Root README.md**: Project overview, prerequisites, getting started, module links, license
- **Module READMEs** (`scalardb-analytics-core/README.md`, `scalardb-analytics-spark/README.md`): Architecture, build commands, module/submodule listings, configuration
- **Sub-module READMEs** (e.g., `server/`, `grpc/`, `client/cli/`): Detailed implementation docs for that component
- Do not duplicate information from the official docs — link to them instead

### Code Style & Formatting

- **Java (Core)**: Google Java Format via Spotless, SpotBugs, ErrorProne
- **Scala (Spark)**: Scalafmt, Scalafix, WartRemover
- **Lefthook**: Pre-commit hooks handle trailing space removal, EOF newline enforcement, and `spotlessApply` for Java and Gradle Kotlin DSL files (`.java`, `.kts`). See `lefthook.yml` for details.
- **Source license headers**: Every tracked `.java`, `.scala`, `.kt`, `.kts`, `.py`, and `.mill` file must carry `Copyright Scalar, Inc.` and `SPDX-License-Identifier: Apache-2.0` within its first 12 lines. Spotless inserts the header only for `src/*/java/**/*.java`, so files of the other types need it written by hand. Verify with `./scripts/check-source-license-headers.sh`.

### Java Version Rules

**Policy**: Client-facing modules use Java 8 for broad compatibility. Internal/server-side modules use the latest LTS.

**Client-side modules (Java 8 target)**: `api`, `lib`, `client-sdk`, `client/cli`, `grpc`, `grpc-common`. These are compiled with a modern JDK using `--release 8` (so current Error Prone / NullAway tooling can run) and have their tests executed on a real Java 8 runtime — see `build-logic.java8-conventions`.

**Server-side modules (Java 21)**: `domain`, `usecase`, `usecase-impl`, `service`, `repository`, `repository-impl-spring-data-jdbc`, `server/*`, `tools/*`, `schema-resolver`, `datasource-scalardb`. The `build-logic.java-common-conventions` default toolchain is Java 21; modules fall through to it unless they opt into `java8-conventions`.

**Spark module (Java 17)**: Mill build uses `temurin:17`.

> **Note**: The Spark Mill JVM is `temurin:17` because the Spark cross-build targets Spark 3.4/3.5, which support up to Java 17 (JDK 21 requires Spark 4.0). It is intentionally kept at 17 — not a version-policy gap — and will move only when the Spark module adopts Spark 4.0.

### Build Dependencies

- **Private Maven Repository**: Requires `GPR_USERNAME` / `GPR_PASSWORD` environment variables (or Gradle properties `gprUsername` / `gprPassword`) for `https://maven.pkg.github.com/scalar-labs/scalardb-sql`, which `repository-impl-spring-data-jdbc` resolves. This is a temporary state until ScalarDB SQL is published under an open license.
- **Spark depends on Core**: The Spark module depends on Core artifacts in Maven Local. Run `./gradlew publishToMavenLocal` in `scalardb-analytics-core/` before building Spark.

## Docker

| Image | Purpose |
|-------|---------|
| `ghcr.io/scalar-labs/scalardb-analytics-server` | Catalog server, built from the `server:app` distribution |
| `ghcr.io/scalar-labs/scalardb-analytics-cli` | Command-line client |

Base image: `eclipse-temurin:21-jdk-noble`. Runs as non-root user `scalardb` (UID 201).
