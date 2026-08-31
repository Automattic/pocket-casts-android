#!/usr/bin/env bash
# Push the sync branch, detect .github/workflow push limits, and open/update the PR.
set -euo pipefail

STATE_FILE="${RUNNER_TEMP}/upstream-sync/state.env"
REPORT_DIR="${RUNNER_TEMP}/upstream-sync"
AUDIT_FILE="${REPORT_DIR}/audit.env"
PR_BODY_FILE="${REPORT_DIR}/pr-body.md"
WORKFLOW_PATCH_FILE="${REPORT_DIR}/github-workflows.patch"

log() { printf '[upstream-sync-finalize] %s\n' "$*" >&2; }

# shellcheck disable=SC1090
source "$STATE_FILE"

WORKTREE_ROOT="${WORKTREE_ROOT:?}"
SYNC_TAG="${SYNC_TAG:?}"
SYNC_BRANCH="${SYNC_BRANCH:?}"
BASE_BRANCH="${BASE_BRANCH:-main}"
UPSTREAM_REPO="${UPSTREAM_REPO:?}"

PI_RESPONSE="${PI_RESPONSE:-}"
PI_SUCCESS="${PI_SUCCESS:-}"

has_unresolved_conflicts=false
github_changed=false
workflow_changed=false
workflows_deferred=false
push_succeeded=false
pr_number=""
alerts=()

read_audit() {
  if [ -f "$AUDIT_FILE" ]; then
    # shellcheck disable=SC1090
    source "$AUDIT_FILE"
  fi
  [ -s "${REPORT_DIR}/github_changed_files.txt" ] && [ "$(head -1 "${REPORT_DIR}/github_changed_files.txt")" != "" ] && github_changed=true
  [ -s "${REPORT_DIR}/workflow_changed_files.txt" ] && [ "$(head -1 "${REPORT_DIR}/workflow_changed_files.txt")" != "" ] && workflow_changed=true
}

add_alert() {
  alerts+=("$1")
}

build_pr_body() {
  local body_file="$1"
  : >"$body_file"

  if [ "${#alerts[@]}" -gt 0 ]; then
    {
      echo "## 🚨 Upstream sync needs attention"
      echo
      for alert in "${alerts[@]}"; do
        echo "> [!CAUTION]"
        echo "> ${alert}"
        echo
      done
    } >>"$body_file"
  fi

  {
    echo "## Summary"
    echo
    echo "- Upstream: [\`${UPSTREAM_REPO}\`](https://github.com/${UPSTREAM_REPO})"
    echo "- Release tag: \`${SYNC_TAG}\`"
    echo "- Sync branch: \`${SYNC_BRANCH}\`"
    echo "- Base branch: \`${BASE_BRANCH}\`"
    echo
    echo "## Status"
    echo
    echo "| Check | Result |"
    echo "| --- | --- |"
    echo "| Merge conflicts resolved | $([ "${has_unresolved_conflicts}" = true ] && echo '❌ **NO**' || echo '✅ yes') |"
    echo "| Push to origin | $([ "$push_succeeded" = true ] && echo '✅ yes' || echo '❌ **FAILED**') |"
    echo "| \`.github/workflows/\` in merge | $([ "$workflow_changed" = true ] && echo '✅ yes' || echo 'n/a') |"
    echo "| Workflow files deferred for push | $([ "$workflows_deferred" = true ] && echo '⚠️ **YES — manual apply required**' || echo 'no') |"
    echo "| Pi agent success | ${PI_SUCCESS:-n/a} |"
    echo
  } >>"$body_file"

  if [ -n "$PI_RESPONSE" ]; then
    {
      echo "## Pi agent response"
      echo
      echo '```text'
      echo "$PI_RESPONSE"
      echo '```'
      echo
    } >>"$body_file"
  fi

  if [ "$workflows_deferred" = true ] && [ -f "$WORKFLOW_PATCH_FILE" ]; then
    {
      echo "## Deferred \`.github/workflows/\` changes"
      echo
      echo "Apply this patch on \`${SYNC_BRANCH}\` after merge (or cherry-pick with a PAT that has the \`workflow\` scope):"
      echo
      echo '```diff'
      head -c 60000 "$WORKFLOW_PATCH_FILE"
      echo '```'
      if [ "$(wc -c <"$WORKFLOW_PATCH_FILE")" -gt 60000 ]; then
        echo
        echo "_Patch truncated in PR body — download the \`upstream-sync-workflows-patch\` artifact from the workflow run._"
      fi
      echo
    } >>"$body_file"
  fi

  if [ "${has_unresolved_conflicts}" = true ]; then
    {
      echo "## Unresolved conflicts"
      echo
      if [ -s "${REPORT_DIR}/unmerged_files.txt" ] && [ -n "$(grep -v '^$' "${REPORT_DIR}/unmerged_files.txt" || true)" ]; then
        echo "**Unmerged files:**"
        echo '```'
        grep -v '^$' "${REPORT_DIR}/unmerged_files.txt" || true
        echo '```'
      fi
      if [ -s "${REPORT_DIR}/conflict_marker_files.txt" ] && [ -n "$(grep -v '^$' "${REPORT_DIR}/conflict_marker_files.txt" || true)" ]; then
        echo "**Files with conflict markers:**"
        echo '```'
        grep -v '^$' "${REPORT_DIR}/conflict_marker_files.txt" || true
        echo '```'
      fi
      echo
    } >>"$body_file"
  fi

  {
    echo "---"
    echo "_Automated upstream sync via [upstream-sync workflow](${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/workflows/upstream-sync.yml)._"
  } >>"$body_file"
}

