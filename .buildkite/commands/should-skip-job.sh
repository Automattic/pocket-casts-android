#!/bin/bash -eu

# Usage: should-skip-job.sh --job-type <job>
# Returns 0 to skip, 1 to run, 15 for invalid arguments.
# Webhook PR and main builds can skip jobs. POCKET_CASTS_CI_FULL_BUILD=1 forces a full run.
# main compares HEAD to its first parent; CI_CHANGED_FILES_BASE supplies a wider range.

[[ $# -eq 2 && "$1" == --job-type ]] || exit 15
job="$2"
case "$job" in
  validation|unit-tests|instrumented-tests|lint|app-build|automotive-build|wear-build|tv-build) ;;
  *) echo "Unknown job type: $job" >&2; exit 15 ;;
esac
if [[ "${POCKET_CASTS_CI_FULL_BUILD:-}" == 1 || "${BUILDKITE_SOURCE:-}" != webhook ||
      -n "${BUILDKITE_TAG:-}" ]]; then
  exit 1
fi

if [[ "${BUILDKITE_PULL_REQUEST-false}" =~ ^[0-9]+$ ]]; then
  changed_files=(pr_changed_files)
elif [[ "${BUILDKITE_PULL_REQUEST-false}" == false && "${BUILDKITE_BRANCH:-}" == main ]]; then
  changed_files=(commit_changed_files)
  if [[ -n "${CI_CHANGED_FILES_BASE:-}" ]]; then
    changed_files+=(--base "$CI_CHANGED_FILES_BASE")
  fi
else
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

# Empty diffs, errors and unknown paths run the job: skipping requires success.
if "${changed_files[@]}" --all-match "${SKIP_PATTERNS[@]}"; then
  message="Skipping ${BUILDKITE_LABEL:-$job} - no relevant files changed"
  echo "$message"
  if [[ "${BUILDKITE:-}" == true ]]; then
    echo "$message" | buildkite-agent annotate --style info --context "skip-${BUILDKITE_JOB_ID:-$job}" || echo 'Could not annotate skipped job' >&2
  fi
  exit 0
fi
exit 1
