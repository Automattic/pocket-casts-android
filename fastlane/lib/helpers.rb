# frozen_string_literal: true

SUBMISSION_ROLLOUT_PERCENTAGE = 1

# The rollout announcement message for Slack.
#
# @param track [String] The Google Play track the rollout is for, either `beta` or `production`.
# @param percent [Float] The rollout percentage, between 0 and 1.
# @param version [String, nil] The version the announcement is about. A blank version still posts, flagged for investigation.
# @param milestone [String, nil] The milestone to name alongside the version, when there is one.
# @param skipped_apps [Array<String>] Variants Google Play had no matching release for.
# @return [String] The slack message body to use, typically in a call to the `slack()` fastlane action
#
def rollout_announcement(track:, percent:, version:, milestone: nil, skipped_apps: [])
  version = version.to_s.strip
  version_label = version.empty? ? 'An unknown version' : "`#{version}`"
  subject = [version_label, milestone.to_s.strip].reject(&:empty?).join(' ')

  percentage = (percent * 100).round
  message = if track == 'production' && percentage == SUBMISSION_ROLLOUT_PERCENTAGE
              ":announcement: #{version_label} has been submitted to the Production track for Google to review."
            else
              ":announcement: #{subject} has started rolling out to #{percentage}% of users."
            end

  warnings = []
  warnings << ':warning: Version unavailable. This is unexpected, please investigate.' if version.empty?
  warnings << ":warning: Not rolled out to #{skipped_apps.join(', ')} — Google Play had no matching release." unless skipped_apps.empty?

  [message, *warnings].join("\n")
end
