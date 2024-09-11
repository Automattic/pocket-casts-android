#!/bin/bash -eu

echo "--- 🧹 Linting"
./gradlew lintDebug

find . -type f -path "**/build/reports/lint-results*.*" -exec upload_sarif_to_github '{}' 'Automattic' 'pocket-casts-android' \;
