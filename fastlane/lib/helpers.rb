# frozen_string_literal: true

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
# @return [String] The slack message body to use, typically in a call to the `slack()` fastlane action
#
def rollout_announcement(track:, percent:, fallback_version:)
  version = ENV.fetch('RELEASE_VERSION', nil).to_s.strip
  version = fallback_version if version.empty?

  # The scenario submits a build for Google to review by rolling production out to 1%, so that
  # one rollout announces a submission instead of a percentage.
  return ":announcement: `#{version}` has been submitted to the Production track for Google to review." if track == 'production' && (percent - 0.01).abs < 0.001

  milestone = ENV.fetch('MILESTONE', nil).to_s.strip
  subject = ["`#{version}`", milestone].reject(&:empty?).join(' ')

  ":announcement: #{subject} has started rolling out to #{(percent * 100).round}% of users."
end
