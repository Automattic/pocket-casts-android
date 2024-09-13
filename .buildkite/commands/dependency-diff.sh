#!/bin/bash -u

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

comment_with_dependency_diff 'app' 'releaseRuntimeClasspath' 'Automattic' 'pocket-casts-android'
