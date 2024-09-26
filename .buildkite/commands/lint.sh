#!/bin/bash -u

echo "--- 🧹 Linting"
# Run lint on app module first, to make sure that the lint-results-debug.sarif file is always generated
./gradlew :app:lint
app_lint_exit_code=$?

./gradlew :automotive:lint :wear:lint
automotive_wear_lint_exit_code=$?

if [ $app_lint_exit_code -ne 0 ] || [ $automotive_wear_lint_exit_code -ne 0 ]; then
  lint_exit_code=1
else
  lint_exit_code=0
fi

upload_sarif_to_github 'app/build/reports/lint-results-debug.sarif'

exit $lint_exit_code
