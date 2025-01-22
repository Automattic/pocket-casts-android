#!/bin/bash -eu

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- 🧪 Testing"

./gradlew testDebugUnitTest
