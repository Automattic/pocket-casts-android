package au.com.shiftyjelly.pocketcasts.upnext

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.UpNextSyncWorker
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.UpNextDiscoverButtonTappedEvent
import com.automattic.eventhorizon.UpNextShownEvent
import com.automattic.eventhorizon.UpNextSourceType
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
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber

@HiltViewModel
class TvUpNextViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncManager: SyncManager,
    private val upNextQueue: UpNextQueue,
    private val playbackManager: PlaybackManager,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    val uiState: StateFlow<TvUpNextUiState> = upNextQueue.changesObservable.asFlow()
        .map { state ->
            val episodes = when (state) {
                is UpNextQueue.State.Empty -> emptyList()
                is UpNextQueue.State.Loaded -> state.queue.filterIsInstance<PodcastEpisode>()
            }
            if (episodes.isEmpty()) {
                TvUpNextUiState.Empty
            } else {
                TvUpNextUiState.Loaded(episodes)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout = 300.milliseconds, replayExpiration = Duration.ZERO),
            TvUpNextUiState.Loading,
        )

    fun onShown() {
        UpNextSyncWorker.enqueue(syncManager, context)
    }

    fun trackUpNextShown() {
        eventHorizon.track(UpNextShownEvent(source = UpNextSourceType.TabBar))
    }

    fun trackDiscoverButtonTapped() {
        eventHorizon.track(UpNextDiscoverButtonTappedEvent(source = UpNextSourceType.TabBar))
    }

    fun play(episode: PodcastEpisode) {
        viewModelScope.launch {
            try {
                playbackManager.playNowSuspend(episode = episode, sourceView = SourceView.UP_NEXT)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to play episode from TV up next")
            }
        }
    }
}

sealed interface TvUpNextUiState {
    data object Loading : TvUpNextUiState

    data object Empty : TvUpNextUiState

    data class Loaded(
        val episodes: List<PodcastEpisode>,
    ) : TvUpNextUiState
}
