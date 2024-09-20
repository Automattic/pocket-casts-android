package au.com.shiftyjelly.pocketcasts.reimagine.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = SharePodcastViewModel.Factory::class)
class SharePodcastViewModel @AssistedInject constructor(
    @Assisted private val podcastUuid: String,
    @Assisted private val sourceView: SourceView,
    private val podcastManager: PodcastManager,
    private val tracker: AnalyticsTracker,
) : ViewModel() {
    val uiState = combine(
        podcastManager.observePodcastByUuidFlow(podcastUuid),
        podcastManager.observeEpisodeCountByPodcatUuid(podcastUuid),
        ::UiState,
    ).stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = UiState())

    fun onScreenShown() {
        tracker.track(
            AnalyticsEvent.SHARE_SCREEN_SHOWN,
            mapOf(
                "type" to "podcast",
                "podcast_uuid" to podcastUuid,
                "source" to sourceView.analyticsValue,
            ),
        )
    }

    data class UiState(
        val podcast: Podcast? = null,
        val episodeCount: Int = 0,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            podcastUuid: String,
            sourceView: SourceView,
        ): SharePodcastViewModel
    }
}
