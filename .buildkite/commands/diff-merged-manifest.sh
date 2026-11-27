#!/bin/bash -eu

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

BUILD_VARIANT=$1

echo "--- :rubygems: Setting up Gems"
install_gems

"$(dirname "${BASH_SOURCE[0]}")/install-secrets.sh"

echo "--- 💾 Diff Merged Manifest (Module: app, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "app" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: wear, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "wear" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: automotive, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "automotive" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: tv, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "tv" ${BUILD_VARIANT}
