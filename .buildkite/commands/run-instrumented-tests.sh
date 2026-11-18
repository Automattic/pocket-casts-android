#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type validation; then
  exit 0
fi

# TODO: Temporary diagnostics for https://github.com/Automattic/pocket-casts-android/pull/6407. Revert before merging.
#
# GitHub shows only this step's exit status, so when the step fails, it exits with a code that says where:
#   50-52    Setup failed: 50 restoring the dependency cache, 51 installing gems, 52 installing secrets.
#   39       The lane failed before its Gradle build finished.
#   11-15    The Gradle build failed. Rebuilt from scratch without the build cache, the build and the tests passed.
#   21-25    The Gradle build failed, and the rebuild from scratch without the build cache failed too. When that
#            rebuild failed on Dagger, Hilt or KSP, a third build, one task at a time, failed as well.
#   31-35    The Gradle build failed. The rebuild without the build cache failed on Dagger, Hilt or KSP, and the
#            third build, one task at a time, passed. That points at KSP's race between parallel tasks.
#            For 11-35, the last digit is the first build's error: 1 Dagger, Hilt or KSP, 2 another compilation
#            error, 3 out of memory or a lost Gradle daemon, 4 downloading dependencies, 5 something else.
#            11 and 12 point at a broken entry in the remote build cache. 13-15 can also be one-off failures.
#   18       The rebuild passed, then the lane failed before its build finished.
#   19       The rebuild passed, then the build failed again in the lane.
#   60-79    The build passed, and the Firebase Test Lab stage failed. The code is 60 plus a reason below.
#   100-119  The build failed, the rebuild passed, and the Firebase Test Lab stage failed. 100 plus a reason:
#              0 no test run was created, 1 tests failed, 2 Firebase rejected the APKs or the app didn't install,
#              3 the app crashed, 4 the tests timed out, 5 Firebase couldn't finish the run (infrastructure
#              failure, inconclusive or aborted), 6 something failed before gcloud ran, 7 the tests passed and the
#              step failed afterwards, 8 something else, 9 a Firebase project problem (quota, billing, permission
#              or a disabled API), 10 uploading the APKs failed, 11 a Test Lab request failed, 12 the device or
#              Android version isn't available, 13 no tests ran, 14 signing in to Google Cloud failed,
#              15 a gcloud lookup failed (fastlane hides its output), 16 the APKs weren't where the lane looks.
#   Any other code, including 1, means the step failed outside these checks, or the checks themselves broke.

finish() {
  echo "+++ Diagnostics: $2. Exiting with $1."
  exit "$1"
}

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh" || finish 50 "Restoring the dependency cache failed"

echo "--- :rubygems: Setting up Gems"

install_gems || finish 51 "Installing gems failed"

echo "--- :closed_lock_with_key: Installing Secrets"

bundle exec fastlane run configure_apply || finish 52 "Installing secrets failed"

lane_log="$(mktemp)"
lane_plain="$(mktemp)"
rebuild_log="$(mktemp)"
rebuild_plain="$(mktemp)"
firebase_plain="$(mktemp)"
firebase_log="build/instrumented-tests/output.log"

esc="$(printf '\033')"

# Writes a log without color codes. Fastlane also starts each line of Gradle's output with "▸ ", so patterns don't
# anchor to the start of a line. grep -a keeps a stray binary byte from hiding matches.
strip_colors() {
  sed -E "s/${esc}\[[0-9;]*[A-Za-z]//g" "$1" > "$2"
}

run_lane() {
  rm -f "$firebase_log"
  set +e
  bundle exec fastlane build_and_instrumented_test 2>&1 | tee "$lane_log"
  local status=${PIPESTATUS[0]}
  set -e
  strip_colors "$lane_log" "$lane_plain"
  return "$status"
}

build_failed() {
  grep -aqE 'BUILD FAILED|FAILURE: Build (failed|completed with)' "$lane_plain"
}

build_passed() {
  grep -aq 'BUILD SUCCESSFUL' "$lane_plain"
}

build_error_pattern='(^|[^[:alnum:]])e: |error:|symbol:|cannot access|Compilation (error|failed)'
oom_pattern='OutOfMemoryError|Java heap space|GC overhead limit exceeded|daemon disappeared unexpectedly'
dependency_pattern='Could not (resolve|download|GET|HEAD)'

