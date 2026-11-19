#!/bin/bash -eu

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

BUILD_VARIANT=$1

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

# The base build needs its secrets where its own .configure decrypts them, not where this branch's does.
# Copies nothing once the base branch's destinations match.
git fetch --quiet origin "$BUILDKITE_PULL_REQUEST_BASE_BRANCH"
git show "origin/$BUILDKITE_PULL_REQUEST_BASE_BRANCH:.configure" | ruby -rjson -rfileutils -e '
  destination = ->(entry) { File.expand_path(entry["destination"]) }
  head = JSON.parse(File.read(".configure"))["files_to_copy"].to_h { |entry| [entry["file"], destination[entry]] }
  JSON.parse($stdin.read)["files_to_copy"].each do |entry|
    source, target = head[entry["file"]], destination[entry]
    next if source.nil? || source == target || File.exist?(target)

    puts "Copying #{entry["file"]} to #{entry["destination"]} for the base branch build"
    FileUtils.mkdir_p(File.dirname(target))
    FileUtils.cp(source, target)
  end
'

echo "--- 💾 Diff Merged Manifest (Module: app, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "app" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: wear, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "wear" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: automotive, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "automotive" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: tv, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "tv" ${BUILD_VARIANT}
