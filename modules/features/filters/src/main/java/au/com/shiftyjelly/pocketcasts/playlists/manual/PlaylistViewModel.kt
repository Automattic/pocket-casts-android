package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = PlaylistViewModel.Factory::class)
class PlaylistViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
) : ViewModel() {
    val bottomInset = settings.bottomInset

    val uiState = playlistManager.observeManualPlaylist(playlistUuid)
        .map { UiState(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = UiState.Empty)

    data class UiState(
        val manualPlaylist: ManualPlaylist?,
    ) {
        companion object {
            val Empty = UiState(
                manualPlaylist = null
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String): PlaylistViewModel
    }
}