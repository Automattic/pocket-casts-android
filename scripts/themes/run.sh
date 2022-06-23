#!/usr/bin/env bash
set -e # fail on error
set -x # output commands when running them

bundle install
bundle exec ruby generate_xml.rb
bundle exec ruby generate_kotlin.rb
bundle exec ruby generate_kotlin_compose.rb