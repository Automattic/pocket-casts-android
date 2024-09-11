#!/bin/bash -u

echo "--- 🧹 Linting"
./gradlew lintDebug
lint_exit_code=$?

find . -type f -path "**/build/reports/lint-results*.sarif" -exec upload_sarif_to_github '{}' 'Automattic' 'pocket-casts-android' \;

exit $lint_exit_code
