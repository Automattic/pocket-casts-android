#!/bin/bash -eu

if "$(dirname "${BASH_SOURCE[0]}")/should-skip-job.sh" --job-type validation; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :closed_lock_with_key: Installing Secrets"
git-conceal-unlock

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- 🧪 Testing"

bundle exec fastlane build_and_instrumented_test
