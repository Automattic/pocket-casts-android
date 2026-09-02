#!/usr/bin/env bash

set -euo pipefail

# TEMPORARY — validation only, never to be merged. See the PR description.

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :slack: Probing the Slack webhook"
log_file=$(mktemp)
bundle exec fastlane slack_webhook_probe 2>&1 | tee "$log_file"

echo "--- :white_check_mark: Asserting the invalid webhook degraded instead of failing"
if ! grep -q "Slack notification failed" "$log_file"; then
  echo "^^^ +++"
  echo "Expected notify_slack to log a failure for the deliberately invalid webhook, but it did not."
  echo "Either the invalid URL was accepted, or the rescue in notify_slack never fired."
  exit 1
fi

echo "notify_slack degraded as expected, and the job stayed green."