push_token() {
  # Prefer PAT so we can push .github/workflows/; fall back to Actions token.
  if [ -n "${UPSTREAM_SYNC_PAT:-}" ]; then
    printf '%s\n' "$UPSTREAM_SYNC_PAT"
  elif [ -n "${GH_TOKEN:-}" ]; then
    printf '%s\n' "$GH_TOKEN"
  else
    printf '%s\n' "${GITHUB_TOKEN:-}"
  fi
}

push_token_label() {
  if [ -n "${UPSTREAM_SYNC_PAT:-}" ]; then
    printf 'UPSTREAM_SYNC_PAT'
  elif [ -n "${GH_TOKEN:-}" ]; then
    printf 'GH_TOKEN'
  else
    printf 'GITHUB_TOKEN'
  fi
}

push_rejected_for_workflows() {
  [ -f "${REPORT_DIR}/push.err" ] && grep -qiE 'workflow|workflows permission' "${REPORT_DIR}/push.err"
}

push_branch() {
  local token
  token="$(push_token)"
  if [ -z "$token" ]; then
    log "No push token available"
    return 1
  fi

  local host="${GITHUB_SERVER_URL#https://}"
  local remote_url="https://x-access-token:${token}@${host}/${GITHUB_REPOSITORY}.git"

  # actions/checkout persists GITHUB_TOKEN in http.*.extraheader, which overrides
  # credentials embedded in the remote URL. Clear it for this push only.
  log "Pushing ${SYNC_BRANCH} with $(push_token_label)"
  mkdir -p "$REPORT_DIR"
  if ! git -C "$WORKTREE_ROOT" \
    -c "http.https://${host}/.extraheader=" \
    -c "http.https://github.com/.extraheader=" \
    push "$remote_url" "HEAD:${SYNC_BRANCH}" 2>"${REPORT_DIR}/push.err"; then
    return 1
  fi
  return 0
}

configure_git_identity() {
  git -C "$WORKTREE_ROOT" config user.name "${GIT_AUTHOR_NAME:-github-actions[bot]}"
  git -C "$WORKTREE_ROOT" config user.email "${GIT_AUTHOR_EMAIL:-github-actions[bot]@users.noreply.github.com}"
}

defer_workflow_files() {
  local patch_file="$1"
  cd "$WORKTREE_ROOT"
  configure_git_identity

  if ! git diff --name-only "origin/${BASE_BRANCH}"...HEAD -- '.github/workflows/' | grep -q .; then
    return 1
  fi

  git diff "origin/${BASE_BRANCH}" HEAD -- '.github/workflows/' >"$patch_file"
  git checkout "origin/${BASE_BRANCH}" -- '.github/workflows/'
  if ! git diff --cached --quiet; then
    git commit -m "chore(upstream): defer .github/workflows sync (token cannot push workflow files)"
    return 0
  fi
  return 1
}

