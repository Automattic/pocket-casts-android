#!/bin/bash -eu

# Usage: should-skip-job.sh --job-type <job>
# Returns 0 to skip, 1 to run, 15 for invalid arguments.
# Only webhook PR builds skip jobs. POCKET_CASTS_CI_FULL_BUILD=1 forces a full run.

[[ $# -eq 2 && "$1" == --job-type ]] || exit 15
job="$2"
case "$job" in
  validation|unit-tests|instrumented-tests|lint|app-build|automotive-build|wear-build|tv-build) ;;
  *) echo "Unknown job type: $job" >&2; exit 15 ;;
esac
if [[ "${POCKET_CASTS_CI_FULL_BUILD:-}" == 1 || "${BUILDKITE_SOURCE:-}" != webhook ||
      ! "${BUILDKITE_PULL_REQUEST:-}" =~ ^[0-9]+$ ]]; then
  exit 1
fi

# Preserve the existing documentation/tooling exclusions for every job.
SKIP_PATTERNS=(
  '*.md' '*.pot' '*.txt' '.gitignore'
  'config/*' 'fastlane/*' 'Gemfile' 'Gemfile.lock' 'version.properties'
  'docs/*' 'metadata/*'
)
case "$job" in
  validation|unit-tests|instrumented-tests) SKIP_PATTERNS+=('*/src/main/res/values*/strings.xml') ;;
esac
case "$job" in
  unit-tests) SKIP_PATTERNS+=('*/src/androidTest/*') ;;
  *-build|instrumented-tests)
    SKIP_PATTERNS+=('*/src/test/*')
    if [[ "$job" == instrumented-tests ]]; then
      # Firebase builds and tests only the phone app. Shared test helpers still matter.
      target=app
    else
      target="${job%-build}"
      SKIP_PATTERNS+=('*/src/androidTest/*')
    fi
    # App source trees are independent; shared modules and module build files
    # still matter, since they can affect more than one app.
    for app in app automotive wear tv; do
      [[ "$app" == "$target" ]] || SKIP_PATTERNS+=("$app/src/*")
    done
    ;;
esac

# Include both sides of renames without changing the checkout's Git config.
config_index=${GIT_CONFIG_COUNT:-0}
export GIT_CONFIG_COUNT=$((config_index + 1))
export "GIT_CONFIG_KEY_${config_index}=diff.renames" "GIT_CONFIG_VALUE_${config_index}=false"

# Empty diffs, errors and unknown paths run the job: skipping requires success.
if pr_changed_files --all-match "${SKIP_PATTERNS[@]}"; then
  message="Skipping ${BUILDKITE_LABEL:-$job} - no relevant files changed"
  echo "$message"
  if [[ "${BUILDKITE:-}" == true ]]; then
    echo "$message" | buildkite-agent annotate --style info --context "skip-${BUILDKITE_JOB_ID:-$job}" || echo 'Could not annotate skipped job' >&2
  fi
  exit 0
fi
exit 1
