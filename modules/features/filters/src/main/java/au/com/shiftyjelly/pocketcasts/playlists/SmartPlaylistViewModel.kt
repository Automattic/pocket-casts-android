package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylist
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SmartPlaylistViewModel.Factory::class)
class SmartPlaylistViewModel @AssistedInject constructor(
    @Assisted playlistUuid: String,
    private val playlistManager: PlaylistManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
) : ViewModel() {
    val bottomInset = settings.bottomInset

    val uiState = playlistManager.observeSmartPlaylist(playlistUuid)
        .map { UiState(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = UiState.Empty)

    fun shouldShowPlayAllWarning(): Boolean {
        val queueEpisodes = playbackManager.upNextQueue.allEpisodes
        return queueEpisodes.size >= PLAY_ALL_WARNING_EPISODE_COUNT
    }

    private var playAllJob: Job? = null

    fun playAll() {
        if (playAllJob?.isActive == true) {
            return
        }
        playAllJob = viewModelScope.launch(Dispatchers.Default) {
            val episodes = uiState.value.smartPlaylist?.episodes?.takeIf { it.isNotEmpty() } ?: return@launch
            playbackManager.upNextQueue.removeAll()
            playbackManager.playEpisodes(episodes, SourceView.FILTERS)
        }
    }

    data class UiState(
        val smartPlaylist: SmartPlaylist?,
    ) {
        companion object {
            val Empty = UiState(
                smartPlaylist = null,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String): SmartPlaylistViewModel
    }

    companion object {
        private const val PLAY_ALL_WARNING_EPISODE_COUNT = 4
    }
}
