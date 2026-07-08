---
name: self-approve-pr
description: Check whether a Pocket Casts Android pull request qualifies for self-approval and, if it does, label it "[Review] Self Approved" and squash-merge it. Use this whenever the user asks if they can self-approve, self-review, or merge their own PR without a human reviewer, or asks to "self-approve this PR". Pass a PR number as an argument, or run it from a branch with an open PR. Pass "check" to report the verdict without labelling or merging.
allowed-tools: Read, Bash(gh pr view *), Bash(gh pr checks *), Bash(gh pr diff *), Bash(gh api user *), Bash(gh api orgs/Automattic/teams/pocket-casts-android/*), Bash(gh api repos/Automattic/pocket-casts-android/*), Bash(git diff *), Bash(git log *)
---

# Self-Approve a Pull Request

Decide whether a PR can be self-approved by its author or needs a human reviewer. Self-approval is a trust mechanism for small, safe changes made by engineers who own this platform; every gate below exists to keep that trust cheap to extend. If all gates pass, apply the self-approved label and squash-merge. If any gate fails, stop and explain which gate failed and what to do instead.

## Identify the PR

- Arguments are `[check] [pr-number]` in either order. If a PR number was given, use it. Otherwise find the PR for the current branch with `gh pr view --json number`. If there is no PR, stop and suggest the `create-pr` skill.
- If the argument `check` was given, run every gate and report the verdict, but do not label or merge.
- All `gh` commands assume the checkout's remote; pass `--repo Automattic/pocket-casts-android` explicitly to be safe.
- Fetch the basics once: `gh pr view <num> --json number,title,author,baseRefName,isDraft,state,additions,deletions,files,labels`.
- The PR must be open and not a draft. A draft is not ready for any review, self or otherwise.

## Gates

Run all gates even if an early one fails, so the user gets the complete picture in one pass. Report each gate as PASS or FAIL with a one-line reason.

### 1. Ownership and primary platform

Self-approval only applies to your own work on your own platform. A web or iOS engineer making a drive-by Android change is exactly who the human-review rule is for.

- Get the authenticated user: `gh api user --jq .login`.
- The PR author must match that user.
- The user must be a member of the Android team: `gh api orgs/Automattic/teams/pocket-casts-android/members --jq '.[].login'`. Membership of this team is the definition of "Android is your primary platform". If the membership call fails with a permissions error, fall back to asking the user to confirm Android is their primary platform.

### 2. CI checks

Run `gh pr checks <num>`. Every check must be passing. Pending checks mean the verdict is "not yet", not "no": tell the user to wait and re-run the skill. A failing check is a hard fail even if it looks unrelated to the diff (e.g. a Danger rule about labels or milestones), but look at what failed so you can tell the user the concrete next step. Skipped checks are acceptable, as is a Buildkite build reported as "passed and blocked" (a held deploy step, not a failure).

### 3. All PR feedback addressed

A self-merge must never bury a teammate's unanswered comment.

- Check reviews: `gh pr view <num> --json reviews`. Any review with state `CHANGES_REQUESTED` that has not been dismissed or superseded by a later `APPROVED` review from the same person is a hard fail. If someone already approved, self-approval is unnecessary but merging is still fine. `COMMENTED` reviews (often empty-bodied thread containers) only matter through their threads, checked next.
- Check for unresolved review threads via GraphQL:

```bash
gh api graphql -F number=<num> -f query='
  query($number: Int!) { repository(owner: "Automattic", name: "pocket-casts-android") {
    pullRequest(number: $number) {
      reviewThreads(first: 100) { nodes { isResolved comments(first: 1) { nodes { author { login } body } } } }
    } } }'
```

- Every thread must have `isResolved: true`. A thread whose fix is already in the diff but that nobody resolved still fails: the resolution is the signal that the feedback loop closed, so resolve it and re-run. For any unresolved thread, show the user the first comment so they know what still needs a reply or a resolution. Threads opened by bots (e.g. `dangermattic`, `claude`) still count: resolve them or address them.

### 4. No critical areas touched

Get the changed file paths from the `files` field of the earlier `gh pr view` call and match them against the areas below as path substrings (the `**` patterns just mean "anywhere under"). The change must not touch any of these areas. They are the parts of the app where a subtle bug reaches every user or corrupts data, so a second pair of eyes is always required:

- **Playback engine**: `modules/services/repositories/**/playback/`, `modules/services/repositories/**/chromecast/`, and the `modules/features/player/` module. Playback is the core of the product.
- **Database**: anything in `modules/services/model/`. Entities, DAOs, and especially Room migrations; a bad migration bricks the app on upgrade.
- **Sync and refresh**: `modules/services/repositories/**/sync/`, `modules/services/repositories/**/refresh/`, and `modules/services/servers/`. Bugs here corrupt user data across devices.
- **Payments and subscriptions**: `modules/services/payment/`, `modules/services/repositories/**/payment/`, `modules/services/repositories/**/subscription/`. Money.
- **Auth and account**: sign-in, sign-up, or token handling in `modules/features/account/`.
- **Episode downloads**: `modules/services/repositories/**/download/`. Failures here strand offline listeners.

A test-only change (`src/test/`, `src/androidTest/`) inside these areas does not count as touching them.

### 5. Low risk

This gate is a judgment call; make it honestly rather than mechanically. Read the diff (`gh pr diff <num>`) and weigh these signals, any of which push the PR toward human review:

- Large surface: more than roughly 300 changed lines or 15 files (use the `additions`, `deletions`, and `files` fields).
- `AndroidManifest.xml` changes, new permissions, ProGuard/R8 rule changes, or release build configuration.
- `FeatureFlag` changes that alter behaviour: a new or removed flag, or a changed `defaultValue` or remote-config gating, since these control rollout. Pure metadata edits (a flag's display title, say) are fine.
- Base branch is not `main` (release-branch PRs carry release risk by definition).
- Anything you read in the diff that you would not be confident shipping without another opinion. Say so plainly if that is the case.

Small, well-tested changes pass: string tweaks, UI polish in non-critical features, docs, test additions, lint fixes, analytics events, refactors contained to one non-critical module.

## Verdict

Present a short table of the five gates with PASS/FAIL and reasons, then the verdict.

**If any gate failed**: say the PR needs a human reviewer (or another action, e.g. wait for CI, reply to a thread) and stop. Do not label or merge.

**If all gates passed and `check` mode is off**:

1. Confirm with the user before acting: "All gates passed. Label and squash-merge PR #<num>?"
2. Apply the label: `gh pr edit <num> --add-label "[Review] Self Approved"`.
3. Merge with `gh pr merge <num> --squash`. The repository only allows squash merges.
4. Confirm the merge succeeded and report the merged commit.
