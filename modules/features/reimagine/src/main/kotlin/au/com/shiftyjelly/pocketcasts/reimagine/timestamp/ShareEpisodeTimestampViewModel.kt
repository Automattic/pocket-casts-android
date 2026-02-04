package au.com.shiftyjelly.pocketcasts.reimagine.timestamp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.ShareActionMediaType
import com.automattic.eventhorizon.ShareScreenShownEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = ShareEpisodeTimestampViewModel.Factory::class)
class ShareEpisodeTimestampViewModel @AssistedInject constructor(
    @Assisted("podcastId") private val podcastUuid: String,
    @Assisted("episodeId") private val episodeUuid: String,
    @Assisted private val sourceView: SourceView,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
) : ViewModel() {
    val uiState = combine(
        podcastManager.podcastByEpisodeUuidFlow(episodeUuid),
        episodeManager.findByUuidFlow(episodeUuid),
        settings.artworkConfiguration.flow.map { it.useEpisodeArtwork },
        ::UiState,
    ).stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = UiState())

    data class UiState(
        val podcast: Podcast? = null,
        val episode: PodcastEpisode? = null,
        val useEpisodeArtwork: Boolean = false,
    )

    fun onScreenShown() {
        eventHorizon.track(
            ShareScreenShownEvent(
                podcastUuid = podcastUuid,
                episodeUuid = episodeUuid,
                source = sourceView.eventHorizonValue,
                type = ShareActionMediaType.EpisodeTimestamp,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("podcastId") podcastUuid: String,
            @Assisted("episodeId") episodeUuid: String,
            sourceView: SourceView,
        ): ShareEpisodeTimestampViewModel
    }
}
