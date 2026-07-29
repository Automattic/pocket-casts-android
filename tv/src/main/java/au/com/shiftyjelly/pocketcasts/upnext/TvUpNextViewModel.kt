package au.com.shiftyjelly.pocketcasts.upnext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel
class TvUpNextViewModel @Inject constructor(
    private val upNextQueue: UpNextQueue,
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
}

sealed interface TvUpNextUiState {
    data object Loading : TvUpNextUiState

    data object Empty : TvUpNextUiState

    data class Loaded(
        val episodes: List<PodcastEpisode>,
    ) : TvUpNextUiState
}
