#!/bin/bash -eu

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"

install_gems

echo "--- :closed_lock_with_key: Installing Secrets"

bundle exec fastlane run configure_apply

echo "--- ⚙️ Building release variant"

./gradlew "$1:assembleRelease" -PskipSentryProguardMappingUpload=true
