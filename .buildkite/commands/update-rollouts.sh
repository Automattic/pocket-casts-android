#!/usr/bin/env bash

set -euo pipefail

: "${TRACK:?Missing value}"
: "${ROLLOUT_PERCENT:?Missing value}"
: "${RELEASE_VERSION:?Missing value}"

echo '--- :ruby: Setup Ruby Tools'
install_gems

echo '--- 🔐 Access Secrets'
bundle exec fastlane run configure_apply

echo '--- 🚀 Update Rollouts'
bundle exec fastlane update_rollouts track:"$TRACK" percent:"$ROLLOUT_PERCENT" version:"$RELEASE_VERSION" milestone:"${MILESTONE:-}"
