#!/bin/bash -eu

APP="${1:?Expected an app module: app, automotive, wear, or tv}"
GRADLE_PROPERTIES=(-PskipSentryProguardMappingUpload=true)
case "$APP" in
  app) ;;
  automotive) GRADLE_PROPERTIES+=(-PIS_AUTOMOTIVE_BUILD=true) ;;
  wear) GRADLE_PROPERTIES+=(-PIS_WEAR_BUILD=true) ;;
  tv) GRADLE_PROPERTIES+=(-PIS_TV_BUILD=true) ;;
  *) echo "Unsupported app module: $APP" >&2; exit 1 ;;
esac

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"

install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- ⚙️ Building release variant"

./gradlew "$APP:assembleRelease" "${GRADLE_PROPERTIES[@]}"
