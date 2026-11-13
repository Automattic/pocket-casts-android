#!/usr/bin/env bash

set -euo pipefail

EXPECTED_TRACK=beta
EXPECTED_PERCENT=0.42

echo '--- Runtime env, as the shell sees it'
for var in TRACK ROLLOUT_PERCENT RELEASE_VERSION MILESTONE; do
  echo "$var=${!var-<unset>}"
done

stubs=$(mktemp -d)
calls="$stubs/calls.log"
for tool in install_gems bundle; do
  printf '#!/usr/bin/env bash\necho "%s $*" >> "%s"\n' "$tool" "$calls" > "$stubs/$tool"
  chmod +x "$stubs/$tool"
done

failures=0
fail() {
  echo "FAIL  $*"
  failures=$((failures + 1))
}

echo '--- Positive: the real update-rollouts.sh, with install_gems and bundle stubbed'
if PATH="$stubs:$PATH" .buildkite/commands/update-rollouts.sh; then
  echo 'PASS  update-rollouts.sh exited 0'
else
  fail 'update-rollouts.sh exited non-zero with the build-level env in place'
fi
cat "$calls" 2>/dev/null || true
expected_call="bundle exec fastlane update_rollouts track:$EXPECTED_TRACK percent:$EXPECTED_PERCENT version: milestone:"
if grep -qxF "$expected_call" "$calls" 2>/dev/null; then
  echo "PASS  fastlane received the build-level values: $expected_call"
else
  fail "fastlane did not receive: $expected_call"
fi

echo '--- Negative control: same script with TRACK removed must stop at its guard'
: > "$calls"
set +e
output=$(env -u TRACK PATH="$stubs:$PATH" .buildkite/commands/update-rollouts.sh 2>&1)
status=$?
set -e
echo "$output"
if [[ $status -ne 0 ]] && grep -qF 'missing or empty environment variables: TRACK' <<< "$output" && [[ ! -s "$calls" ]]; then
  echo 'PASS  guard fired on the missing TRACK before any tool ran'
else
  fail "guard did not fire as expected (exit $status)"
fi

if [[ $failures -gt 0 ]]; then
  echo "+++ $failures assertion(s) failed"
  exit 1
fi
echo '+++ All assertions passed'
