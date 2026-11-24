#!/bin/bash -eu

# Usage: should-skip-job.sh <job>
# Prints the reason to skip the job, or `false` to run it. Exits 0 either way, and 15 for invalid arguments.
# `shared-pipeline-vars` exports the result as SKIP_<JOB TYPE>, used as the step's `skip:` (`false` runs the step).
# Only webhook PR builds skip jobs. POCKET_CASTS_CI_FULL_BUILD=1 forces a full run.

run_job() { echo false; exit 0; }
skip_job() { echo "$1"; exit 0; }

job="${1:-}"
case "$job" in
  validation|unit-tests|instrumented-tests|lint|app-build|automotive-build|wear-build|tv-build) ;;
  *) echo "Unknown job type: $job" >&2; exit 15 ;;
esac
if [[ "${POCKET_CASTS_CI_FULL_BUILD:-}" == 1 || "${BUILDKITE_SOURCE:-}" != webhook ||
      ! "${BUILDKITE_PULL_REQUEST:-}" =~ ^[0-9]+$ ]]; then
  run_job
fi

# Preserve the existing documentation/tooling exclusions for every job.
SKIP_PATTERNS=(
  '*.md' '*.pot' '*.txt' '.gitignore'
  'config/*' 'fastlane/*' 'Gemfile' 'Gemfile.lock' 'version.properties'
  'docs/*' 'metadata/*'
)
LOCALIZATION_PATTERNS=('*/src/main/res/values*/strings.xml')
TEST_PATTERNS=('*/src/test/*' '*/src/androidTest/*')

# Each job lists all its exclusions here. App source trees are independent;
# shared modules and module build files can still affect more than one app.
case "$job" in
  validation)
    SKIP_PATTERNS+=("${LOCALIZATION_PATTERNS[@]}")
    ;;
  unit-tests)
    SKIP_PATTERNS+=("${LOCALIZATION_PATTERNS[@]}" '*/src/androidTest/*')
    ;;
  instrumented-tests)
    # Firebase builds and tests only the phone app. Shared test helpers still matter.
    SKIP_PATTERNS+=(
      "${LOCALIZATION_PATTERNS[@]}" '*/src/test/*'
      'automotive/src/*' 'wear/src/*' 'tv/src/*'
    )
    ;;
  app-build)
    SKIP_PATTERNS+=("${TEST_PATTERNS[@]}" 'automotive/src/*' 'wear/src/*' 'tv/src/*')
    ;;
  automotive-build)
    SKIP_PATTERNS+=("${TEST_PATTERNS[@]}" 'app/src/*' 'wear/src/*' 'tv/src/*')
    ;;
  wear-build)
    SKIP_PATTERNS+=("${TEST_PATTERNS[@]}" 'app/src/*' 'automotive/src/*' 'tv/src/*')
    ;;
  tv-build)
    SKIP_PATTERNS+=("${TEST_PATTERNS[@]}" 'app/src/*' 'automotive/src/*' 'wear/src/*')
    ;;
  lint) ;;
esac

# Include both sides of renames without changing the checkout's Git config.
config_index=${GIT_CONFIG_COUNT:-0}
export GIT_CONFIG_COUNT=$((config_index + 1))
export "GIT_CONFIG_KEY_${config_index}=diff.renames" "GIT_CONFIG_VALUE_${config_index}=false"

# Empty diffs, errors and unknown paths run the job: skipping requires success.
# Stdout is the skip decision, so keep any `pr_changed_files` output out of it.
pr_changed_files --all-match "${SKIP_PATTERNS[@]}" >&2 || run_job
skip_job "No relevant files changed"
