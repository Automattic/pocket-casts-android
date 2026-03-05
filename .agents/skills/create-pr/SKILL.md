---
name: create-pr
description: Create a pull request
disable-model-invocation: true
allowed-tools: Read, Bash(which gh), Bash(git diff *), Bash(git log *), Bash(gh pr create --repo Automattic/pocket-casts-android *), Bash(gh pr edit --repo Automattic/pocket-casts-android *), Bash(gh label list --repo Automattic/pocket-casts-android *), Bash(gh api repos/Automattic/pocket-casts-android/*)
---

# Create Pull Request

Create a draft PR against `main`.

## Context

Commits on this branch:
!`git log main..HEAD --oneline 2>/dev/null`

## Requirements

- [GitHub CLI (`gh`)](https://cli.github.com/) must be installed and authenticated.

## Steps

1. Verify `gh` is installed by running `which gh`. If it is not found, stop and tell the user to install it.
2. Read the diff with `git diff main...HEAD` to understand all changes.
3. Create the PR using `gh pr create --draft --base main` using the
template below. Fill in every section:
- **Description**: Summarize what changed and why (not just *what* files changed).
- **Fixes**: Link the Linear/GitHub issue. If unknown, ask the user.
- **Testing Instructions**: Concrete numbered steps someone can follow to verify.
- **Screenshots**: Note whether screenshots are applicable. If UI changed, ask the user to add them.
- **Checklist**: Keep as-is for the author to check off.
- **CHANGELOG**: If user-facing, remind the user to add a CHANGELOG.md entry.
4. Add one of the following labels to the PR using `gh pr edit --add-label`:
   - `[Type] Bug` — Not functioning as intended.
   - `[Type] Feature` — Adding a new feature.
   - `[Type] Enhancement` — Improve an existing feature.
   - `[Type] Tech Debt` — Involving upgrades or refactoring to maintain or enhance the codebase.
   - `[Type] Tooling` — Related to the Gradle build scripts and the setup or maintenance of the project build process.
   - `[Type] Core` — Core infrastructure changes.
   - `[Type] Other` — Issues not covered by other types, such as refactoring and documentation.
5. Add an `[Area]` label that matches the changes. Run `gh label list --search "[Area]"` to find the best match, then apply with `gh pr edit --add-label`.
6. Set the milestone to the current one (not past its due date). Run `gh api repos/Automattic/pocket-casts-android/milestones --jq '[.[] | select(.due_on) | select((.due_on | fromdateiso8601) >= now)] | sort_by(.due_on) | .[0].title'` to find it, then apply with `gh pr edit --milestone`.
7. After creating the PR, output the PR URL.

## PR Template

![[.github/pull_request_template.md]]
