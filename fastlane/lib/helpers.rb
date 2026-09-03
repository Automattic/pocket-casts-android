# frozen_string_literal: true

# The scenario submits a build for Google to review by rolling production out to 1%, so that one
# rollout announces a submission instead of a percentage.
SUBMISSION_ROLLOUT_PERCENTAGE = 1

# Wording for a rollout update, matching what the release scenario used to post by hand.
#
# `RELEASE_VERSION` and `MILESTONE` are passed by the Releases V2 Buildkite action. `MILESTONE` is
# only sent once the Releases V2 side stops posting these messages by hand, so the milestone-less
# wording is what ships until then.
#
# @param track [String] The Google Play track the rollout is for, either `beta` or `production`.
# @param percent [Float] The rollout percentage, between 0 and 1.
# @param fallback_version [String] Version to announce when `RELEASE_VERSION` is not set. Rollouts
#   usually run from `main`, whose `version.properties` has already moved on to the next version,
#   so this is only right for a run from the release branch.
# @param skipped_apps [Array<String>] Variants Google Play had no matching release for. The lane
#   treats those as non-fatal as long as one variant rolled out, so the message has to say which
#   ones did not.
# @return [String] The slack message body to use, typically in a call to the `slack()` fastlane action
#
def rollout_announcement(track:, percent:, fallback_version:, skipped_apps: [])
  version = ENV.fetch('RELEASE_VERSION', nil).to_s.strip
  version = fallback_version if version.empty?

  milestone = ENV.fetch('MILESTONE', nil).to_s.strip
  subject = ["`#{version}`", milestone].reject(&:empty?).join(' ')

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
