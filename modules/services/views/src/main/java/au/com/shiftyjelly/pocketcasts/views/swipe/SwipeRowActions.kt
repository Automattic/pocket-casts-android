package au.com.shiftyjelly.pocketcasts.views.swipe

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import javax.inject.Inject

data class SwipeRowActions(
    val ltr1: SwipeAction? = null,
    val ltr2: SwipeAction? = null,
    val ltr3: SwipeAction? = null,
    val rtl1: SwipeAction? = null,
    val rtl2: SwipeAction? = null,
    val rtl3: SwipeAction? = null,
) {
    fun applyTo(swipeLayout: SwipeRowLayout<SwipeAction>) {
        swipeLayout.setLtr1State(ltr1)
        swipeLayout.setLtr2State(ltr2)
        swipeLayout.setLtr3State(ltr3)
        swipeLayout.setRtl1State(rtl1)
        swipeLayout.setRtl2State(rtl2)
        swipeLayout.setRtl3State(rtl3)
    }

    companion object {
        val Empty = SwipeRowActions()
    }

    class Factory @Inject constructor(
        private val settings: Settings,
        private val queue: UpNextQueue,
    ) {
        fun unavailablePlaylistEpisode() = buildSwipeRowActions {
            rtl1 = SwipeAction.RemoveFromPlaylist
        }

        fun availablePlaylistEpisode(
            playlistType: Playlist.Type,
            episode: PodcastEpisode,
        ) = buildSwipeRowActions {
            val isInUpNext = queue.contains(episode.uuid)

            if (isInUpNext) {
                ltr1 = SwipeAction.RemoveFromUpNext
            } else {
                val (upNext1, upNext2) = settings.upNextSwipe.value.toSwipeActions()
                ltr1 = upNext1
                ltr2 = upNext2
            }

            when (playlistType) {
                Playlist.Type.Manual -> {
                    rtl1 = SwipeAction.RemoveFromPlaylist
                    rtl2 = episode.archiveSwipeAction()
                    rtl3 = SwipeAction.Share
                }

                Playlist.Type.Smart -> {
                    rtl1 = episode.archiveSwipeAction()
                    rtl2 = SwipeAction.Share
                }
            }
        }

        fun podcastEpisode(
            episode: PodcastEpisode,
        ) = buildSwipeRowActions {
            val isInUpNext = queue.contains(episode.uuid)

            if (isInUpNext) {
                ltr1 = SwipeAction.RemoveFromUpNext
            } else {
                val (upNext1, upNext2) = settings.upNextSwipe.value.toSwipeActions()
                ltr1 = upNext1
                ltr2 = upNext2
            }

            rtl1 = episode.archiveSwipeAction()
            rtl2 = SwipeAction.Share
        }
    }
}

private fun Settings.UpNextAction.toSwipeActions() = when (this) {
    Settings.UpNextAction.PLAY_NEXT -> SwipeAction.AddToUpNextTop to SwipeAction.AddToUpNextBottom
    Settings.UpNextAction.PLAY_LAST -> SwipeAction.AddToUpNextBottom to SwipeAction.AddToUpNextTop
}

private fun PodcastEpisode.archiveSwipeAction() = if (isArchived) SwipeAction.Unarchive else SwipeAction.Archive

private fun buildSwipeRowActions(block: SwipeRowActionsBuilder.() -> Unit = {}): SwipeRowActions {
    return SwipeRowActionsBuilder().apply(block).build()
}

private class SwipeRowActionsBuilder {
    var ltr1: SwipeAction? = null
    var ltr2: SwipeAction? = null
    var ltr3: SwipeAction? = null
    var rtl1: SwipeAction? = null
    var rtl2: SwipeAction? = null
    var rtl3: SwipeAction? = null

    fun build() = SwipeRowActions(
        ltr1 = ltr1,
        ltr2 = ltr2,
        ltr3 = ltr3,
        rtl1 = rtl1,
        rtl2 = rtl2,
        rtl3 = rtl3,
    )
}
