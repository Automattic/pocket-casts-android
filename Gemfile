# frozen_string_literal: true

source "https://rubygems.org"
gem 'fastlane', '~> 2'
# These lines are kept to help with testing Release Toolkit changes
# gem 'fastlane-plugin-wpmreleasetoolkit', '~> 9.1'
# gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: 'deprecate/project-root-folder-env-var'

plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)
