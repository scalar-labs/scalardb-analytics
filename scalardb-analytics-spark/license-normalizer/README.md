# License normalization data

`default-license-normalizer-bundle.json` is an unmodified copy of the default
normalization rules distributed with Gradle License Report 3.1.2.

- Upstream project: <https://github.com/jk1/Gradle-License-Report>
- Upstream version: `v3.1.2`
- Upstream path: `src/main/resources/default-license-normalizer-bundle.json`
- SHA-256: `8b6fab89d26a288b717f0d6c96b4d0c2f457a120e2b889ae9d018784a78c3b89`
- License: Apache License 2.0; see `LICENSE.txt`
- Upstream attribution notice: see `NOTICE.txt`

The Spark license-reporting tasks implement the normalization behavior in
Mill and read this data file. They do not execute code from the Gradle plugin.

When updating the Gradle License Report version used by the core build, copy
the corresponding upstream data file here, update the version and checksum,
and verify that the core and Spark builds normalize shared fixtures identically.
