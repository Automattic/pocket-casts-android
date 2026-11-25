#!/bin/bash -eu

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"

install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- 🧪 Testing"

bundle exec fastlane build_and_instrumented_test
