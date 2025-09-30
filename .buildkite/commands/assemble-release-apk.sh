#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type build; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"

install_gems

echo "--- :closed_lock_with_key: Installing Secrets"

bundle exec fastlane run configure_apply

echo "--- ⚙️ Building release variant"

# ./gradlew "$1:assembleRelease" -PskipSentryProguardMappingUpload=true
# TODO: Revert this, as this only builds the .aab not the .apk
# We're just doing this temporarily to test the Sentry Mapping UUID annotation
bundle exec fastlane build_bundle app:"$1"
