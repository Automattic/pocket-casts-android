#!/bin/bash -eu

.buildkite/commands/restore-cache.sh

echo "--- 🧪 Testing"

./gradlew testDebugUnitTest
