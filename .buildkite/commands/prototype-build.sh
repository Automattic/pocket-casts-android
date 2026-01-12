#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type build; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :closed_lock_with_key: Installing Secrets"
git-conceal-unlock

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :hammer_and_wrench: Building"
bundle exec fastlane build_and_upload_prototype_build
