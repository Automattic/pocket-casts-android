#!/usr/bin/env bash

set -euo pipefail

# Print the previous main tip for a multi-commit GitHub push. For a single commit, keep the
# regular first-parent rule. Failure tells the upload to run every job.
head=$(git rev-parse HEAD)
before=$(buildkite-agent meta-data get buildkite:webhook | jq -er --arg head "$head" '
  select(.ref == "refs/heads/main" and .after == $head and .forced == false)
  | .before | select(test("^[0-9a-f]{40}$"))
')
git merge-base --is-ancestor "$before" "$head"
[[ "$before" != "$head" ]]

# A previous tip with a Buildkite skip marker never got its own build.
message=$(git log -1 --format=%B "$before")
if grep -qiE '\[(ci[ -]skip|skip[ -]ci)\]' <<< "$message"; then
  exit 1
fi
first_parent=$(git rev-parse --verify HEAD^1)
if [[ "$before" != "$first_parent" ]]; then
  echo "$before"
fi
