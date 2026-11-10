---
name: release-process
description: This skill should be used when preparing, executing, or resuming a ScalarDB Analytics release — cutting a minor (X.Y.0) or patch (X.Y.Z) release, creating a release branch, tagging, or returning main to the next SNAPSHOT. Covers the release tracking issue, where the version lives, the order of operations, what release.yaml requires, and the follow-up steps that fail silently when skipped. Not applicable to routine dependency bumps, or to releasing ScalarDB itself.
---

# ScalarDB Analytics Release Process

A release spans several days and does not fit in one session. Drive it from a tracking issue so that any session can pick up where the previous one stopped.

## 1. Open the tracking issue first

```bash
awk 'c>=2 {print} /^---$/ {c++}' .github/ISSUE_TEMPLATE/release.md > "${TMPDIR:-/tmp}/release-issue.md"
gh issue create --title "Release X.Y.Z" --body-file "${TMPDIR:-/tmp}/release-issue.md"
```

(Or pick the **Release** template from the GitHub UI.) Delete the section that does not apply, then keep the checklist current: check items off as they land and record anything unusual under **Notes**. When resuming, read the issue before touching the repository — the checklist, not the local worktree, is the source of truth for what remains.

## 2. Branch and tag model

- `main` carries the next development version and always ends in `-SNAPSHOT`.
- `release/X.Y` is the maintenance branch for a minor line. It is created from the release commit and stays on the released version; it is never returned to a SNAPSHOT.
- Git tags are annotated and **v-prefixed**: `vX.Y.Z`. Docker image tags are not — `scalardb-analytics-core/docker/{cli,server}/Makefile` strips the `v` via `$(patsubst v%,%,$(shell git describe --tags --always))`, so images are published as `X.Y.Z`.
- `CHANGELOG.md` version headings are also v-prefixed (`## [vX.Y.Z]`) because `release.yaml` looks up the heading using the tag name verbatim.

## 3. Where versions live

| File | Holds |
|------|-------|
| `scalardb-analytics-core/VERSION` | This project's version. Both the Gradle and the Mill build read it; nothing else defines it |
| `scalardb-analytics-core/gradle/libs.versions.toml` | `scalardb`, the ScalarDB dependency |
| `scalardb-analytics-spark/build.mill` | `Versions.scalardb`, the same ScalarDB dependency |

The ScalarDB dependency tracks the same version number as this project. To see all three at once:

```bash
cat scalardb-analytics-core/VERSION
rg -n '^scalardb = "|val scalardb ' \
  scalardb-analytics-core/gradle/libs.versions.toml \
  scalardb-analytics-spark/build.mill
```

**Other builds take their version from this file.** The Enterprise repository consumes this one as a submodule and reads `VERSION` from it, so its releases carry the same numbers as this repository's and follow them: an Enterprise `vX.Y.Z` can only be released once this repository has released `vX.Y.Z`.

## 4. Minor release (X.Y.0)

1. **Confirm ScalarDB `vX.Y.0` is released.**
   ```bash
   gh release view vX.Y.0 --repo scalar-labs/scalardb
   ```
   The release build resolves ScalarDB `X.Y.0`, and `release.yaml` does not check for it beforehand. Check it yourself before tagging.
2. **Finalize the changelog** on `main`: check that every entry in `[Unreleased]` has its PR reference and that breaking changes carry the `[BREAKING]` prefix.
3. **Open the release PR** (`build: release X.Y.0`). It sets `VERSION` and the ScalarDB dependency to `X.Y.0` and, in `CHANGELOG.md`, inserts a `## [vX.Y.0] - YYYY-MM-DD` heading directly below `[Unreleased]` so the accumulated entries fall under the new version and `[Unreleased]` is left empty, repoints the `[Unreleased]` compare link to `vX.Y.0...HEAD`, and adds the `[vX.Y.0]` compare link. Do not push the tag from this PR. The date in that heading is a placeholder — tagging usually happens days later.
4. **Correct the release date** if the tag is not pushed on the date in the `## [vX.Y.0]` heading. Fix it on `main` as its own `build:` PR before cutting the branch, so the release branch and the tag both carry the corrected commit — that commit is the `<release-commit>` used below. The heading line itself is not part of the GitHub release notes (`release.yaml` extracts from the line after it), so a stale date shows up in `CHANGELOG.md`, not in the release notes.
5. **Create the release branch** from the merged release commit and push it:
   ```bash
   git branch release/X.Y <release-commit> && git push origin release/X.Y
   ```
