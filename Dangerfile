# frozen_string_literal: true

def wip_feature?
  has_wip_label = github.pr_labels.any? { |label| label.include?('WIP') }
  has_wip_title = github.pr_title.include?('WIP')

  has_wip_label || has_wip_title
end

def finished_reviews?
  repo_name = github.pr_json['base']['repo']['full_name']
  pr_number = github.pr_json['number']

  !github.api.pull_request_reviews(repo_name, pr_number).empty?
end

def requested_reviewers?
  has_requested_reviews = !github.pr_json['requested_teams'].to_a.empty? || !github.pr_json['requested_reviewers'].to_a.empty?
  has_requested_reviews || finished_reviews?
end

return if github.pr_labels.include?('Releases')

github.dismiss_out_of_range_messages

manifest_pr_checker.check_gemfile_lock_updated

labels_checker.check(
  required_labels: [//],
  required_labels_error: 'PR requires at least one label.'
)

view_changes_need_screenshots.view_changes_need_screenshots

pr_size_checker.check_diff_size

android_unit_test_checker.check_missing_tests

milestone_checker.check_milestone_due_date(days_before_due: 2)

rubocop.lint(inline_comment: true, fail_on_inline_comment: true, include_cop_names: true)

warn('PR is classed as Work in Progress') if wip_feature?

warn("No reviewers have been set for this PR yet. Please request a review from **@\u2028Automattic/pocket-casts-android**.") unless requested_reviewers?
