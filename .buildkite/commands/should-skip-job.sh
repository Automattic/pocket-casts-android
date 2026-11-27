#!/bin/bash -eu

# Usage: should-skip-job.sh <job>
# Prints the reason to skip the job, or `false` to run it. Exits 0 either way, and 15 for invalid arguments.
# `shared-pipeline-vars` exports the result as SKIP_<JOB TYPE>, used as the step's `skip:` (`false` runs the step).
# Webhook PR and main builds can skip jobs. API and trigger builds of main can supply CI_CHANGED_FILES_BASE
# to check all changes since that ancestor. Manual UI, scheduled, tagged and POCKET_CASTS_CI_FULL_BUILD=1 builds run.

run_job() { echo false; exit 0; }
skip_job() { echo "$1"; exit 0; }

job="${1:-}"
case "$job" in
  validation|unit-tests|instrumented-tests|lint|app-build|automotive-build|wear-build|tv-build) ;;
  *) echo "Unknown job type: $job" >&2; exit 15 ;;
esac
if [[ "${POCKET_CASTS_CI_FULL_BUILD:-}" == 1 || -n "${BUILDKITE_TAG:-}" ]]; then
  run_job
fi
case "${BUILDKITE_SOURCE:-}" in
  webhook) ;;
  api|trigger_job)
    [[ "${BUILDKITE_PULL_REQUEST:-}" == false && -n "${CI_CHANGED_FILES_BASE:-}" ]] || run_job
    ;;
  *) run_job ;;
esac

if [[ "${BUILDKITE_PULL_REQUEST:-}" =~ ^[0-9]+$ ]]; then
  changed_files_command=(pr_changed_files)
elif [[ "${BUILDKITE_PULL_REQUEST:-}" == false && "${BUILDKITE_BRANCH:-}" == main ]]; then
  changed_files_command=(check_changed_files)
  if [[ -z "${CI_CHANGED_FILES_BASE:-}" ]]; then
    # GitHub squash merges usually end in "(#123)". Only a caller-supplied base opts ordinary pushes in.
    squash_subject='\(#[0-9]+\)$'
    git rev-parse --verify --quiet HEAD^2 >/dev/null || [[ "$(git log -1 --format=%s HEAD)" =~ $squash_subject ]] || run_job
  fi
  base=${CI_CHANGED_FILES_BASE:-${CI_MAIN_PUSH_BASE:-}}
  if [[ -n "$base" ]]; then
    base=$(git rev-parse --quiet --verify --end-of-options "${base}^{commit}") || run_job
    git merge-base --is-ancestor "$base" HEAD || run_job
    changed_files_command+=(--base "$base")
  fi
else
  run_job
fi

# Gradle configuration, Fastlane and gems are build inputs. The TV app also packages a .txt asset.
SKIP_PATTERNS=(
  '*.md' '*.pot' '.gitignore' 'version.properties'
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

# Empty diffs, errors and unknown paths run the job: skipping requires success.
# Stdout is the skip decision, so keep any output of the helper out of it.
"${changed_files_command[@]}" --all-match "${SKIP_PATTERNS[@]}" >&2 || run_job
skip_job "No relevant files changed"
