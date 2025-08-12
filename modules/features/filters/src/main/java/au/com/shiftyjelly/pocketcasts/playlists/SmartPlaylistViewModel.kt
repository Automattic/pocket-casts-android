package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylist
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SmartPlaylistViewModel.Factory::class)
class SmartPlaylistViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    private val playlistManager: PlaylistManager,
    private val playbackManager: PlaybackManager,
    private val downloadManager: DownloadManager,
    private val settings: Settings,
) : ViewModel() {
    val bottomInset = settings.bottomInset

    private val _startMultiSelectingSignal = MutableSharedFlow<Unit>()
    val startMultiSelectingSignal = _startMultiSelectingSignal.asSharedFlow()

    private val _chromeCastSignal = MutableSharedFlow<Unit>()
    val chromeCastSignal = _chromeCastSignal.asSharedFlow()

    private val _showSettingsSignal = MutableSharedFlow<Unit>()
    val showSettingsSignal = _showSettingsSignal.asSharedFlow()

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

    fun downloadAll() {
        val episodes = uiState.value.smartPlaylist?.episodes?.take(DOWNLOAD_ALL_LIMIT)
        episodes?.forEach { episode ->
            downloadManager.addEpisodeToQueue(episode, "filter download all", fireEvent = false, source = SourceView.FILTERS)
        }
    }

    fun updateSortType(type: PlaylistEpisodeSortType) {
        viewModelScope.launch(NonCancellable) {
            playlistManager.updateSortType(playlistUuid, type)
        }
    }

    fun updateAutoDownload(isEnabled: Boolean) {
        viewModelScope.launch(NonCancellable) {
            playlistManager.updateAutoDownload(playlistUuid, isEnabled)
        }
    }

    fun updateAutoDownloadLimit(limit: Int) {
        viewModelScope.launch(NonCancellable) {
            playlistManager.updateAutoDownloadLimit(playlistUuid, limit)
        }
    }

    fun updateName(name: String) {
        val sanitizedName = name.trim()
        if (sanitizedName.isEmpty()) {
            return
        }
        viewModelScope.launch(NonCancellable) {
            playlistManager.updateName(playlistUuid, sanitizedName)
        }
    }

    fun startMultiSelecting() {
        viewModelScope.launch {
            _startMultiSelectingSignal.emit(Unit)
        }
    }

    fun startChromeCast() {
        viewModelScope.launch {
            _chromeCastSignal.emit(Unit)
        }
    }

    fun showSettings() {
        viewModelScope.launch {
            _showSettingsSignal.emit(Unit)
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
        const val DOWNLOAD_ALL_LIMIT = 100
        private const val PLAY_ALL_WARNING_EPISODE_COUNT = 4
    }
}
