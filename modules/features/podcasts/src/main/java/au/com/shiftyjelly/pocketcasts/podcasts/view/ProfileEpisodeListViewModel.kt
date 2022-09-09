package au.com.shiftyjelly.pocketcasts.podcasts.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper.SwipeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

@HiltViewModel
class ProfileEpisodeListViewModel @Inject constructor(
    val episodeManager: EpisodeManager,
    val playbackManager: PlaybackManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    lateinit var episodeList: LiveData<List<Episode>>
    private lateinit var mode: ProfileEpisodeListFragment.Mode

    fun setup(mode: ProfileEpisodeListFragment.Mode) {
        this.mode = mode
        val episodeListFlowable = when (mode) {
            is ProfileEpisodeListFragment.Mode.Downloaded -> episodeManager.observeDownloadEpisodes()
            is ProfileEpisodeListFragment.Mode.Starred -> episodeManager.observeStarredEpisodes()
            is ProfileEpisodeListFragment.Mode.History -> episodeManager.observePlaybackHistoryEpisodes()
        }

        episodeList = LiveDataReactiveStreams.fromPublisher(episodeListFlowable)
    }

    @Suppress("UNUSED_PARAMETER")
    fun episodeSwiped(episode: Playable, index: Int) {
        if (episode !is Episode) return

        launch {
            if (!episode.isArchived) {
                episodeManager.archive(episode, playbackManager)
                trackSwipeAction(SwipeAction.ARCHIVE)
            } else {
                episodeManager.unarchive(episode)
                trackSwipeAction(SwipeAction.UNARCHIVE)
            }
        }
    }

    fun episodeSwipeUpNext(episode: Playable) {
        launch {
            if (playbackManager.upNextQueue.contains(episode.uuid)) {
                playbackManager.removeEpisode(episode)
                trackSwipeAction(SwipeAction.UP_NEXT_REMOVE)
            } else {
                playbackManager.playNext(episode)
                trackSwipeAction(SwipeAction.UP_NEXT_ADD_TOP)
            }
        }
    }

    fun episodeSwipeUpLast(episode: Playable) {
        launch {
            if (playbackManager.upNextQueue.contains(episode.uuid)) {
                playbackManager.removeEpisode(episode)
                trackSwipeAction(SwipeAction.UP_NEXT_REMOVE)
            } else {
                playbackManager.playLast(episode)
                trackSwipeAction(SwipeAction.UP_NEXT_ADD_BOTTOM)
            }
        }
    }

    fun onArchiveFromHereCount(episode: Episode): Int {
        val episodes = episodeList.value ?: return 0
        val index = max(episodes.indexOf(episode), 0) // -1 on not found
        return episodes.count() - index
    }

    fun clearAllEpisodeHistory() {
        launch {
            episodeManager.clearAllEpisodeHistory()
        }
    }

    private fun trackSwipeAction(swipeAction: SwipeAction) {
        val source = when (mode) {
            ProfileEpisodeListFragment.Mode.Downloaded -> SwipeSource.DOWNLOADS
            ProfileEpisodeListFragment.Mode.History -> SwipeSource.LISTENING_HISTORY
            ProfileEpisodeListFragment.Mode.Starred -> SwipeSource.STARRED
        }
        analyticsTracker.track(
            AnalyticsEvent.EPISODE_SWIPE_ACTION_PERFORMED,
            mapOf(
                ACTION_KEY to swipeAction.analyticsValue,
                SOURCE_KEY to source.analyticsValue
            )
        )
    }

    companion object {
        private const val ACTION_KEY = "action"
        private const val SOURCE_KEY = "source"
    }
}
