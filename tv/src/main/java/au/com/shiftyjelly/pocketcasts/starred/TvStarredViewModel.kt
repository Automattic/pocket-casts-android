package au.com.shiftyjelly.pocketcasts.starred

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.StarredSyncWorker
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.StarredShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
class TvStarredViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeManager: EpisodeManager,
    private val syncManager: SyncManager,
    private val playbackManager: PlaybackManager,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    val uiState: StateFlow<TvStarredUiState> = episodeManager.findStarredEpisodesFlow()
        .map { episodes ->
            if (episodes.isEmpty()) {
                TvStarredUiState.Empty
            } else {
                TvStarredUiState.Loaded(episodes)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout = 300.milliseconds, replayExpiration = Duration.ZERO),
            TvStarredUiState.Loading,
        )

    fun onShown() {
        StarredSyncWorker.enqueue(syncManager, context)
    }

    fun trackStarredShown() {
        eventHorizon.track(StarredShownEvent)
    }

    fun play(episode: PodcastEpisode) {
        viewModelScope.launch {
            try {
                playbackManager.playNowSuspend(episode = episode, sourceView = SourceView.STARRED)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to play episode from TV starred")
            }
        }
    }
}

sealed interface TvStarredUiState {
    data object Loading : TvStarredUiState

    data object Empty : TvStarredUiState

    data class Loaded(
        val episodes: List<PodcastEpisode>,
    ) : TvStarredUiState
}
