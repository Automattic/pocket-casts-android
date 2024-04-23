package au.com.shiftyjelly.pocketcasts.views.fragments

import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics

object BaseFragmentToolbar {
    sealed class ChromeCastButton {
        class Shown(val chromeCastAnalytics: ChromeCastAnalytics) : ChromeCastButton()
        data object None : ChromeCastButton()
    }

    sealed class ProfileButton {
        data class Shown(
            val onClick: (() -> Unit)? = null,
        ) : ProfileButton()
        data object None : ProfileButton()
    }
}
