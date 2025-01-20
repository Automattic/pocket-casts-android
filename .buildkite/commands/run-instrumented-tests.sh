#!/bin/bash -eu

.buildkite/commands/restore-cache.sh

echo "--- :rubygems: Setting up Gems"

install_gems

echo "--- :closed_lock_with_key: Installing Secrets"

bundle exec fastlane run configure_apply

echo "--- 🧪 Testing"

bundle exec fastlane build_and_instrumented_test
