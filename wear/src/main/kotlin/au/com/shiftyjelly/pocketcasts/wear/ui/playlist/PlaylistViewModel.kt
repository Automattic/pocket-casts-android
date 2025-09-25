package au.com.shiftyjelly.pocketcasts.wear.ui.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistManager: PlaylistManager,
    settings: Settings,
) : ViewModel() {

    private val playlistUuid: String = savedStateHandle[PlaylistScreen.ARGUMENT_PLAYLIST_UUID] ?: ""
    private val playlistType = Playlist.Type.fromValue(savedStateHandle[PlaylistScreen.ARGUMENT_PLAYLIST_TYPE] ?: "")

    sealed class UiState {
        object Loading : UiState()

        data class Empty(
            val playlistTitle: String?,
        ) : UiState()

        data class Loaded(
            val playlistTitle: String,
            val episodes: List<PodcastEpisode>,
        ) : UiState()
    }

    private val playlistFlow = when (playlistType) {
        Playlist.Type.Manual -> playlistManager.manualPlaylistFlow(playlistUuid)
        Playlist.Type.Smart -> playlistManager.smartPlaylistFlow(playlistUuid)
        null -> flowOf(null)
    }

    val uiState = playlistFlow
        .map { playlist ->
            if (playlist != null) {
                val mappedEpisodes = withContext(Dispatchers.Default) {
                    playlist.episodes.mapNotNull(PlaylistEpisode::toPodcastEpisode)
                }
                if (mappedEpisodes.isNotEmpty()) {
                    UiState.Loaded(playlist.title, mappedEpisodes)
                } else {
                    UiState.Empty(playlist.title)
                }
            } else {
                UiState.Empty(playlistTitle = null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Loading)

    val artworkConfiguration = settings.artworkConfiguration.flow
}
