#!/bin/bash -u

echo "--- 🧹 Linting"
./gradlew :app:lint :automotive:lint :wear:lint
lint_exit_code=$?

upload_sarif_to_github 'app/build/reports/lint-results-debug.sarif' 'Automattic' 'pocket-casts-android'

exit $lint_exit_code
