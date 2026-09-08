#!/usr/bin/env bash

set -uo pipefail

# Throwaway: proves which parameter-expansion guard survives `buildkite-agent
# pipeline upload` interpolation, on the agent that uploads the release
# pipelines. Delete with the rest of this branch.

TMP="$(mktemp -d)"

failures=0

probe_pipeline() {
  local file="$TMP/$1.yml"
  {
    printf 'steps:\n  - label: probe\n    command: |\n      '
    printf '%s\n' "$2"
  } > "$file"
  printf '%s' "$file"
}

expect_upload_fails() {
  local name="$1" file="$2" pattern="$3" out
  if out=$(buildkite-agent pipeline upload --dry-run "$file" 2>&1); then
    echo "FAIL  $name: upload succeeded, expected it to fail"
    sed 's/^/        /' <<<"$out"
    failures=$((failures + 1))
  elif ! grep -qF "$pattern" <<<"$out"; then
    echo "FAIL  $name: upload failed, but not with '$pattern'"
    sed 's/^/        /' <<<"$out"
    failures=$((failures + 1))
  else
    echo "PASS  $name: $(grep -F "$pattern" <<<"$out" | tail -1)"
  fi
}

expect_upload_yields() {
  local name="$1" file="$2" expected="$3" out
  if ! out=$(buildkite-agent pipeline upload --dry-run "$file" 2>&1); then
    echo "FAIL  $name: upload failed, expected it to succeed"
    sed 's/^/        /' <<<"$out"
    failures=$((failures + 1))
  elif ! grep -qF "$expected" <<<"$out"; then
    echo "FAIL  $name: uploaded command does not contain [$expected]"
    sed 's/^/        /' <<<"$out"
    failures=$((failures + 1))
  else
    echo "PASS  $name: $(grep -F '"command"' <<<"$out" | tail -1 | sed 's/^ *//')"
  fi
}

echo "--- :buildkite: Agent doing the interpolating"
buildkite-agent --version

echo "--- :mag: Upload-time interpolation of a command node"

export PROBE_SET=beta
export PROBE_EMPTY=
unset PROBE_UNSET || true

expect_upload_fails \
  'colon form ${VAR:?msg}, VAR set' \
  "$(probe_pipeline colon-set 'echo probe:"${PROBE_SET:?Missing value}"')" \
  'Unable to parse offset'

expect_upload_fails \
  'colon form ${VAR:?msg}, VAR unset' \
  "$(probe_pipeline colon-unset 'echo probe:"${PROBE_UNSET:?Missing value}"')" \
  'Unable to parse offset'

expect_upload_yields \
  'bare form ${VAR?msg}, VAR set' \
  "$(probe_pipeline bare-set 'echo probe:"${PROBE_SET?Missing value}"')" \
  'probe:\"beta\"'

expect_upload_fails \
  'bare form ${VAR?msg}, VAR unset' \
  "$(probe_pipeline bare-unset 'echo probe:"${PROBE_UNSET?Missing value}"')" \
  '$PROBE_UNSET: Missing value'

expect_upload_yields \
  'bare form ${VAR?msg}, VAR empty -- guard does NOT fire' \
  "$(probe_pipeline bare-empty 'echo probe:"${PROBE_EMPTY?Missing value}"')" \
  'probe:\"\"'

expect_upload_yields \
  'escaped form $${VAR:?msg} -- deferred to the job shell' \
  "$(probe_pipeline escaped 'echo probe:"$${PROBE_UNSET:?Missing value}"')" \
  'probe:\"${PROBE_UNSET:?Missing value}\"'

expect_upload_yields \
  'plain $VAR, unset -- silently empty (todays behaviour)' \
  "$(probe_pipeline plain-unset 'echo probe:"$PROBE_UNSET"')" \
  'probe:\"\"'

echo "--- :white_check_mark: Result"
if [ "$failures" -ne 0 ]; then
  echo "$failures assertion(s) failed"
  exit 1
fi
echo "all upload-time assertions held"
