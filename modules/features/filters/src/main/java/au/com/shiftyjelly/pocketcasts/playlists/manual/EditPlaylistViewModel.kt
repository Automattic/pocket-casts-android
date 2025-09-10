package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = EditPlaylistViewModel.Factory::class)
class EditPlaylistViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
) : ViewModel() {
    private var isOrderChanged = false
    private var _episodes by mutableStateOf(emptyList<PlaylistEpisode>())
    val episodes get() = _episodes

    val useEpisodeArtwork = settings.artworkConfiguration.flow.map { config ->
        config.useEpisodeArtwork(ArtworkConfiguration.Element.Filters)
    }

    init {
        viewModelScope.launch {
            // Add a small delay to prevent rendering all data while the screen is animating
            delay(350)
            val playlist = playlistManager.manualPlaylistFlow(playlistUuid).first()
            _episodes = playlist?.episodes.orEmpty()
        }
    }

    fun deleteEpisode(episodeUuid: String) {
        _episodes = _episodes.filter { it.uuid != episodeUuid }
        viewModelScope.launch {
            playlistManager.deleteManualEpisode(playlistUuid, episodeUuid)
        }
    }

    fun updateEpisodesOrder(episodes: List<PlaylistEpisode>) {
        this._episodes = episodes
        isOrderChanged = true
    }

    fun persistEpisodesOrder() {
        if (isOrderChanged) {
            viewModelScope.launch(NonCancellable) {
                val sortedUuids = withContext(Dispatchers.Default) {
                    _episodes.map(PlaylistEpisode::uuid)
                }
                playlistManager.sortManualEpisodes(playlistUuid, sortedUuids)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String): EditPlaylistViewModel
    }
}
