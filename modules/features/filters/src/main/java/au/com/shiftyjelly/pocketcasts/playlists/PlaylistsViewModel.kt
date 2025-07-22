package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
) : ViewModel() {
    internal val uiState = combine(
        playlistManager.observePlaylistsPreview(),
        settings.showPlaylistsOnboarding.flow,
        ::UiState,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Empty)

    fun deletePlaylist(uuid: String) {
        viewModelScope.launch {
            playlistManager.deletePlaylist(uuid)
        }
    }

    internal data class UiState(
        val playlists: List<PlaylistPreview>,
        val showOnboarding: Boolean,
    ) {
        companion object {
            val Empty = UiState(
                playlists = emptyList(),
                showOnboarding = false,
            )
        }
    }
}
