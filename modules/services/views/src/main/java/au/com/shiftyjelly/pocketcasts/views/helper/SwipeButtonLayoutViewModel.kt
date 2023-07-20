package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwipeButtonLayoutViewModel @Inject constructor(
    val analyticsTracker: AnalyticsTrackerWrapper,
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
}
