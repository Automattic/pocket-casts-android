package au.com.shiftyjelly.pocketcasts.views.swipe

import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment

interface AddToPlaylistFragmentFactory {
    fun create(
        episodeUuid: String,
        customTheme: Theme.ThemeType? = null,
    ): BaseDialogFragment

    companion object {
        // We support adding episodes only from phones but we need a stub to satisfy Dagger.
        val Stub = object : AddToPlaylistFragmentFactory {
            override fun create(episodeUuid: String, customTheme: Theme.ThemeType?): BaseDialogFragment {
                error("Adding episodes to playlist is not supported")
            }
        }
    }
}
