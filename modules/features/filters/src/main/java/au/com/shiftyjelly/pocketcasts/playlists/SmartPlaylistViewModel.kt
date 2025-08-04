package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = SmartPlaylistViewModel.Factory::class)
class SmartPlaylistViewModel @AssistedInject constructor(
    @Assisted playlistUuuid: String,
    private val playlistManager: PlaylistManager,
) : ViewModel() {
    val uiState = playlistManager.observePlaylistsPreview()
        .mapNotNull { playlists -> playlists.firstOrNull { it.uuid == playlistUuuid } }
        .map { UiState(it.title) }
        .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = UiState.Empty)

    data class UiState(
        val playlistTitle: String,
    ) {
        companion object {
            val Empty = UiState(
                playlistTitle = "",
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistUuuid: String): SmartPlaylistViewModel
    }
}
