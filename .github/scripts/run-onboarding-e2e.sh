#!/usr/bin/env bash
set -euo pipefail

adb devices
adb logcat -c || true

set +e
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=au.com.shiftyjelly.pocketcasts.deeplink.CloudFilesDeepLinkTest \
  --stacktrace \
  --info
status=$?
set -e

if [ "$status" -ne 0 ]; then
  echo "Gradle failed, dumping logs..."
fi

adb logcat -d -v time > logcat.txt || true
adb shell dumpsys activity processes > dumpsys-processes.txt || true
adb shell dumpsys activity top > dumpsys-top.txt || true

exit "$status"
