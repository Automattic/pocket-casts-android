# frozen_string_literal: true

source 'https://rubygems.org'

gem 'danger-dangermattic', '~> 1.2.4'
gem 'fastlane', '~> 2.235'
gem 'fastlane-plugin-firebase_app_distribution', '~> 1.0'

gem 'fastlane-plugin-wpmreleasetoolkit', '~> 14.6'
# These lines are kept to help with testing Release Toolkit changes
# gem 'fastlane-plugin-wpmreleasetoolkit', path: '../../release-toolkit'
# gem 'fastlane-plugin-wpmreleasetoolkit', git: 'https://github.com/wordpress-mobile/release-toolkit', branch: ''

# Used in scripts/themes/
gem 'google-apis-sheets_v4', '~> 0.47'

# To avoid errors like:
#
# SSL_connect returned=1 errno=0 peeraddr=3.5.132.155:443 state=error: certificate verify failed (unable to get certificate CRL)
#
# See https://github.com/ruby/openssl/issues/949
gem 'openssl', '~> 4.0'

# Security: https://github.com/lostisland/faraday/pull/1665
# Faraday 2.0 is not compatible with Fastlane
gem 'faraday', '~> 1.10', '>= 1.10.5'
