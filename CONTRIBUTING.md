# How to Contribute

First off, thank you for contributing! We're excited to collaborate with you! 🎉

The following is a set of guidelines for the many ways you can join our collective effort.

Before anything else, please take a moment to read our [Code of Conduct](CODE-OF-CONDUCT.md). We expect all participants, from full-timers to occasional tinkerers, to uphold it.

## Reporting Bugs, Asking Questions, and Suggesting Features

Have a suggestion or feedback? Please go to [Issues](https://github.com/automattic/pocket-casts-android/issues) and [open a new issue](https://github.com/automattic/pocket-casts-android/issues/new). Prefix the title with a category like _"Bug:"_, _"Question:"_, or _"Feature Request:"_. Screenshots help us resolve issues and answer questions faster, so thanks for including some if you can.

## Translating

We use GlotPress to manage translations. Please go to the [Pocket Casts Android GlotPress page](https://translate.wordpress.com/projects/pocket-casts/android/) for more information on how to add or edit translations.

## Beta Testing

Interested in using the upcoming versions of Pocket Casts? Do you love giving feedback on new features and don't mind reporting bugs that come up along the way? Join us in the beta-testing program by going to the [Pocket Casts Android Testing program](https://play.google.com/apps/testing/au.com.shiftyjelly.pocketcasts). Sign in with your Google account, and follow the instructions.

## Submitting Code Changes

If you're just getting started and want to familiarize yourself with the app’s code, we suggest looking at [these issues](https://github.com/automattic/pocket-casts-android/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) with the **good first issue** label. But if you’d like to tackle something different, you're more than welcome to visit the [Issues](https://github.com/automattic/pocket-casts-android/issues) page and pick an item that interests you.

We always try to avoid duplicating efforts, so if you decide to work on an issue, leave a comment to state your intent. If you choose to focus on a new feature or the change you’re proposing is significant, we recommend waiting for a response before proceeding. The issue may no longer align with project goals.

If the change is trivial, feel free to send a pull request without notifying us.

We use [Spotless](https://github.com/diffplug/spotless) to maintain a consistent code style consistent. Please run the `spotlessCheck` gradle task to check for any issues with your code (which can be fixed with `spotlessApply`).

### Kotlin

All new features should be written in Kotlin and any Java code should be converted to Kotlin at the first opportunity.

### Jetpack Compose

We love working with Jetpack Compose and we are slowly migrating the screens in the App to use compose. Any new screens/views should be written in Compose. If you would like to update a screen or view to use compose when you are updating it, that would be great!

### Prefer Coroutines to RxJava

We have quite a bit of RxJava in the app, and that's not changing anytime soon, but we prefer coroutines going forward. Therefore, when deciding between implementing something using RxJava and Coroutines, please prefer Coroutines. Likewise, if you see a good opportunity to convert something from RxJava to coroutines, feel free to make that change.

### Adding String Resources
As [discussed above](#Translating), we use GlotPress for translations. This means that when adding string resources to the app you only need to add a resource for English—our release process will take care of getting it translated through GlotPress.

If you do add a string resource that should not be translated, just add the appropriate `translatable` flag to the string resource. For example,

``` xml
<string name="app_name" translatable="false">Pocket Casts</string>
```

## Pull Requests and Code Reviews

All code contributions pass through pull requests. If you haven't created a pull request before, we recommend this free video series, [How to Contribute to an Open Source Project on GitHub](https://egghead.io/courses/how-to-contribute-to-an-open-source-project-on-github).

The core team monitors and reviews all pull requests. Depending on the changes, we will either approve them or close them with an explanation. We might also work with you to improve a pull request before approval.

We do our best to respond quickly to all pull requests. If you don't get a response from us after a week, feel free to ping us on your PR.

Note: If you are part of the org and have the permissions on the repo, don't forget to assign yourself to the PR, and add the appropriate GitHub label and Milestone for the PR.

### Continuous Integration

When opening a PR from a fork, some of the CI checks must be manually triggered by a member of the Pocket Casts team. That means you don't need to worry if some of the CI checks are not running—we'll take care of it when we review the PR and, if there are any issues, we'll let you know.

### PR merge policy

* PRs require one reviewer to approve the PR before it can be merged to the base branch
* We keep the PR git history when merging (merge via "merge commit")
* The reviewer who approved the PR may merge it right after approval (without waiting for the PR author) if all checks are green.
