#!/usr/bin/env bash
# Preflight checks for upstream release sync.
# Exits 0 with proceed=false when no sync is needed; proceed=true when Pi should run.
set -euo pipefail

UPSTREAM_REPO="${UPSTREAM_REPO:?UPSTREAM_REPO is required}"
UPSTREAM_REMOTE="${UPSTREAM_REMOTE:-upstream}"
BASE_BRANCH="${BASE_BRANCH:-main}"
SYNC_BRANCH_PREFIX="${SYNC_BRANCH_PREFIX:-upstream-sync}"

log() { printf '[upstream-sync-preflight] %s\n' "$*"; }

latest_release_tag() {
  local url="https://github.com/${UPSTREAM_REPO}.git"
  git ls-remote --tags "$url" \
    | awk -F/ '{print $NF}' \
    | grep -Ev '(rc|beta|alpha|snapshot)' \
    | python3 -c '
import re, sys
def key(v):
    parts = [int(p) for p in re.split(r"[.\-]", v) if p.isdigit()]
    return tuple(parts)
tags = [line.strip() for line in sys.stdin if line.strip()]
if not tags:
    raise SystemExit("no release tags found")
print(max(tags, key=key))
'
}

tag_commit() {
  local tag="$1"
  git rev-parse "refs/tags/${tag}^{commit}" 2>/dev/null \
    || git ls-remote "https://github.com/${UPSTREAM_REPO}.git" "refs/tags/${tag}^{}" \
      | awk '{print $1}'
}

branch_for_tag() {
  printf '%s/%s' "$SYNC_BRANCH_PREFIX" "$1"
}

main() {
  git fetch origin "$BASE_BRANCH" --tags

  local tag
  tag="$(latest_release_tag)"
  log "Latest upstream release tag: ${tag}"

  local commit branch
  commit="$(tag_commit "$tag")"
  branch="$(branch_for_tag "$tag")"

  if git merge-base --is-ancestor "$commit" "origin/${BASE_BRANCH}" 2>/dev/null; then
    log "Tag ${tag} already merged into origin/${BASE_BRANCH}"
    {
      echo "proceed=false"
      echo "tag=${tag}"
      echo "branch=${branch}"
    } >> "${GITHUB_OUTPUT:-/dev/stdout}"
    exit 0
  fi

  if gh pr list --head "$branch" --state open --json number --jq 'length' | grep -qv '^0$'; then
    log "Open PR already exists for branch ${branch}"
    {
      echo "proceed=false"
      echo "tag=${tag}"
      echo "branch=${branch}"
    } >> "${GITHUB_OUTPUT:-/dev/stdout}"
    exit 0
  fi

  log "Sync needed for tag ${tag} -> branch ${branch}"
  {
    echo "proceed=true"
    echo "tag=${tag}"
    echo "branch=${branch}"
    echo "commit=${commit}"
  } >> "${GITHUB_OUTPUT:-/dev/stdout}"
}

main "$@"
