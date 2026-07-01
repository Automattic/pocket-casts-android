---
name: cherry-pick-to-release
description: Cherry-pick a merged pull request's commit(s) from `main` onto the current release branch and open a draft PR.
disable-model-invocation: true
allowed-tools: Read, Bash(which gh), Bash(git fetch *), Bash(git branch *), Bash(git branch -r *), Bash(git checkout *), Bash(git switch *), Bash(git cherry-pick *), Bash(git status *), Bash(git log *), Bash(git show *), Bash(git rev-list *), Bash(git push *), Bash(gh pr view *), Bash(gh pr create --repo Automattic/pocket-casts-android *), Bash(gh pr edit --repo Automattic/pocket-casts-android *), Bash(gh pr list --repo Automattic/pocket-casts-android *)
---

# Cherry-pick a PR to the release branch

Take a merged pull request, cherry-pick its commit(s) onto a fresh branch off the **current release branch**, and open a **draft** PR targeting that release branch.

The workflow this supports: fixes are developed and merged on `main`, then cherry-picked to the release branch. That keeps the release branch limited to exactly the commits it needs, so merging it back into `main` later is clean and unambiguous (see the Git Workflow section of AGENTS.md).

## Requirements

- [GitHub CLI (`gh`)](https://cli.github.com/) installed and authenticated. Verify with `which gh`; if missing, stop and tell the user to install it.
- The PR URL (or number) to cherry-pick. If the user did not provide one, ask for it.

## Steps

### 1. Read the source PR

Run:

```bash
gh pr view <pr-url-or-number> --repo Automattic/pocket-casts-android \
  --json number,title,url,state,mergedAt,mergeCommit,headRefName,author,labels,body
```

- If `state` is not `MERGED`, warn the user: cherry-picking is meant for changes already reviewed and merged on `main`. Ask whether to continue anyway before proceeding.
- Note the `mergeCommit.oid` (the commit that landed on `main`). This is what you will cherry-pick.

### 2. Determine the current release branch

There should be only one active release branch. Fetch and find the highest version:

```bash
git fetch origin --prune
git branch -r | grep -oE 'origin/release/[0-9]+\.[0-9]+' | sed 's|origin/release/||' | sort -V | tail -1
```

This prints the latest release version (e.g. `8.15`), giving the branch `release/<ver>`. **Confirm the target branch with the user** before continuing, in case a new release branch has been cut or an older one is intended.

### 3. Identify the commit(s) to cherry-pick

The repository squash-merges PRs, so the merge commit is usually a single commit containing the whole change. Handle both squash and true merge commits by checking the parent count:

```bash
git rev-list --parents -n 1 <mergeCommit.oid>
```

- **One parent** (squash or rebase merge): cherry-pick the commit directly.
  ```bash
  git cherry-pick <mergeCommit.oid>
  ```
- **Two parents** (true merge commit): cherry-pick relative to the first parent.
  ```bash
  git cherry-pick -m 1 <mergeCommit.oid>
  ```

If `mergeCommit` is null (rare, e.g. the PR was merged in an unusual way), fall back to the PR's own commits from `gh pr view --json commits` and cherry-pick them in order, oldest first. Tell the user this is what you are doing.

### 4. Create the branch off the release branch

Branch **from the release branch**, never from `main`. Use a descriptive name that ties it to the source PR:

```bash
git checkout -b cherry-pick/<ver>/pr-<number> origin/release/<ver>
```

### 5. Cherry-pick

Run the cherry-pick command chosen in step 3.

- **On success**, verify with `git log --oneline -n 3` that the commit landed.
- **On conflict**, stop. Show the conflicting files (`git status`) and hand back to the user to resolve. Do not force or guess a resolution. Once they have staged the fixes, continue with `git cherry-pick --continue`. Resolving conflicts carefully matters here because the release branch and `main` may have diverged.

### 6. Push and open the draft PR

```bash
git push -u origin cherry-pick/<ver>/pr-<number>
```

Create a **draft** PR targeting the release branch, filling in the template below:

```bash
gh pr create --repo Automattic/pocket-casts-android --draft \
  --base release/<ver> \
  --head cherry-pick/<ver>/pr-<number> \
  --title "<original title> (cherry-pick to <ver>)" \
  --body "<filled-in body>"
```

### 7. Carry over labels and report

- Copy the `[Type]` and `[Area]` labels from the source PR onto the new one:
  ```bash
  gh pr edit <new-pr-number> --repo Automattic/pocket-casts-android --add-label "<label>"
  ```
- Output the new PR URL.

## PR body template

Fill in every field. The body must make clear this is an already-reviewed change being cherry-picked, while still asking reviewers/CI to confirm it applies cleanly, because the release branch may have diverged from `main`.

```markdown
## Description

Cherry-pick of #<original-number> into `release/<ver>`.

> **This change was already reviewed and merged on `main`.** This PR exists to land it in the upcoming release. CI and a quick review should still run to confirm the cherry-pick applies cleanly and introduces no issues on the release branch, since `release/<ver>` may have diverged from `main`.

- Original PR: <original-url>
- Cherry-picked commit: `<mergeCommit.oid short sha>`

## Testing Instructions

See the original PR (<original-url>) for full testing steps. In addition:

1. Confirm CI passes on this branch.
2. Verify the change behaves as described on top of `release/<ver>`.

## Checklist

- [ ] The cherry-pick applied without unresolved conflicts
- [ ] CI is green on this branch
```
