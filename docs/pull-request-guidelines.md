# Pull Request Guidelines

## Main Branches

The `master` branch represents the latest version released in production.

The `develop` branch represents the cutting edge version. Usually, this is what you want to fork and base your feature branch on. This is the default GitHub branch.

## Feature Branches

Feature or fix branch names should use the pattern `issue/ISSUEID-description` where the `ISSUEID` is the Github issue number. For example, if the issue number is 1000 and the issue is related to an editor crash, an appropriate branch name would be `issue/1000-editor-crash`.

If there is no Github issue, you can use prefixes like `feature/` or `fix/`. Some examples are `feature/publishing-posts` and `fix/notifications-crash`.

## Commits

As you commit code to these branches, don’t tag the issue number in the individual commit messages as it pollutes the pull request and makes it messier. Just attach the issue number to the final pull request. Before you submit your final pull request, make sure all your branches are up to date with `develop`.

## Release Branches

Release branches are branched from `develop` as `release/x.x` while we iterate release versions. Pull requests can target specific release branches but should be limited to bug fixes or patches only, no features or new implementation. The core team will subsequently merge the release branch back to `develop` to keep it up to date.

## Version Tags

All released versions are tagged with the version number. For example, the 7.19.2 production release will be tagged as `7.19.2`.

## Raise a Pull Request

When you are ready, please, spend time crafting a good Pull Request, since it will have a huge impact on the work of reviewers, release managers and testers.

We have a [Pull Request template](.github/pull_request_template.md) to help. 

_Thank you very much for contributing to Pocket Casts Android!_