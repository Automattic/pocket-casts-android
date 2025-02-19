#!/bin/bash -eu

# Script to checkout a specific release branch
# Usage: ./checkout-release-branch.sh <RELEASE_VERSION>

# Buildkite, by default, checks out a specific commit, ending up in a detached HEAD state.
# But in some cases, we need to ensure to be checked out on the `release/*` branch instead, namely:
# - When a `release-pipelines/*.yml` will end up needing to do a `git push` to the `release/*` branch (for version bumps)
# - When doing a new `release-build.sh` from a job that was `pipeline upload`'d by such a pipeline,
#   to ensure that the job doing the build would include that recent extra commit before starting doing the build.

echo "--- :git: Checkout Release Branch"

if [[ -n "${1:-}" ]]; then
  RELEASE_VERSION="$1"
elif [[ "${BUILDKITE_BRANCH:-}" =~ ^release/ ]]; then
  RELEASE_VERSION="${BUILDKITE_BRANCH#release/}"
else
  echo "Error: RELEASE_VERSION parameter missing and BUILDKITE_BRANCH is not a release branch"
  exit 1
fi
BRANCH_NAME="release/${RELEASE_VERSION}"

git fetch origin "$BRANCH_NAME"
git checkout "$BRANCH_NAME"
git pull
