package au.com.shiftyjelly.pocketcasts.playlists.manual.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = AddEpisodesViewModel.Factory::class)
class AddEpisodesViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
) : ViewModel() {
    val uiState = flow {
        val playlistFlow = playlistManager.observeManualPlaylist(playlistUuid)

        val uiStates = combine(
            playlistFlow,
            settings.artworkConfiguration.flow,
        ) { playlist, artworkConfig ->
            if (playlist != null) {
                UiState(
                    playlist = playlist,
                    sources = playlistManager.getManualPlaylistEpisodeSources(),
                    useEpisodeArtwork = artworkConfig.useEpisodeArtwork(ArtworkConfiguration.Element.Filters),
                )
            } else {
                null
            }
        }

        // Add a small delay to prevent rendering all data while the bottom sheet is still animating in
        delay(350)
        emitAll(uiStates)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    private val episodeFlowsCache = mutableMapOf<String, StateFlow<List<PodcastEpisode>>>()

    fun getEpisodesFlow(podcastUuid: String): StateFlow<List<PodcastEpisode>> {
        return episodeFlowsCache.getOrPut(podcastUuid) {
            val flow = playlistManager.observeManualPlaylistAvailableEpisodes(playlistUuid, podcastUuid)
            flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 300), initialValue = emptyList())
        }
    }

    data class UiState(
        val playlist: ManualPlaylist,
        val sources: List<ManualPlaylistEpisodeSource>,
        val useEpisodeArtwork: Boolean,
    )

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String): AddEpisodesViewModel
    }
}
