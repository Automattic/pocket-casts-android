#!/usr/bin/env bash
# Create an isolated worktree and merge the upstream release tag (including .github/).
set -euo pipefail

UPSTREAM_REPO="${UPSTREAM_REPO:?UPSTREAM_REPO is required}"
BASE_BRANCH="${BASE_BRANCH:-main}"
SYNC_TAG="${SYNC_TAG:?SYNC_TAG is required}"
SYNC_COMMIT="${SYNC_COMMIT:?SYNC_COMMIT is required}"
SYNC_BRANCH="${SYNC_BRANCH:?SYNC_BRANCH is required}"
WORKTREE_ROOT="${WORKTREE_ROOT:-${RUNNER_TEMP}/upstream-sync-worktrees/${SYNC_BRANCH}}"
STATE_FILE="${RUNNER_TEMP}/upstream-sync/state.env"

log() { printf '[upstream-sync-prepare] %s\n' "$*"; }

write_state() {
  mkdir -p "$(dirname "$STATE_FILE")"
  cat >"$STATE_FILE" <<EOF
WORKTREE_ROOT=${WORKTREE_ROOT}
SYNC_TAG=${SYNC_TAG}
SYNC_COMMIT=${SYNC_COMMIT}
SYNC_BRANCH=${SYNC_BRANCH}
BASE_BRANCH=${BASE_BRANCH}
UPSTREAM_REPO=${UPSTREAM_REPO}
EOF
}

main() {
  local repo_root
  repo_root="$(git rev-parse --show-toplevel)"
  cd "$repo_root"

  git fetch origin "$BASE_BRANCH" --tags

  if git show-ref --verify --quiet "refs/heads/${SYNC_BRANCH}"; then
    :
  elif git show-ref --verify --quiet "refs/remotes/origin/${SYNC_BRANCH}"; then
    git branch --track "$SYNC_BRANCH" "origin/${SYNC_BRANCH}"
  else
    git branch "$SYNC_BRANCH" "origin/${BASE_BRANCH}"
  fi

  if [ -d "$WORKTREE_ROOT" ]; then
    log "Removing existing worktree at ${WORKTREE_ROOT}"
    git worktree remove --force "$WORKTREE_ROOT" 2>/dev/null || rm -rf "$WORKTREE_ROOT"
  fi
  mkdir -p "$(dirname "$WORKTREE_ROOT")"
  git worktree add "$WORKTREE_ROOT" "$SYNC_BRANCH"
  cd "$WORKTREE_ROOT"

  if git remote | grep -qx upstream; then
    git remote set-url upstream "https://github.com/${UPSTREAM_REPO}.git"
  else
    git remote add upstream "https://github.com/${UPSTREAM_REPO}.git"
  fi
  git fetch upstream --tags
  git fetch origin "$BASE_BRANCH" "$SYNC_BRANCH" 2>/dev/null || git fetch origin "$BASE_BRANCH"

  local merge_conflicts=false

  if [ -f .git/MERGE_HEAD ]; then
    log "Merge already in progress"
    merge_conflicts=true
  elif git merge-base --is-ancestor "$SYNC_COMMIT" HEAD; then
    log "Upstream commit already merged into ${SYNC_BRANCH}"
  else
    log "Merging upstream ${SYNC_TAG} (${SYNC_COMMIT})"
    if ! git merge --no-edit "$SYNC_COMMIT" -m "chore(upstream): merge release ${SYNC_TAG}"; then
      merge_conflicts=true
      log "Merge has conflicts"
    fi
  fi

  write_state

  {
    echo "merge_conflicts=${merge_conflicts}"
    echo "worktree_root=${WORKTREE_ROOT}"
  } >>"${GITHUB_OUTPUT:-/dev/stdout}"
}

main "$@"
