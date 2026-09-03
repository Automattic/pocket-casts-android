# frozen_string_literal: true

# The scenario submits a build for Google to review by rolling production out to 1%, so that one
# rollout announces a submission instead of a percentage.
SUBMISSION_ROLLOUT_PERCENTAGE = 1

# The rollout announcement.
#
# @param track [String] The Google Play track the rollout is for, either `beta` or `production`.
# @param percent [Float] The rollout percentage, between 0 and 1.
# @param version [String] The version the announcement is about.
# @param milestone [String, nil] The milestone to name alongside the version, when there is one.
# @param skipped_apps [Array<String>] Variants Google Play had no matching release for.
# @return [String] The slack message body to use, typically in a call to the `slack()` fastlane action
#
def rollout_announcement(track:, percent:, version:, milestone: nil, skipped_apps: [])
  version = version.to_s.strip
  subject = ["`#{version}`", milestone.to_s.strip].reject(&:empty?).join(' ')

  # Derived once so the announced percentage and the submission check cannot disagree.
  percentage = (percent * 100).round
  message = if track == 'production' && percentage == SUBMISSION_ROLLOUT_PERCENTAGE
              ":announcement: `#{version}` has been submitted to the Production track for Google to review."
            else
              ":announcement: #{subject} has started rolling out to #{percentage}% of users."
            end

  return message if skipped_apps.empty?

  "#{message}\n:warning: Not rolled out to #{skipped_apps.join(', ')} — Google Play had no matching release."
end
