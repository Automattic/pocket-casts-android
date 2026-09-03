# frozen_string_literal: true

require 'minitest/autorun'
require_relative '../lib/helpers'

# Tests the project-specific helpers used to build the release Slack announcements.
class FastlaneHelpersTest < Minitest::Test
  def test_rollout_announcement_omits_the_milestone_when_it_is_not_set
    with_env('RELEASE_VERSION' => '7.94', 'MILESTONE' => nil) do
      assert_equal ':announcement: `7.94` has started rolling out to 20% of users.',
                   rollout_announcement(track: 'beta', percent: 0.2, fallback_version: 'unused')
    end
  end

  def test_rollout_announcement_includes_the_milestone_when_it_is_set
    with_env('RELEASE_VERSION' => '7.94', 'MILESTONE' => '(Milestone 7.94)') do
      assert_equal ':announcement: `7.94` (Milestone 7.94) has started rolling out to 50% of users.',
                   rollout_announcement(track: 'production', percent: 0.5, fallback_version: 'unused')
    end
  end

  def test_rollout_announcement_covers_the_percentages_the_scenarios_use
    expected = {
      ['beta', 0.2] => ':announcement: `7.94` has started rolling out to 20% of users.',
      ['beta', 1.0] => ':announcement: `7.94` has started rolling out to 100% of users.',
      ['production', 0.1] => ':announcement: `7.94` has started rolling out to 10% of users.',
      ['production', 0.5] => ':announcement: `7.94` has started rolling out to 50% of users.',
      ['production', 1.0] => ':announcement: `7.94` has started rolling out to 100% of users.'
    }

    with_env('RELEASE_VERSION' => '7.94', 'MILESTONE' => nil) do
      expected.each do |(track, percent), message|
        assert_equal message, rollout_announcement(track: track, percent: percent, fallback_version: 'unused')
      end
    end
  end

  def test_rollout_announcement_reports_a_1_percent_production_rollout_as_a_submission
    with_env('RELEASE_VERSION' => '7.94', 'MILESTONE' => '(Milestone 7.94)') do
      assert_equal ':announcement: `7.94` has been submitted to the Production track for Google to review.',
                   rollout_announcement(track: 'production', percent: 0.01, fallback_version: 'unused')
    end
  end

  def test_rollout_announcement_treats_1_percent_beta_as_an_ordinary_rollout
    with_env('RELEASE_VERSION' => '7.94', 'MILESTONE' => nil) do
      assert_equal ':announcement: `7.94` has started rolling out to 1% of users.',
                   rollout_announcement(track: 'beta', percent: 0.01, fallback_version: 'unused')
    end
  end

  def test_rollout_announcement_trims_surrounding_whitespace
    with_env('RELEASE_VERSION' => "\t7.94\n", 'MILESTONE' => ' (Milestone 7.94) ') do
      assert_equal ':announcement: `7.94` (Milestone 7.94) has started rolling out to 20% of users.',
                   rollout_announcement(track: 'beta', percent: 0.2, fallback_version: 'unused')
    end
  end

  def test_rollout_announcement_falls_back_when_the_version_is_unset_or_blank
    ['', "  \n", nil].each do |release_version|
      with_env('RELEASE_VERSION' => release_version, 'MILESTONE' => nil) do
        assert_equal ':announcement: `7.94` has started rolling out to 20% of users.',
                     rollout_announcement(track: 'beta', percent: 0.2, fallback_version: '7.94')
      end
    end
  end

  def test_rollout_announcement_drops_a_blank_milestone_rather_than_padding_the_subject
    with_env('RELEASE_VERSION' => '7.94', 'MILESTONE' => "  \n") do
      assert_equal ':announcement: `7.94` has started rolling out to 20% of users.',
                   rollout_announcement(track: 'beta', percent: 0.2, fallback_version: 'unused')
    end
  end

  def test_rollout_announcement_names_the_variants_that_were_not_rolled_out
    with_env('RELEASE_VERSION' => '7.94', 'MILESTONE' => nil) do
      assert_equal <<~MESSAGE.chomp,
        :announcement: `7.94` has started rolling out to 20% of users.
        :warning: Not rolled out to automotive, wear — Google Play had no matching release.
      MESSAGE
                   rollout_announcement(track: 'beta', percent: 0.2, fallback_version: 'unused', skipped_apps: %w[automotive wear])
    end
  end

  def test_rollout_announcement_flags_skipped_variants_on_the_submission_message_too
    with_env('RELEASE_VERSION' => '7.94', 'MILESTONE' => nil) do
      assert_equal <<~MESSAGE.chomp,
        :announcement: `7.94` has been submitted to the Production track for Google to review.
        :warning: Not rolled out to wear — Google Play had no matching release.
      MESSAGE
                   rollout_announcement(track: 'production', percent: 0.01, fallback_version: 'unused', skipped_apps: ['wear'])
    end
  end

  def test_rollout_announcement_keeps_the_original_wording_when_every_variant_rolled_out
    with_env('RELEASE_VERSION' => '7.94', 'MILESTONE' => nil) do
      assert_equal ':announcement: `7.94` has started rolling out to 20% of users.',
                   rollout_announcement(track: 'beta', percent: 0.2, fallback_version: 'unused', skipped_apps: [])
    end
  end

  # A percentage the message would round to 1% must take the submission branch, otherwise the
  # announced number and the submission check disagree.
  def test_rollout_announcement_ties_the_submission_check_to_the_announced_percentage
    with_env('RELEASE_VERSION' => '7.94', 'MILESTONE' => nil) do
      assert_equal ':announcement: `7.94` has been submitted to the Production track for Google to review.',
                   rollout_announcement(track: 'production', percent: 0.014, fallback_version: 'unused')
    end
  end

  private

  # Sets the given environment variables for the duration of the block, then puts back what was
  # there. A `nil` value means the variable is unset for the block.
  def with_env(vars)
    original = vars.keys.to_h { |key| [key, ENV.fetch(key, nil)] }
    vars.each { |key, value| value.nil? ? ENV.delete(key) : ENV[key] = value }
    yield
  ensure
    original.each { |key, value| value.nil? ? ENV.delete(key) : ENV[key] = value }
  end
end
