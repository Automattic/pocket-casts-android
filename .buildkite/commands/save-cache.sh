#!/bin/bash

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

# .buildkite/pipeline.yml -> ./gradlew assembleRelease
# .buildkite/commands/prototype-build.sh -> build_and_upload_prototype_build
# -> prototype_build_type = 'debugProd'
echo "--- 🛠 Download Mobile App Dependencies [Assemble Apps]"
./gradlew assembleDebug
echo ""

# .buildkite/commands/lint.sh -> ./gradlew :app:lintRelease + ./gradlew :automotive:lintRelease :wear:lintRelease
echo "--- 🧹 Download Lint Dependencies [Lint Apps]"
./gradlew lintDebug
echo ""

# .buildkite/pipeline.yml -> ./gradlew testDebugUnitTest
echo "--- 🧪 Download Unit Test Dependencies [Assemble Unit Tests]"
./gradlew testDebugUnitTest
echo ""

# .buildkite/pipeline.yml -> build_and_instrumented_test
# -> gradle(tasks: %w[assembleDebug assembleDebugAndroidTest])
echo "--- 🧪 Download Android Test Dependencies [Assemble Android Tests]"
./gradlew assembleDebugAndroidTest
echo ""

echo "--- 💾 Save Cache"
save_gradle_dependency_cache
