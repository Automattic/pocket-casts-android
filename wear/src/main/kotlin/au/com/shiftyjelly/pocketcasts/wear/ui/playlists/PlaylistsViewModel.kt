package au.com.shiftyjelly.pocketcasts.wear.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    playlistManager: PlaylistManager,
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(val playlists: List<PlaylistPreview>) : UiState()
    }

    val uiState = playlistManager.playlistPreviewsFlow()
        .map { UiState.Loaded(playlists = it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Loading)
}
