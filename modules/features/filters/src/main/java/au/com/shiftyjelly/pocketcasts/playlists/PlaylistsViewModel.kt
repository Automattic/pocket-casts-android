package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
) : ViewModel() {
    internal val uiState = combine(
        playlistManager.observePlaylistsPreview(),
        settings.artworkConfiguration.flow.map { it.useEpisodeArtwork(ArtworkConfiguration.Element.Filters) },
        settings.showPlaylistsOnboarding.flow,
        ::UiState,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Empty)

    internal data class UiState(
        val playlists: List<PlaylistPreview>,
        val showEpisodeArtwork: Boolean,
        val showOnboarding: Boolean,
    ) {
        companion object {
            val Empty = UiState(
                playlists = emptyList(),
                showEpisodeArtwork = false,
                showOnboarding = false,
            )
        }
    }
}
