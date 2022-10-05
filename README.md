<p align="center">
    <img src="https://user-images.githubusercontent.com/308331/194037473-41ad7eba-8602-4be5-be73-49e3c0c48c12.svg#gh-light-mode-only" />
    <img src="https://user-images.githubusercontent.com/308331/194041226-4c6d8181-cafa-4ea8-8735-1d8106f5e5f6.svg#gh-dark-mode-only" />
</p>

<p align="center">
    <a href="https://buildkite.com/automattic/pocket-casts-android"><img src="https://badge.buildkite.com/dc67ef3537ad6cf097ff193b28063347418205a341d55f9940.svg?branch=main" /></a>
    <a href="https://github.com/Automattic/pocket-casts-android/blob/main/LICENSE.md"><img src="https://img.shields.io/badge/license-MPL-blue" /></a>
</p>

<p align="center">
    Pocket Casts is the world's most powerful podcast platform, an app by listeners, for listeners.
</p>

## Install

If you're just looking to install Pocket Casts Android, you can find it on [Google Play](https://play.google.com/store/apps/details?id=au.com.shiftyjelly.pocketcasts). If you're a developer wanting to contribute, read on.

## Build Instructions

1. Make sure you've installed [Android Studio](https://developer.android.com/studio/index.html).
2. In Android Studio, open the project from the local repository.
3. Go to Tools → Device Manager and create an emulated device.
4. Go to Run → Edit Configurations… and create an Android App configuration. 
5. Select the module "pocketcasts-android.app.main".
6. Run.

## Build and Test

To build, install, and test the project from the command line:

    $ ./gradlew :app:assembleDebugProd          # assemble the debug .apk
    $ ./gradlew :app:installDebugProd           # install the debug .apk to a connected device
    $ ./gradlew :app:testDebugUnitTest          # assemble, install and run unit tests
    $ ./gradlew :app:connectedDebugAndroidTest  # assemble, install and run Android tests

## Directory structure
    .
    ├── app                    # Mobile app
    ├── automotive             # Automotive app
    ├── modules
    │   ├── features
    │   │   ├── account        # Create account and sign in pages.
    │   │   ├── cartheme       # Automotive resources needed for the account pages.
    │   │   ├── discover       # Discover section.
    │   │   ├── filters        # Filters section.
    │   │   ├── navigation     # Navigation utilities.
    │   │   ├── player         # Full screen player
    │   │   ├── podcasts       # Podcasts section.
    │   │   ├── profile        # Profile section.
    │   │   ├── search         # Search pages.
    │   │   └── settings       # Settings pages.
    │   └── services
    │       ├── compose        # Shared Compose code.
    │       ├── images         # Image resources.
    │       ├── localization   # Contains the strings in English and localized strings from GlotPress. 
    │       ├── model          # The database logic and entities. Also transfer objects required which aren't stored in the database.
    │       ├── preferences    # Stores the user preferences and configuration settings.
    │       ├── repositories   # Provides accessing to the data from the 'servers' and 'model' modules.
    │       ├── servers        # Provides the network calls to the servers. The UI layer should access these through the 'repositories' module.
    │       ├── ui             # Shared UI code for the 'compose' and 'views' modules. This includes the themes.
    │       ├── utils          # Utility classes.
    │       └── views          # Shred Only the old views code.

## Contributing

Read our [Contributing Guide](CONTRIBUTING.md) to learn about reporting issues, contributing code, and more ways to contribute.

## Security

If you happen to find a security vulnerability, please let us know at https://hackerone.com/automattic and allow us to respond before disclosing the issue publicly.

## Documentation

- [Coding Style](docs/coding-style.md) - guidelines and validation and auto-formatting tools
- [Pull Request Guidelines](docs/pull-request-guidelines.md) - branch naming and how to write good pull requests

## Signing a Release

To build a _signed_ release, add these lines to your ~/.gradle/gradle.properties file

    pocketcastsKeyStoreFile=/Users/username/git/secret.keystore
    pocketcastsKeyStorePassword=
    pocketcastsKeyStoreAlias=
    pocketcastsKeyStoreAliasPassword=
