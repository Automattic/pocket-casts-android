#!/usr/bin/env bash
# Audit a sync worktree for incomplete merges and .github/ push limitations.
set -euo pipefail

WORKTREE_ROOT="${WORKTREE_ROOT:?WORKTREE_ROOT is required}"
BASE_BRANCH="${BASE_BRANCH:-main}"
REPORT_DIR="${REPORT_DIR:-${RUNNER_TEMP}/upstream-sync}"

log() { printf '[upstream-sync-audit] %s\n' "$*"; }

main() {
  mkdir -p "$REPORT_DIR"
  cd "$WORKTREE_ROOT"

  local unmerged conflict_marker_files incomplete=false
  unmerged="$(git diff --name-only --diff-filter=U 2>/dev/null || true)"
  conflict_marker_files="$(git grep -l '^<<<<<<< ' -- . ':(exclude).git' 2>/dev/null || true)"

  printf '%s\n' "$unmerged" >"$REPORT_DIR/unmerged_files.txt"
  printf '%s\n' "$conflict_marker_files" >"$REPORT_DIR/conflict_marker_files.txt"

  if [ -n "$unmerged" ] || [ -n "$conflict_marker_files" ]; then
    incomplete=true
    echo "has_unresolved_conflicts=true" >"$REPORT_DIR/audit.env"
  else
    echo "has_unresolved_conflicts=false" >"$REPORT_DIR/audit.env"
  fi

  local github_changed workflow_changed
  github_changed="$(git diff --name-only "origin/${BASE_BRANCH}"...HEAD -- '.github/' 2>/dev/null || true)"
  workflow_changed="$(git diff --name-only "origin/${BASE_BRANCH}"...HEAD -- '.github/workflows/' 2>/dev/null || true)"

  printf '%s\n' "$github_changed" >"$REPORT_DIR/github_changed_files.txt"
  printf '%s\n' "$workflow_changed" >"$REPORT_DIR/workflow_changed_files.txt"

  if [ -n "$github_changed" ]; then
    echo "github_changed=true" >>"$REPORT_DIR/audit.env"
  else
    echo "github_changed=false" >>"$REPORT_DIR/audit.env"
  fi

  if [ -n "$workflow_changed" ]; then
    echo "workflow_changed=true" >>"$REPORT_DIR/audit.env"
  else
    echo "workflow_changed=false" >>"$REPORT_DIR/audit.env"
  fi

  if [ "$incomplete" = true ]; then
    log "Audit: unresolved conflicts detected"
    exit 2
  fi

  log "Audit: merge tree is clean"
}

main "$@"