use_pat_for_gh_cli() {
  # gh pr create/edit should use the same privileged token when available.
  if [ -n "${UPSTREAM_SYNC_PAT:-}" ]; then
    export GH_TOKEN="$UPSTREAM_SYNC_PAT"
  fi
}

main() {
  REPORT_DIR="$REPORT_DIR" BASE_BRANCH="$BASE_BRANCH" WORKTREE_ROOT="$WORKTREE_ROOT" \
    "$(dirname "$0")/upstream-sync-audit.sh" || true
  read_audit
  use_pat_for_gh_cli

  if [ "${has_unresolved_conflicts:-false}" = true ]; then
    has_unresolved_conflicts=true
    add_alert "Unresolved merge conflicts remain. This PR is **not** ready to merge."
  fi

  if [ "$has_unresolved_conflicts" = false ]; then
    if push_branch; then
      push_succeeded=true
      log "Pushed ${SYNC_BRANCH}"
    elif push_rejected_for_workflows && [ "$workflow_changed" = true ]; then
      log "Push rejected for workflow files — deferring .github/workflows/ and retrying"
      cat "${REPORT_DIR}/push.err" >&2 || true
      if defer_workflow_files "$WORKFLOW_PATCH_FILE"; then
        workflows_deferred=true
        add_alert "\`.github/workflows/\` changes were merged locally but **could not be pushed** (token lacks \`workflow\` permission). Workflow updates are saved as a patch below and in the workflow artifact — apply them manually, or set \`UPSTREAM_SYNC_PAT\` to a classic PAT with the \`workflow\` scope / fine-grained PAT with Workflows write."
        if push_branch; then
          push_succeeded=true
          log "Pushed ${SYNC_BRANCH} after deferring workflow files"
        else
          push_succeeded=false
          add_alert "Failed to push \`${SYNC_BRANCH}\` even after deferring workflow files. See workflow logs."
          log "Push error:"
          cat "${REPORT_DIR}/push.err" >&2 || true
        fi
      else
        push_succeeded=false
        add_alert "Push rejected for \`.github/workflows/\` changes, and deferral failed. See workflow logs."
      fi
    else
      push_succeeded=false
      add_alert "Failed to push \`${SYNC_BRANCH}\` to origin. See workflow logs for details."
      if [ -f "${REPORT_DIR}/push.err" ]; then
        log "Push error:"
        cat "${REPORT_DIR}/push.err" >&2
      fi
    fi
  else
    add_alert "Branch was **not pushed** because merge conflicts are unresolved."
  fi

  build_pr_body "$PR_BODY_FILE"

  if [ "$push_succeeded" = true ] || git ls-remote --heads origin "$SYNC_BRANCH" | grep -q .; then
    pr_number="$(gh pr list --head "$SYNC_BRANCH" --state open --json number --jq '.[0].number // empty')"
    local title="chore(upstream): merge release ${SYNC_TAG}"
    if [ -n "$pr_number" ]; then
      gh pr edit "$pr_number" --title "$title" --body-file "$PR_BODY_FILE"
      log "Updated PR #${pr_number}"
    else
      pr_number="$(gh pr create --base "$BASE_BRANCH" --head "$SYNC_BRANCH" --title "$title" --body-file "$PR_BODY_FILE" | sed -n 's/.*\/\([0-9]*\)$/\1/p')"
      log "Created PR #${pr_number}"
    fi
  else
    add_alert "No PR was opened because the sync branch is not on origin."
    build_pr_body "$PR_BODY_FILE"
    log "PR body (not published — push failed):"
    cat "$PR_BODY_FILE" >&2
  fi

  {
    echo "push_succeeded=${push_succeeded}"
    echo "workflows_deferred=${workflows_deferred}"
    echo "has_unresolved_conflicts=${has_unresolved_conflicts}"
    [ -n "$pr_number" ] && echo "pr_number=${pr_number}"
  } >>"${GITHUB_OUTPUT:-/dev/stdout}"

  if [ "$has_unresolved_conflicts" = true ] || [ "$push_succeeded" != true ]; then
    exit 1
  fi
}

main "$@"