6. **Tag the same commit** and push, which triggers `release.yaml`:
   ```bash
   git tag -a vX.Y.0 -m "Release vX.Y.0" <release-commit> && git push origin vX.Y.0
   ```
7. **Return `main` to `X.(Y+1).0-SNAPSHOT`.** This is the step that gets forgotten — see section 6.
8. **Verify the outputs** listed in the tracking issue.
9. **Hand over to the Enterprise release**, which moves its submodule to this `vX.Y.0` tag.

## 5. Patch release (X.Y.Z, Z > 0)

Everything happens on `release/X.Y`; `main` is not touched, so no SNAPSHOT bump is involved.

A patch release is also needed when only the Enterprise repository has fixes: it cannot release `vX.Y.Z` until this repository has, even if nothing changed here.

1. **Confirm ScalarDB `vX.Y.Z` is released**, as in section 4.
2. Merge the fixes into `release/X.Y` (backport from `main` when the fix also applies there).
3. Add the `## [vX.Y.Z] - YYYY-MM-DD` section and its compare link to `CHANGELOG.md` on that branch.
4. Merge `build: release X.Y.Z`, setting `VERSION` and the ScalarDB dependency to `X.Y.Z`.
5. Create and push the annotated tag `vX.Y.Z`.
6. Hand over to the Enterprise release.

## 6. Returning `main` to the next SNAPSHOT

Set `VERSION` and the ScalarDB dependency on `main` to `X.(Y+1).0-SNAPSHOT` as its own `build:` PR, immediately after the tag is pushed.

**Skipping this does not break anything loudly.** `release-snapshot.yaml` guards its publish step with `if: contains(steps.version.outputs.version, '-SNAPSHOT')`, so while `main` still carries the released version the job is skipped and the workflow stays green. No SNAPSHOT reaches Maven Central, and nothing reports it.

The window opens the moment the release PR lands on `main`, not when the tag is pushed. To check the current state at any time, run the commands in section 3 against `main` and confirm every value ends in `-SNAPSHOT`.

## 7. Dependabot

Dependabot is configured only for the `docker` ecosystem, and only for `main`: it proposes updates of the base images pinned in the Dockerfiles. No release branch receives Dependabot updates; base image updates reach a release branch by backporting the change merged into `main`. If a release branch ever needs its own updates, it needs its own entry on `main`: Dependabot reads `.github/dependabot.yml` only on the default branch.

## 8. What `release.yaml` does

Triggered by pushing a tag matching `v[0-9]+.[0-9]+.[0-9]+`:

| Job | Output |
|-----|--------|
| `verify-version` | Fails unless the tag equals `v` + `VERSION`. Every publishing job waits for it |
| `release-to-maven-central` | The Spark assembly `scalardb-analytics-spark-all-<spark>_<scala>` via `mill sparkAll.__.publishSonatypeCentral` |
| `release-to-ghcr` | The CLI and server images to GHCR |
| `create-github-release` | GitHub release with the changelog section as notes, the CLI fat JAR, and the generated `scalardb-analytics-status-codes.mdx` |

The workflows need `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `MAVEN_CENTRAL_GPG_PASSPHRASE`, `MAVEN_CENTRAL_GPG_SECRET_KEY` and `CR_PAT` on this repository.

One precondition is checked late: `CHANGELOG.md` must contain a non-empty `## [<tag>]` section — an unprefixed heading such as `## [3.19.0]` will not be found. The check runs in `create-github-release`, after Maven Central and GHCR have published, so a missing or empty section fails the release only once the artifacts are out. Check the heading yourself before tagging.
