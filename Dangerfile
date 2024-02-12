# frozen_string_literal: true

github.dismiss_out_of_range_messages

# `files: []` forces rubocop to scan all files, not just the ones modified in the PR
rubocop.lint(files: [], force_exclusion: true, inline_comment: true, fail_on_inline_comment: true, include_cop_names: true)

manifest_pr_checker.check_gemfile_lock_updated

# skip remaining checks if we're during the release process
if github.pr_labels.include?('releases')
  message('This PR has the `releases` label: some checks will be skipped.')
  return
end

labels_checker.check(
  do_not_merge_labels: ['do not merge'],
  required_labels: [//],
  required_labels_error: 'PR requires at least one label.'
)

view_changes_checker.check

pr_size_checker.check_diff_size(max_size: 500)

android_unit_test_checker.check_missing_tests

milestone_checker.check_milestone_due_date(days_before_due: 2)

warn('PR is classed as Work in Progress') if github_utils.wip_feature?

unless github_utils.requested_reviewers? || github.pr_draft?
  warn("No reviewers have been set for this PR yet. Please request a review from **@\u2060Automattic/pocket-casts-android**.")
end
