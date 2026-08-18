#!/bin/bash -eu

# Throwaway: proves the Sentry mapping upload works with the auth token coming from the
# environment rather than `secret.properties`. No other CI job uploads, because they all
# pass `-PskipSentryProguardMappingUpload=true`.
# Delete this script and its pipeline step once AINFRA-2790 is verified.

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"

install_gems

echo "--- :closed_lock_with_key: Installing Secrets"

bundle exec fastlane run configure_apply

echo "--- :key: Checking SENTRY_AUTH_TOKEN"

if [ -n "${SENTRY_AUTH_TOKEN:-}" ]; then
  echo "SENTRY_AUTH_TOKEN is present"
else
  echo "SENTRY_AUTH_TOKEN is absent"
fi

# Minifies with R8 to produce each mapping, then uploads it. Deliberately not `assembleRelease`:
# packaging and signing are not needed. All three modules run because each declares its own
# Sentry project slug, and the token has to authenticate against all of them.
echo "--- :sentry: Uploading ProGuard mappings"

./gradlew \
  :app:uploadSentryProguardMappingsRelease \
  :automotive:uploadSentryProguardMappingsRelease \
  :wear:uploadSentryProguardMappingsRelease
