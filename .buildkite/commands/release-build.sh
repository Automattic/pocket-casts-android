#!/bin/bash -eu

# Ensure we get the latest commit of the `release/*` branch, especially to get last version bump commit before building the release and publishing the GitHub Release and creating the git tag
RELEASE_VERSION="${1:?RELEASE_VERSION parameter missing}"
"$(dirname "${BASH_SOURCE[0]}")/checkout-release-branch.sh" "$RELEASE_VERSION"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Building"
bundle exec fastlane build_and_upload_to_play_store skip_confirm:true skip_prechecks:true create_gh_release:true
