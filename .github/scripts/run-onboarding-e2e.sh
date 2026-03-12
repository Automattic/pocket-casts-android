#!/usr/bin/env bash
set -euo pipefail

DEFAULT_TEST_CLASSES="au.com.shiftyjelly.pocketcasts.account.onboarding.e2e.LogInFullAppTest,"
DEFAULT_TEST_CLASSES+="au.com.shiftyjelly.pocketcasts.account.onboarding.e2e.OnboardingFullAppTest"
TEST_CLASSES="${ONBOARDING_E2E_TEST_CLASSES:-$DEFAULT_TEST_CLASSES}"

adb devices
adb logcat -c || true

gradle_args=(
  :app:connectedDebugAndroidTest
  "-Pandroid.testInstrumentationRunnerArguments.class=$TEST_CLASSES"
  --stacktrace
  --info
)

set +e
./gradlew "${gradle_args[@]}"
status=$?
set -e

if [ "$status" -ne 0 ]; then
  echo "Gradle failed, dumping logs..."
fi

adb logcat -d -v time > logcat.txt || true
adb shell dumpsys activity processes > dumpsys-processes.txt || true
adb shell dumpsys activity top > dumpsys-top.txt || true

exit "$status"
