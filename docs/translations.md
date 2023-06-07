# Translations

English string resources are saved in the 
[localization module's strings.xml file](https://github.com/Automattic/pocket-casts-android/blob/main/modules/services/localization/src/main/res/values/strings.xml).
Those English strings are then translated into the other languages we support through
https://translate.wordpress.com/projects/pocket-casts/android/. We update the translations for
all languages other than English from translate.wordpress.com each time we cut a release.

This means that only English string resources should be edited directly in this repo. All other
languages will get updated from translate.wordpress.com as [part of our release process](https://github.com/Automattic/pocket-casts-android/blob/b8287a4b97e4c1deac852e861b7ea17583412d3d/fastlane/Fastfile#L203-L209).

If you're interested in helping to translate Pocket Casts you can contribute at translate.wordpress.com.
This page gives an overview of how to get started: https://translate.wordpress.com/glotpress/.
