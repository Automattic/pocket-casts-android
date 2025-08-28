package au.com.shiftyjelly.pocketcasts.playlists.manual.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = AddEpisodesViewModel.Factory::class)
class AddEpisodesViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    private val playlistManager: PlaylistManager,
) : ViewModel() {
    val uiState = flow {
        val uiStates = playlistManager.observeManualPlaylist(playlistUuid).map { playlist ->
            if (playlist != null) {
                UiState(
                    playlist = playlist,
                    sources = playlistManager.getManualPlaylistEpisodeSources(),
                )
            } else {
                null
            }
        }

        // Add a small delay to prevent rendering all data while the bottom sheet is still animating in
        delay(350)
        emitAll(uiStates)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    data class UiState(
        val playlist: ManualPlaylist,
        val sources: List<ManualPlaylistEpisodeSource>,
    )

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String): AddEpisodesViewModel
    }
}
