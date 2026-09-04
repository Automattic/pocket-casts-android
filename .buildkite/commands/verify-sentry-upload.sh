#!/bin/bash -eu

set -o pipefail

# Throwaway: proves the Sentry mapping upload works with the auth token coming from the
# environment rather than `secret.properties`. No other CI job uploads, because they all
# pass `-PskipSentryProguardMappingUpload=true`.
# Delete this script and its pipeline step once AINFRA-2790 is verified.

GUARD_MESSAGE="SENTRY_AUTH_TOKEN is not set (or is blank)"
MODULES="app automotive wear"

# The keys mobile-secrets dropped in 789d0d6. The DSNs and `sentryTvProject` stay.
REMOVED_KEYS="sentryAuthToken sentryOrg sentryAndroidProject sentryAutomotiveProject sentryWearProject"

fail() {
  echo "^^^ +++"
  echo "$1"
  exit 1
}

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"

install_gems

echo "--- :closed_lock_with_key: Installing Secrets"

bundle exec fastlane run configure_apply

echo "--- :mag: Checking the provisioned secret.properties"

leftovers=""
for key in $REMOVED_KEYS; do
  if grep -qi "^${key}[[:space:]]*=" secret.properties; then
    leftovers="$leftovers $key"
  fi
done

if [ -n "$leftovers" ]; then
  fail "secret.properties still defines:$leftovers — .configure is not on the cleaned-up hash"
fi
echo "None of the removed Sentry keys are present, as expected"

if [ -n "${SENTRY_AUTH_TOKEN:-}" ]; then
  echo "SENTRY_AUTH_TOKEN is present in the environment"
else
  echo "SENTRY_AUTH_TOKEN is absent from the environment"
fi

# Minifies with R8 to produce each mapping, then uploads it. Deliberately not `assembleRelease`:
# packaging and signing are not needed. All three modules run because each declares its own
# Sentry project slug, and the token has to authenticate against all of them.
echo "--- :sentry: Uploading ProGuard mappings"

upload_log="$(pwd)/sentry-upload.log"

./gradlew --no-daemon --console=plain \
  :app:uploadSentryProguardMappingsRelease \
  :automotive:uploadSentryProguardMappingsRelease \
  :wear:uploadSentryProguardMappingsRelease 2>&1 | tee "$upload_log"

echo "--- :white_check_mark: Checking the uploads actually ran"

# A task served from the cache asserts nothing, and looks exactly like one that worked.
for module in $MODULES; do
  task=":${module}:uploadSentryProguardMappingsRelease"
  if ! grep -qE "^> Task ${task}\$" "$upload_log"; then
    outcome=$(grep -E "^> Task ${task}( |\$)" "$upload_log" || echo "(absent from the build output)")
    fail "$task did not execute: $outcome"
  fi
  echo "$task executed"
done

# `--rerun` forces the upload task to execute again; without it the run above leaves it
# up-to-date, `doFirst` never fires, and a passing build would prove nothing. It is a task
# option, so it only parses after the task name.
# `--no-daemon` keeps `providers.environmentVariable` off a daemon that read the real token.
echo "--- :no_entry: Checking the guard fails the build on a blank token"

guard_log="$(pwd)/sentry-guard.log"

set +e
SENTRY_AUTH_TOKEN='' ./gradlew --no-daemon --console=plain \
  :app:uploadSentryProguardMappingsRelease --rerun 2>&1 | tee "$guard_log"
guard_status=${PIPESTATUS[0]}
set -e

if [ "$guard_status" -eq 0 ]; then
  fail "Expected a blank SENTRY_AUTH_TOKEN to fail the build, but it succeeded"
fi

if ! grep -qF "$GUARD_MESSAGE" "$guard_log"; then
  fail "The build failed, but not through the guard: '$GUARD_MESSAGE' is absent from the output"
fi

echo "Guard fired as expected"
