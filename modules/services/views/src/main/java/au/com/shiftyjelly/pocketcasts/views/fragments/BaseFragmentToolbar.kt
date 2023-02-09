package au.com.shiftyjelly.pocketcasts.views.fragments

import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics

object BaseFragmentToolbar {
    sealed class ChromeCastButton {
        class Shown(val chromeCastAnalytics: ChromeCastAnalytics) : ChromeCastButton()
        object None : ChromeCastButton()
    }
}
