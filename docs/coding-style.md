# Coding Style

Our code style guidelines are based on the [Android Code Style Guidelines for Contributors](https://source.android.com/source/code-style.html). We only changed a few rules:

* Line length is 120 characters
* FIXME must not be committed in the repository—use TODO instead.

## Spotless

All code needs to comply with the Spotless checks before being merged. You can check this locally using `./gradlew spotlessCheck`, or auto-format your code with `./gradlew spotlessApply`.

The project has a Git hook you can install to run this check on pre-commit `./gradlew installGitHooks`.
