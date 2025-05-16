#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type validation; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

BUILD_VARIANT=$1

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 💾 Diff Merged Manifest (Module: app, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "app" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: wear, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "wear" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: automotive, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "automotive" ${BUILD_VARIANT}