# Prints 1 Dagger, Hilt or KSP, 2 another compilation error, 3 out of memory, 4 dependencies, 5 something else
build_error_kind() {
  # Only error lines, since warnings can mention KSP or Hilt too
  local errors
  errors="$(grep -aE "$build_error_pattern|$oom_pattern|$dependency_pattern" "$1" || true)"
  if grep -aqE 'Dagger/|MissingBinding|\[Hilt\]|\[ksp\]|Hilt_' <<< "$errors"; then
    echo 1
  elif grep -aqE "$oom_pattern" <<< "$errors"; then
    echo 3
  elif grep -aqE "$build_error_pattern" <<< "$errors"; then
    echo 2
  elif grep -aqE "$dependency_pattern" <<< "$errors"; then
    echo 4
  else
    echo 5
  fi
}

# Why the lane stopped before gcloud wrote its log, from release-toolkit 15.3.0's messages
setup_failure_reason() {
  local signed_out='No Google Cloud Key file found|You must be logged in to Firebase|You must call `firebase_login`'
  signed_out+='|You are not logged into Firebase'
  if grep -aqE "$signed_out" "$lane_plain" ||
    grep -a 'activate-service-account' "$lane_plain" | grep -a 'instead of 0' > /dev/null; then
    echo 14
  elif grep -aqE 'Invalid application APK|Invalid test APK|Unable to find apk' "$lane_plain"; then
    echo 16
  elif grep -aqE 'Invalid (Model|Version|Locale|Orientation)' "$lane_plain"; then
    echo 12
  elif grep -aq 'Shell command exited with exit status' "$lane_plain"; then
    echo 15
  else
    echo 6
  fi
}

# Messages from gcloud's Firebase Test Lab commands
sign_in_errors='invalid_grant|Invalid JWT|refreshing your current auth tokens|could not be refreshed|UNAUTHENTICATED'
sign_in_errors+='|(Error|ResponseError) 401|[Rr]eauthentication|active account selected'
project_errors='TEST_QUOTA_EXCEEDED|Not enough test quota|SERVICE_NOT_ACTIVATED|cloud service api is not activated'
project_errors+='|UNKNOWN_PERMISSION_ERROR|unknown permission issue|[Bb]illing|PERMISSION_DENIED|[Pp]ermission denied'
project_errors+='|does not have permission|RESOURCE_EXHAUSTED|[Qq]uota|(Error|ResponseError) (403|429)'
project_errors+='|has not been used in project'
device_errors='is not a valid (model|OS version|locale)|not compatible|ncompatible|UNSUPPORTED_ENVIRONMENT'
device_errors+='|not supported|no longer (available|supported)|deprecated'
upload_errors='Could not copy \[|Cloud storage upload failure|Failed to create bucket|Could not access bucket'
upload_errors+='|not found or not accessible'

firebase_failure_reason() {
  if [[ ! -f "$firebase_log" ]]; then
    setup_failure_reason
    return
  fi
  strip_colors "$firebase_log" "$firebase_plain"
  local errors rows
  # gcloud stops at its first error, so everything from the first ERROR line on describes it
  errors="$(sed -n '/^ERROR:/,$p' "$firebase_plain")"
  # The results table has a row per device, like "│ Failed │ Pixel2.arm-30-en-portrait │ 3 test cases failed │"
  rows="$(grep -aE -- '-portrait' "$firebase_plain" | grep -aE '│|[|]' || true)"
  if grep -aqE "$sign_in_errors" <<< "$errors"; then
    echo 14
  elif grep -aqE "$project_errors" <<< "$errors"; then
    echo 9
  elif grep -aqE "$device_errors" <<< "$errors"; then
    echo 12
  elif grep -aqE "$upload_errors" <<< "$errors"; then
    echo 10
  elif grep -aqiE 'infrastructure failure|while monitoring test run' <<< "$errors"; then
    echo 5
  elif grep -aqE 'Http error|ResponseError [0-9]+|HttpError' <<< "$errors"; then
    echo 11
  elif grep -aqE 'failed during validation|INVALID' <<< "$errors"; then
    echo 2
  elif ! grep -aq 'gs://' "$firebase_plain"; then
    echo 0
  elif grep -aqE 'Skipped|ncompatible|does not support' <<< "$rows"; then
    echo 12
  elif grep -aq 'Application crashed' <<< "$rows" ||
    { grep -aq 'Failed' <<< "$rows" && grep -aq 'Native crash' <<< "$rows"; }; then
    echo 3
  elif grep -aq 'Test timed out' <<< "$rows"; then
    echo 4
  elif grep -aq 'App failed to install' <<< "$rows"; then
    echo 2
  elif grep -aqE 'Inconclusive|Infrastructure failure|aborted by user|Unknown reason' <<< "$rows"; then
    echo 5
  elif grep -aqE 'Test failed to run|(│|[|])[[:space:]]*--[[:space:]]*(│|[|])' <<< "$rows"; then
    echo 13
  elif grep -aqE 'Failed|Flaky|test cases (failed|flaky)' <<< "$rows"; then
    echo 1
  elif grep -aq 'test cases passed' <<< "$rows"; then
    echo 7
  else
    echo 8
  fi
}

