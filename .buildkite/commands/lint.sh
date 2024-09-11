#!/bin/bash -u

echo "--- 🧹 Linting"
./gradlew lintDebug
lint_exit_code=$?

 "**/build/reports/lint-results*.sarif" -exec
upload_sarif_to_github 'app/build/reports/lint-results-debug.sarif' 'Automattic' 'pocket-casts-android'

exit $lint_exit_code
