# Upstream release sync

Periodic CI merges the latest upstream Pocket Casts **release tag** into `main` via an isolated git worktree and opens a PR for review. Work is performed by [pi-github-action](https://github.com/marketplace/actions/pi-github-action).

## Schedule

**Weekly — Mondays 10:00 UTC** (`0 10 * * 1`).

Upstream Pocket Casts ships minor releases on a **~14-day cadence**. Weekly checks catch each release within about a week. Preflight makes no-op runs cheap: it exits early when the latest tag is already merged or an open sync PR exists, so Pi is only invoked when there is actual work.

Trigger manually with **Actions → Upstream release sync → Run workflow**.

## How it works

1. **Preflight** (`scripts/upstream-sync-preflight.sh`) fetches upstream tags and picks the highest non-prerelease tag.
2. Skip if that tag is already an ancestor of `main`, or if `upstream-sync/<tag>` already has an open PR.
3. **[pi-github-action](https://github.com/marketplace/actions/pi-github-action)** creates branch `upstream-sync/<tag>` in a **git worktree** (never on `main`), merges the upstream tag, resolves conflicts, pushes, and opens a PR.

## Required GitHub configuration

| Name | Type | Description |
|------|------|-------------|
| `PI_PROVIDER` | secret or variable | Pi provider id (e.g. `anthropic`, `google`, `openai`) |
| `PI_MODEL` | secret or variable | Pi model id (e.g. `claude-sonnet-4-6`) |
| `PI_API_KEY` | secret | API key for the configured provider |

`GITHUB_TOKEN` is used automatically for branch push and PR creation.

## Local dry run (preflight only)

```bash
export UPSTREAM_REPO=Automattic/pocket-casts-android
export GH_TOKEN=ghp_...
./scripts/upstream-sync-preflight.sh
```
