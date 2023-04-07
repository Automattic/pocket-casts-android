package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

@HiltViewModel
class NowPlayingChipViewModel @Inject constructor(
    episodeManager: EpisodeManager,
    playbackManager: PlaybackManager,
    podcastManager: PodcastManager,
) : ViewModel() {

    data class State(
        val upNextQueue: UpNextQueue.State? = null,
        val playbackState: PlaybackState? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            playbackManager
                .upNextQueue
                .getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)
                .asFlow()
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)
                .collect { upNextQueue ->
                    _state.update {
                        it.copy(upNextQueue = upNextQueue)
                    }
                }
        }

        viewModelScope.launch {
            playbackManager
                .playbackStateRelay
                .asFlow()
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)
                .collect { playbackState ->
                    _state.update {
                        it.copy(playbackState = playbackState)
                    }
                }
        }
    }
}
