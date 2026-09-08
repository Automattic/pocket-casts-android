#!/usr/bin/env bash

set -euo pipefail

# Throwaway: refutes the claim that `${#arr[@]}` on an empty array trips `set -u`
# on bash < 4.4. Delete with the rest of this branch.

failures=0

fail() {
  echo "FAIL  $1"
  failures=$((failures + 1))
}

echo "--- :shell: The shell this step runs under"
echo "BASH_VERSION       = $BASH_VERSION"
echo "BASH               = $BASH"
echo "command -v bash    = $(command -v bash)"
echo "/bin/bash          = $(/bin/bash --version | head -1)"

echo "--- :mag: Negative control -- which array semantics is this shell on?"
# The actual bash 4.4 change: expanding an EMPTY array under `set -u`.
if ( set -euo pipefail; a=(); for _ in "${a[@]}"; do :; done ) 2>/dev/null; then
  old_bash=0
  echo "\"\${a[@]}\" on an empty array is allowed -> bash >= 4.4 semantics"
else
  old_bash=1
  echo "\"\${a[@]}\" on an empty array is an error -> bash < 4.4 semantics"
fi

echo "--- :mag: The disputed line: \${#arr[@]} on an empty array"
# If this tripped `set -u`, the script would die here rather than print anything.
empty=()
if [[ ${#empty[@]} -gt 0 ]]; then
  fail "an empty array reported ${#empty[@]} elements"
else
  echo "PASS  \${#empty[@]} = ${#empty[@]}, guard skipped, shell still running"
fi

if [ "$old_bash" -eq 1 ]; then
  echo "      ...and this shell errors on \"\${a[@]}\", so the pre-4.4 regime is the one just exercised"
fi

echo "--- :rocket: The real update-rollouts.sh, both paths"
script_under_test="$(dirname "${BASH_SOURCE[0]}")/update-rollouts.sh"
stubs="$(mktemp -d)"
for cmd in install_gems bundle; do
  printf '#!/bin/sh\necho "[stub] %s $*"\n' "$cmd" > "$stubs/$cmd"
  chmod +x "$stubs/$cmd"
done

echo "happy path -- all required vars set:"
if out=$(PATH="$stubs:$PATH" TRACK=beta ROLLOUT_PERCENT=0.10 RELEASE_VERSION=8.20 \
  "$script_under_test" 2>&1); then
  sed 's/^/    /' <<<"$out"
  echo "PASS  exited 0 -- the guard let the happy path through"
else
  sed 's/^/    /' <<<"$out"
  fail "the happy path exited non-zero"
fi

echo "happy path, forced through /bin/bash regardless of what env bash resolves to:"
if out=$(PATH="$stubs:$PATH" TRACK=beta ROLLOUT_PERCENT=0.10 RELEASE_VERSION=8.20 \
  /bin/bash "$script_under_test" 2>&1); then
  sed 's/^/    /' <<<"$out"
  echo "PASS  exited 0 under $(/bin/bash --version | head -1)"
else
  sed 's/^/    /' <<<"$out"
  fail "the happy path exited non-zero under /bin/bash"
fi

echo "guard path -- RELEASE_VERSION unset:"
if out=$(PATH="$stubs:$PATH" TRACK=beta ROLLOUT_PERCENT=0.10 \
  "$script_under_test" 2>&1); then
  sed 's/^/    /' <<<"$out"
  fail "a missing RELEASE_VERSION did not stop the script"
elif ! grep -q 'RELEASE_VERSION' <<<"$out"; then
  sed 's/^/    /' <<<"$out"
  fail "the script stopped, but not through the guard"
else
  sed 's/^/    /' <<<"$out"
  echo "PASS  stopped through the guard, naming RELEASE_VERSION"
fi

echo "--- :white_check_mark: Result"
if [ "$failures" -ne 0 ]; then
  echo "$failures assertion(s) failed"
  exit 1
fi
echo "all assertions held"
