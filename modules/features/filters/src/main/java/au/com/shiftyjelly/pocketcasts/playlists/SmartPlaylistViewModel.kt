package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylist
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
    @Assisted playlistUuid: String,
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
) : ViewModel() {
    val bottomInset = settings.bottomInset

    val uiState = playlistManager.observeSmartPlaylist(playlistUuid)
        .map { UiState(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = UiState.Empty)

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
}
