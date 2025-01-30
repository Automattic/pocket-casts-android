#!/bin/bash -eu
echo "--- :git: Checkout Release Branch"
# Buildkite, by default, checks out a specific commit. But given this step will be run on a CI build that will
# first push a commit to do the version bump, before `pipeline upload`-ing the job calling this script, we need
# to checkout the `release/` branch explicitly here, to ensure this job would include that extra commit
# instead of running on the initial commit the whole CI build/pipeline was initially triggered on.
RELEASE_VERSION="${1:?RELEASE_VERSION parameter missing}"
BRANCH_NAME="release/${RELEASE_VERSION}"
git fetch origin "$BRANCH_NAME"
git checkout "$BRANCH_NAME"
git pull

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Building"
bundle exec fastlane build_and_upload_to_play_store skip_confirm:true skip_prechecks:true create_gh_release:true
