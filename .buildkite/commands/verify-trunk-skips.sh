#!/usr/bin/env bash
set -euo pipefail

# Verification PR only. Uses real helpers against a tiny Git history, without building the app.
scenario=${1:?Pass docs or code}
repo=$PWD
fixture=$(mktemp -d)
trap 'rm -rf "$fixture"' EXIT
cp -R .buildkite "$fixture/.buildkite"
for file in .xcode-version .nvmrc .ruby-version ios/.xcode-version; do
  if git cat-file -e "HEAD:$file" 2>/dev/null; then
    mkdir -p "$fixture/$(dirname "$file")"
    git show "HEAD:$file" > "$fixture/$file"
  fi
done
cd "$fixture"
git init -q
git config commit.gpgsign false
git config core.hooksPath /dev/null
git config user.name 'CI skip verification'
git config user.email 'ci-probe@example.invalid'
echo base > fixture-base
git add fixture-base
git commit -qm 'Fixture base'
base=$(git rev-parse HEAD)
case "$scenario" in
  docs) path='README.md'; expected=(1 1 1 1 1 1 1 1) ;;
  code) path='build.gradle.kts'; expected=(0 0 0 0 0 0 0 0) ;;
  *) echo "Expected docs or code" >&2; exit 1 ;;
esac
mkdir -p "$(dirname "$path")"
echo changed > "$path"
git add "$path"
git commit -qm 'Fixture merge (#999999)'
export BUILDKITE_SOURCE=webhook BUILDKITE_PULL_REQUEST=false BUILDKITE_BRANCH=main
export PIPELINE=pipeline.yml
unset BUILDKITE_TAG BUILDKITE_GITHUB_EVENT CI_CHANGED_FILES_BASE POCKET_CASTS_CI_FULL_BUILD
variables=(SKIP_VALIDATION SKIP_LINT SKIP_UNIT_TESTS SKIP_INSTRUMENTED_TESTS SKIP_APP_BUILD SKIP_AUTOMOTIVE_BUILD SKIP_WEAR_BUILD SKIP_TV_BUILD)
read_vars() {
  # Start with the central uploader's bash -e options and no previous decision.
  set +u +o pipefail
  unset "${variables[@]}"
  source .buildkite/shared-pipeline-vars
  set -u -o pipefail
}
check_values() {
  local i value actual
  for ((i=0; i<${#variables[@]}; i++)); do
    value=${!variables[i]}
    actual=0
    [[ -z "$value" || "$value" == false ]] || actual=1
    [[ "$actual" == "${expected[i]}" ]] || { echo "Unexpected ${variables[i]}=$value" >&2; exit 1; }
  done
}
read_vars
check_values
selected_expected=("${expected[@]}")
export PROBE_SKIP=${!variables[0]} PROBE_SCENARIO=$scenario
export CI_CHANGED_FILES_BASE=$base
read_vars
check_values
unset CI_CHANGED_FILES_BASE

# Informational render for review; no app or release jobs are submitted.
buildkite-agent pipeline upload --dry-run --format json .buildkite/trunk-probe-production.yml > "$repo/trunk-probe-$scenario.json"
buildkite-agent artifact upload "$repo/trunk-probe-$scenario.json"

# Full builds, invalid ranges and empty diffs must all run.
expected=(0 0 0 0 0 0 0 0)
export POCKET_CASTS_CI_FULL_BUILD=1
read_vars
check_values
unset POCKET_CASTS_CI_FULL_BUILD
export CI_CHANGED_FILES_BASE=missing-ci-probe-base
read_vars
check_values
export CI_CHANGED_FILES_BASE=HEAD
read_vars
check_values
unset CI_CHANGED_FILES_BASE

# A direct push without a PR-merge subject runs fully unless an explicit base is supplied.
git commit --amend -qm 'Fixture direct push'
read_vars
check_values
export CI_CHANGED_FILES_BASE=$base
expected=("${selected_expected[@]}")
read_vars
check_values
unset CI_CHANGED_FILES_BASE

# A caller can select an explicit ancestor for an API or triggered recovery build. UI and schedules run fully.
for source in api trigger_job; do
  export BUILDKITE_SOURCE=$source
  expected=(0 0 0 0 0 0 0 0)
  read_vars
  check_values
  export CI_CHANGED_FILES_BASE=$base
  expected=("${selected_expected[@]}")
  read_vars
  check_values
  unset CI_CHANGED_FILES_BASE
done
export CI_CHANGED_FILES_BASE=$base
expected=(0 0 0 0 0 0 0 0)
for source in ui schedule; do
  export BUILDKITE_SOURCE=$source
  read_vars
  check_values
done
unset CI_CHANGED_FILES_BASE
export BUILDKITE_SOURCE=webhook

# Replay a GitHub push without touching the real webhook or its branch. The real Git diff and pipeline vars run.
(
  mkdir "$fixture/probe-bin"
  cat > "$fixture/probe-bin/buildkite-agent" <<'AGENT'
#!/bin/sh
[ "$*" = "meta-data get buildkite:webhook" ] || exit 1
printf 'read\n' >> "$PROBE_WEBHOOK_READS"
cat "$PROBE_WEBHOOK"
AGENT
  chmod +x "$fixture/probe-bin/buildkite-agent"
  export PATH="$fixture/probe-bin:$PATH" BUILDKITE_GITHUB_EVENT=push
  export PROBE_WEBHOOK="$fixture/webhook.json" PROBE_WEBHOOK_READS="$fixture/webhook-reads"
  push_payload() {
    jq -n --arg before "$1" --arg after "$(git rev-parse HEAD)" \
      '{before: $before, after: $after, ref: "refs/heads/main", forced: false}' > "$PROBE_WEBHOOK"
    : > "$PROBE_WEBHOOK_READS"
  }

  # The last commit is docs-only. Earlier code in the push must still run every job.
  mkdir -p docs
  echo changed > docs/push.md
  git add docs/push.md
  git commit -qm 'Docs (#999998)'
  push_payload "$base"
  expected=("${selected_expected[@]}")
  read_vars
  check_values
  [[ $(wc -l < "$PROBE_WEBHOOK_READS") -eq 1 ]]

  # Single-commit pushes keep the first-parent decision.
  push_payload "$(git rev-parse HEAD^1)"
  expected=(1 1 1 1 1 1 1 1)
  read_vars
  check_values

  # An unavailable payload or a base whose own build was skipped must run fully.
  : > "$PROBE_WEBHOOK"
  expected=(0 0 0 0 0 0 0 0)
  read_vars
  check_values
  git commit --amend -qm 'Docs (#999998) [skip ci]'
  previous=$(git rev-parse HEAD)
  echo changed >> docs/push.md
  git commit -qam 'Docs follow-up (#999997)'
  push_payload "$previous"
  read_vars
  check_values
)

# Use the same notifier/upload flow as the central uploader, with a harmless probe job and its own status.
cat > smoke.yml <<'YAML'
steps:
  - label: "Trunk $PROBE_SCENARIO selection"
    command: "echo 'Relevant code selected this job'"
    agents: { queue: upload }
    skip: "$PROBE_SKIP"
    notify:
      - github_commit_status:
          context: "CI trunk probe/$PROBE_SCENARIO"
        even-if-skipped: true
YAML
notify_skipped_steps smoke.yml | buildkite-agent pipeline upload
