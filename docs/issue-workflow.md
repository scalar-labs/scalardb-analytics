# Issue workflow

## Issues

An Issue records work that has been decided on, with its background, goal, open questions, and discussion. Use the **Bug report** template for bugs and the **Issue** template for everything else; leave its Open questions empty when nothing is undecided.

A goal that takes more than one pull request is a parent Issue with sub-issues, each about one pull request in size; smaller steps stay inside the pull request. Work that takes one pull request is a single Issue. A change that needs its own changelog entry, such as a fix to released behavior or a breaking change, is a separate Issue, not a sub-issue.

## Labels

| Group | Color | Labels |
| --- | --- | --- |
| Type | Green | `enhancement` (Enhancements), `improvement` (Improvements), `bugfix` (Bug fixes) |
| Breaking change | Red | `backward-incompatible` |
| Component | Blue | Labels whose description starts with `Component: ` |

- A type label names the changelog section of the Issue's entry. No type label means no changelog entry, as for internal refactoring, CI, tests, or dependency updates.
- `backward-incompatible` is combined with a type label. Prefix its changelog entry with `[BREAKING]`.
- The component labels set in this repository are the list of components, and each name is that component's Conventional Commit scope. To add a component, add a label with a `Component: ` description. Documentation, build, and CI are commit types, not components, and take the scope of the component they concern, if any.

  ```bash
  gh label list --json name,description --jq '.[] | select(.description | startswith("Component: ")) | .name'
  ```

| Issue | Type label | Component labels | Milestone |
| --- | --- | --- | --- |
| Single Issue | Yes | Yes | When scheduled |
| Parent Issue | The goal's main type | Only when the goal concerns one component | When scheduled |
| Sub-issue | No | Yes | No |

## Milestones

The one open milestone, `Next release`, holds the Issues scheduled for the next release. An Issue without a milestone is not scheduled, and an assignee marks that work has started. The Issues that need attention now are:

```text
is:open no:parent-issue milestone:"Next release"
```

### At each release

Rename the milestone to the release's tag, move its open Issues to a new `Next release`, and close the renamed one. Patch releases from a `release/X.Y` branch leave it alone.

```bash
tag=vX.Y.Z
number=$(gh api 'repos/{owner}/{repo}/milestones?state=open' --jq '.[] | select(.title == "Next release") | .number')
gh api -X PATCH "repos/{owner}/{repo}/milestones/$number" -f title="$tag"
gh api 'repos/{owner}/{repo}/milestones' -f title='Next release'
gh issue list --milestone "$tag" --state open --limit 500 --json number --jq '.[].number' |
  xargs -I{} gh issue edit {} --milestone 'Next release'
gh api -X PATCH "repos/{owner}/{repo}/milestones/$number" -f state=closed
```

## Commits and pull requests

Commit messages and pull request titles follow Conventional Commits, `type(scope): description`, with the types `feat`, `fix`, `refactor`, `build`, `ci`, `docs`, `test`, and `chore`. The scope is one component label's name, omitted when the change spans components or concerns none.

## Changelog

`CHANGELOG.md` has one entry per goal, that is, per single or parent Issue, in the `[Unreleased]` section that its type label names.

- The first pull request that makes the goal visible to users writes the entry; later ones revise it instead of adding another.
- A sub-issue's pull request may add an indented item under the entry for something users touch directly, such as a new option or a newly supported data source.
- Changes with no user-facing effect, and regressions in unreleased changes, get no entry.
