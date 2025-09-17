package au.com.shiftyjelly.pocketcasts.views.swipe

import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment

interface AddToPlaylistFragmentFactory {
    fun create(episodeUuid: String): BaseDialogFragment
}
