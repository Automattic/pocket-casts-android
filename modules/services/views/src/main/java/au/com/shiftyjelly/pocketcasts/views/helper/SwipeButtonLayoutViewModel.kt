package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwipeButtonLayoutViewModel @Inject constructor(
    val analyticsTracker: AnalyticsTrackerWrapper,
    val playbackManager: PlaybackManager,
    val podcastManager: PodcastManager,
) : ViewModel() {

    fun share(
        episode: PodcastEpisode,
        fragmentManager: FragmentManager,
        context: Context,
        swipeSource: EpisodeItemTouchHelper.SwipeSource
    ) {

        viewModelScope.launch(Dispatchers.Default) {

            trackSwipeAction(
                swipeSource = swipeSource,
                swipeAction = EpisodeItemTouchHelper.SwipeAction.SHARE,
            )

            val podcast = podcastManager.findPodcastByUuid(episode.podcastUuid) ?: return@launch

            ShareDialog(
                episode = episode,
                podcast = podcast,
                fragmentManager = fragmentManager,
                context = context,
                shouldShowPodcast = false,
                analyticsTracker = analyticsTracker,
            ).show(sourceView = SourceView.SWIPE_ACTION)
        }
    }

    fun trackSwipeAction(
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
        swipeAction: EpisodeItemTouchHelper.SwipeAction,
    ) {
        analyticsTracker.track(
            AnalyticsEvent.EPISODE_SWIPE_ACTION_PERFORMED,
            mapOf(
                "action" to swipeAction.analyticsValue,
                "source" to swipeSource.analyticsValue
            )
        )
    }

    fun episodeSwipeUpNextTop(
        episode: BaseEpisode,
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            playbackManager.playNext(
                episode = episode,
                source = swipeSourceToSourceView(swipeSource)
            )
            trackSwipeAction(
                swipeSource = swipeSource,
                swipeAction = EpisodeItemTouchHelper.SwipeAction.UP_NEXT_ADD_TOP
            )
        }
    }

    fun episodeSwipeUpNextBottom(
        episode: BaseEpisode,
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            playbackManager.playLast(
                episode = episode,
                source = swipeSourceToSourceView(swipeSource)
            )
            trackSwipeAction(
                swipeSource = swipeSource,
                swipeAction = EpisodeItemTouchHelper.SwipeAction.UP_NEXT_ADD_BOTTOM
            )
        }
    }

    fun episodeSwipeRemoveUpNext(
        episode: BaseEpisode,
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            playbackManager.removeEpisode(
                episodeToRemove = episode,
                source = swipeSourceToSourceView(swipeSource)
            )
            trackSwipeAction(
                swipeSource = swipeSource,
                swipeAction = EpisodeItemTouchHelper.SwipeAction.UP_NEXT_REMOVE
            )
        }
    }

    fun isEpisodeQueued(episode: BaseEpisode) = playbackManager.upNextQueue.contains(episode.uuid)

    private fun swipeSourceToSourceView(swipeSource: EpisodeItemTouchHelper.SwipeSource) = when (swipeSource) {
        EpisodeItemTouchHelper.SwipeSource.PODCAST_DETAILS -> SourceView.PODCAST_SCREEN
        EpisodeItemTouchHelper.SwipeSource.FILTERS -> SourceView.FILTERS
        EpisodeItemTouchHelper.SwipeSource.DOWNLOADS -> SourceView.DOWNLOADS
        EpisodeItemTouchHelper.SwipeSource.LISTENING_HISTORY -> SourceView.LISTENING_HISTORY
        EpisodeItemTouchHelper.SwipeSource.STARRED -> SourceView.STARRED
        EpisodeItemTouchHelper.SwipeSource.FILES -> SourceView.FILES
        EpisodeItemTouchHelper.SwipeSource.UP_NEXT -> SourceView.UP_NEXT
    }

    private fun removeEpisode(
        episode: BaseEpisode,
        swipeSource: EpisodeItemTouchHelper.SwipeSource,
    ) {
        playbackManager.removeEpisode(
            episodeToRemove = episode,
            source = swipeSourceToSourceView(swipeSource)
        )
        trackSwipeAction(
            swipeSource = swipeSource,
            swipeAction = EpisodeItemTouchHelper.SwipeAction.UP_NEXT_REMOVE
        )
    }
}
