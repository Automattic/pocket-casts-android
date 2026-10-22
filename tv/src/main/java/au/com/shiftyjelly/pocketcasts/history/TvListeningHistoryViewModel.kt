package au.com.shiftyjelly.pocketcasts.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.ListeningHistoryShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class TvListeningHistoryViewModel @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    val uiState: StateFlow<TvListeningHistoryUiState> = episodeManager.findPlaybackHistoryEpisodesFlow()
        .map { episodes ->
            if (episodes.isEmpty()) {
                TvListeningHistoryUiState.Empty
            } else {
                TvListeningHistoryUiState.Loaded(episodes)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout = 300.milliseconds, replayExpiration = Duration.ZERO),
            TvListeningHistoryUiState.Loading,
        )

    fun trackShown() {
        eventHorizon.track(ListeningHistoryShownEvent)
    }

    fun play(episode: PodcastEpisode) {
        viewModelScope.launch {
            try {
                playbackManager.playNowSuspend(episode = episode, sourceView = SourceView.LISTENING_HISTORY)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to play episode from TV listening history")
            }
        }
    }
}

sealed interface TvListeningHistoryUiState {
    data object Loading : TvListeningHistoryUiState

    data object Empty : TvListeningHistoryUiState

    data class Loaded(
        val episodes: List<PodcastEpisode>,
    ) : TvListeningHistoryUiState
}
