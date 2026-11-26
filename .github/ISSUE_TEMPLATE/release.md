---
name: Release
about: Track the tasks for a ScalarDB Analytics release
title: "Release X.Y.Z"
---

Tracking issue for the X.Y.Z release. Delete the section that does not apply.

Version locations referenced below:

| File | Key |
|------|-----|
| `scalardb-analytics-core/VERSION` | this project's version |
| `scalardb-analytics-core/gradle/libs.versions.toml` | `scalardb` |
| `scalardb-analytics-spark/build.mill` | `Versions.scalardb` |

## Minor release (X.Y.0)

### Preparation

- [ ] ScalarDB `vX.Y.0` is released — `gh release view vX.Y.0 --repo scalar-labs/scalardb`; `release.yaml` does not check this
- [ ] Every PR planned for this release is merged into `main`
- [ ] `CHANGELOG.md` `[Unreleased]` reviewed: entries accurate, PR references present, `[BREAKING]` prefixes correct

### Release commit (on `main`)

- [ ] Set `VERSION` and the ScalarDB dependency to `X.Y.0`
- [ ] `CHANGELOG.md`: turn `[Unreleased]` into `## [vX.Y.0] - YYYY-MM-DD` (placeholder date; corrected before tagging) and leave a new empty `[Unreleased]` above it
- [ ] `CHANGELOG.md`: repoint the `[Unreleased]` compare link and add the `[vX.Y.0]` compare link
- [ ] `build: release X.Y.0` PR merged into `main`

### Branch and tag

- [ ] `## [vX.Y.0]` heading date set to the actual tag-push date on `main`, before the release branch is cut
- [ ] `release/X.Y` branch created from the release commit and pushed
- [ ] Annotated tag `vX.Y.0` created on the same commit and pushed
- [ ] `release.yaml` succeeded for all jobs

### After the tag — easy to forget

- [ ] `Next release` milestone renamed to `vX.Y.0` and closed, with its open Issues moved to a new `Next release` (`docs/issue-workflow.md#at-each-release`)
- [ ] `main` returned to `X.(Y+1).0-SNAPSHOT` in `VERSION` and the ScalarDB dependency — nothing is published to the SNAPSHOT repository while `main` carries the released version
- [ ] SNAPSHOT publish confirmed on the next `main` push — the job is **skipped without failing** while the version has no `-SNAPSHOT` suffix
- [ ] Enterprise release notified that `vX.Y.0` is out

### Verification

- [ ] `scalardb-analytics-spark-all` published to Maven Central for all four Scala × Spark variants
- [ ] CLI and server images pushed to GHCR
- [ ] GitHub release created with notes, the CLI JAR, and `scalardb-analytics-status-codes.mdx` attached

## Patch release (X.Y.Z, Z > 0)

- [ ] ScalarDB `vX.Y.Z` is released — `gh release view vX.Y.Z --repo scalar-labs/scalardb`
- [ ] Every fix for this release is merged into `release/X.Y`
- [ ] `CHANGELOG.md` on `release/X.Y` has a `## [vX.Y.Z] - YYYY-MM-DD` section (date = actual tag-push date) and its compare link
- [ ] `build: release X.Y.Z` merged into `release/X.Y`, setting `VERSION` and the ScalarDB dependency to `X.Y.Z`
- [ ] Annotated tag `vX.Y.Z` created on that commit and pushed
- [ ] `release.yaml` succeeded for all jobs
- [ ] Enterprise release notified that `vX.Y.Z` is out
- [ ] Changelog entries for the fixes are also reflected on `main` if the fixes were backported from it

`main` is not touched for a patch release, so no SNAPSHOT bump is needed.

## Notes
