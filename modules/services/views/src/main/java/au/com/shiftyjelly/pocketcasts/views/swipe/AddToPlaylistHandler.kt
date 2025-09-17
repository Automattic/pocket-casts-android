package au.com.shiftyjelly.pocketcasts.views.swipe

import androidx.fragment.app.FragmentManager

interface AddToPlaylistHandler {
    fun handle(episodeUuid: String, fragmentManager: FragmentManager)
}
