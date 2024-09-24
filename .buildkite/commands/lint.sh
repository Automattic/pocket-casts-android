#!/bin/bash -u

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 🧹 Linting"
# Run lint on app module first, to make sure that the lint-results-debug.sarif file is always generated
./gradlew :app:lintRelease
app_lint_exit_code=$?

./gradlew :automotive:lintRelease :wear:lintRelease
automotive_wear_lint_exit_code=$?

if [ $app_lint_exit_code -ne 0 ] || [ $automotive_wear_lint_exit_code -ne 0 ]; then
  lint_exit_code=1
else
  lint_exit_code=0
fi

upload_sarif_to_github 'app/build/reports/lint-results-release.sarif' 'Automattic' 'pocket-casts-android'

exit $lint_exit_code