# Prints what failed, for anyone who can read this log
print_summary() {
  local scans
  scans="$(grep -ahoE 'https://gradle\.com/s/[a-z0-9]+' "$lane_plain" "$rebuild_plain" | sort -u | tr '\n' ' ' || true)"
  echo "Build scans: $scans"
  echo "Build errors:"
  grep -aE "$build_error_pattern|$oom_pattern|$dependency_pattern" "$lane_plain" | head -20 || true
  if [[ -f "$firebase_log" ]]; then
    strip_colors "$firebase_log" "$firebase_plain"
    echo "Firebase:"
    grep -aE -- '^ERROR:|^Reason:|-portrait' "$firebase_plain" | head -20 || true
  fi
}

echo "--- 🧪 Testing"

if run_lane; then
  exit 0
fi

print_summary

if ! build_failed; then
  if ! build_passed; then
    finish 39 "The lane failed before its Gradle build finished"
  fi
  reason="$(firebase_failure_reason)"
  finish $((60 + reason)) "The build passed, and the Firebase Test Lab stage failed (reason $reason)"
fi

kind="$(build_error_kind "$lane_plain")"

# --rerun-tasks runs every task again, and --no-build-cache neither reads nor writes the build cache, so the rebuild
# can't load a broken entry or store a new one in the cache that other builds share.
echo "--- 🔁 Rebuilding from scratch without the build cache"
./gradlew --stop || true
set +e
./gradlew --rerun-tasks --no-build-cache :app:assembleDebug :app:assembleDebugAndroidTest 2>&1 | tee "$rebuild_log"
rebuild_status=${PIPESTATUS[0]}
set -e
strip_colors "$rebuild_log" "$rebuild_plain"
if [[ $rebuild_status -ne 0 ]]; then
  grep -aE "$build_error_pattern|$oom_pattern|$dependency_pattern" "$rebuild_plain" | head -20 || true
  rebuild_kind="$(build_error_kind "$rebuild_plain")"
  if [[ $rebuild_kind -ne 1 ]]; then
    finish $((20 + kind)) "The build failed (kind $kind), and so did the rebuild without the cache (kind $rebuild_kind)"
  fi
  # KSP tasks that overlap can generate nothing for Hilt (google/ksp#3286), so build once more one task at a time
  echo "--- 🐢 Rebuilding without the build cache, one task at a time"
  ./gradlew --stop || true
  if ! ./gradlew --rerun-tasks --no-build-cache --max-workers=1 :app:assembleDebug :app:assembleDebugAndroidTest; then
    finish $((20 + kind)) "The build failed (kind $kind), and so did both rebuilds without the cache"
  fi
  finish $((30 + kind)) "The build failed (kind $kind). Without the cache, it failed in parallel but passed serially"
fi

# Every task is now up to date, so the lane tests the APKs from the rebuild.
echo "--- 🧪 Testing the rebuilt APKs"
if run_lane; then
  finish $((10 + kind)) "The build failed (kind $kind) with the build cache. Without it, the build and the tests passed"
fi

print_summary

if build_failed; then
  finish 19 "The rebuild passed, then the build failed again in the lane"
fi

if ! build_passed; then
  finish 18 "The rebuild passed, then the lane failed before its build finished"
fi

reason="$(firebase_failure_reason)"
finish $((100 + reason)) "The build failed (kind $kind), the rebuild passed, and Firebase failed (reason $reason)"
