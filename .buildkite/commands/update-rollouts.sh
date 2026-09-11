#!/usr/bin/env bash

set -euo pipefail

REQUIRED_ENV_VARS=(TRACK ROLLOUT_PERCENT RELEASE_VERSION)

missing=()
for var in "${REQUIRED_ENV_VARS[@]}"; do
  if [[ -z "${!var:-}" ]]; then
    missing+=("$var")
  fi
done

if [[ ${#missing[@]} -gt 0 ]]; then
  echo "Error: missing or empty environment variables: ${missing[*]}" >&2
  echo "They are passed by the ReleasesV2 scenario that triggers this pipeline." >&2
  exit 1
fi

echo '--- :ruby: Setup Ruby Tools'
install_gems

echo '--- 🔐 Access Secrets'
bundle exec fastlane run configure_apply

echo '--- 🚀 Update Rollouts'
bundle exec fastlane update_rollouts \
  track:"$TRACK" \
  percent:"$ROLLOUT_PERCENT" \
  version:"$RELEASE_VERSION" \
  milestone:"${MILESTONE:-}"
