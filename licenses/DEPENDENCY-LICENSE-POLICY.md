# Dependency License Policy

This document covers two things: which dependency licenses ScalarDB Analytics permits, and how the
resulting redistribution obligations are met in the artifacts it publishes.

The permitted licenses are matched by [`allowed-licenses.json`](../allowed-licenses.json), the
executable policy used by both license checks: `./gradlew :checkLicense` in
`scalardb-analytics-core/` and `./mill checkLicense` in `scalardb-analytics-spark/`. The sections
below record why each entry is permitted; this document does not change the check result.

## Permissive and public-domain licenses

The following licenses permit redistribution of unmodified binaries:

- Apache License, Version 2.0
- MIT License
- MIT-0
- PUBLIC DOMAIN
- The 2-Clause BSD License
- The 3-Clause BSD License
- BSD Zero Clause License
- The BSD License
- BSD licence
- Unicode/ICU License
- Go License
- Bouncy Castle Licence

## Weak copyleft licenses

The following licenses are permitted only for unmodified libraries that remain separable from
ScalarDB Analytics:

- Eclipse Public License - v 2.0
- MPL 2.0
- Common Public License - v 1.0
- LGPL-2.1-or-later
- GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1
- GNU LESSER GENERAL PUBLIC LICENSE, Version 3
- COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0
- COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1

## Dual-licensed dependencies

ScalarDB Analytics uses the CDDL or classpath-exception option identified by the dependency
metadata for these licenses:

- GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception
- CDDL + GPLv2 with classpath exception
- CDDL/GPLv2+CE
- Dual license consisting of the CDDL v1.1 and GPL v2

## Product-specific redistribution terms

The following terms permit redistribution of unmodified dependencies. The distribution includes
the Oracle FUTC text when it includes Oracle JDBC:

- International Program License Agreement (IPLA)
- Oracle Free Use Terms and Conditions (FUTC)

## Missing POM metadata

The `org.antlr:ST4:4.3.1` POM omits license metadata. Its upstream `LICENSE.txt` contains the
three-clause BSD license, so the executable policy permits the missing value only for that exact
group, artifact, and version.

## How the obligations are met

Every published artifact that bundles third-party code carries an aggregate notice generated from
that artifact's own runtime classpath.

| Artifact | Carries | Scope of the notice |
| --- | --- | --- |
| CLI fat JAR | `META-INF/THIRD-PARTY-NOTICES.txt` | The `client:cli` runtime classpath |
| Server distribution (`server:app`) | `THIRD-PARTY-NOTICES.txt` and `ORACLE-FUTC.txt` at the distribution root | The distribution's own runtime classpath |
| Spark assemblies (`scalardb-analytics-spark-all-3.x`) | `META-INF/THIRD-PARTY-NOTICES.txt` and `META-INF/ORACLE-FUTC.txt` | Each assembly's own runtime classpath |
| Gradle modules published to Maven Central | Nothing | Not applicable |

The Gradle modules bundle no third-party code, so no attribution travels with them. The CLI fat JAR
merges its dependencies into a single archive, which collapses their `META-INF/NOTICE` entries by
first-wins; the aggregate notice replaces what that merge destroys. The server distribution keeps
each dependency as a separate JAR under `lib/`, where the aggregate notice makes the combined
attribution readable without opening every archive.

The server distribution and the Spark assemblies bundle Oracle JDBC, so both carry the Oracle FUTC
text. The server distribution packages the committed
[`scalardb-analytics-core/licenses/ORACLE-FUTC.txt`](../scalardb-analytics-core/licenses/ORACLE-FUTC.txt).
That file sits under `scalardb-analytics-core/` because the Docker builds use that directory as their
build context and cannot reach anything above it. The Spark assemblies extract the text from the
`ojdbc8` JAR at packaging time instead, so nothing has to be kept in sync there.

Each notice is scoped to one artifact rather than to the whole repository so that it describes what
that artifact actually contains. A repository-wide notice would attribute the server's database
drivers to the CLI, which bundles none of them.

### The two Gradle conventions

`build-logic.license-notices` applies to each distributed artifact and produces that artifact's
notice. Generation runs as part of packaging, so the file always matches the dependencies that were
just resolved. Gradle skips it when those dependencies have not changed, which keeps the cost off
ordinary builds.

`build-logic.license-reporting` applies to the root project and covers the union of every module.
It backs `./gradlew :checkLicense`, the policy check that `.github/workflows/license-check.yaml`
runs on every pull request. Its report is an intermediate result and is never published.

### The Mill equivalents

The Spark build splits the same two responsibilities. `spark[…].generateLicenseReport` produces one
assembly variant's notice from that variant's own runtime classpath, and is an input of that
variant's `assembly` only, so compiling and testing do not depend on the license machinery. The
`licenseReport` module merges every variant's inventory and backs `./mill checkLicense`, the policy
check that `.github/workflows/ci-spark.yaml` runs on every pull request; like the Gradle root
report, the merged inventory is never published.

## Why the notices are not committed

The repository holds no checked-in `THIRD-PARTY-NOTICES.txt`, and no workflow compares a committed
copy against a generated one. Restoring either would reintroduce a check that cannot pass.

Such a comparison assumes the resolved dependency graph is a function of the commit. Two things
break that assumption: the ScalarDB dependency is a SNAPSHOT during a development cycle, and
Dependabot opens grouped update pull requests every week against `main` and each active release
branch. Both change the resolved graph without any change to this repository's own sources, and
Dependabot cannot regenerate a notice file. Every one of those pull requests would fail until
someone regenerated the file by hand on each branch.

Generating at packaging time removes the question. Nothing has to be kept in sync, and the notice
in a published artifact is correct by construction rather than by discipline.

## When the license check fails

`./gradlew :checkLicense` fails when a dependency carries a license that
[`allowed-licenses.json`](../allowed-licenses.json) does not match. The offending entries are
listed in `dependencies-without-allowed-license.json` under
`scalardb-analytics-core/build/reports/dependency-license/`, which the CI job prints on failure.

`./mill checkLicense` fails the same way for the Spark assemblies. It needs no report file: the
exception it raises lists each rejected coordinate with the licenses that were found.

Decide whether the license permits redistribution under the terms above before changing anything.
Adding an entry to `allowed-licenses.json` so that the check passes is the wrong order: the check
reports a licensing question, and the answer may be to replace the dependency instead. When an
entry is added, record the reason in the sections above.

## Adding a distributed artifact

Apply `build-logic.license-notices` to the new artifact's project and copy the generated
`THIRD-PARTY-NOTICES.txt` into whatever that artifact publishes. Include `ORACLE-FUTC.txt` as well
if it bundles Oracle JDBC. An artifact that bundles no third-party code needs neither.
