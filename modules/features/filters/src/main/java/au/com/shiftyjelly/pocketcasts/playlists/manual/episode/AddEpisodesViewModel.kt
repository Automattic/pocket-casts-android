package au.com.shiftyjelly.pocketcasts.playlists.manual.episode

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = AddEpisodesViewModel.Factory::class)
class AddEpisodesViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
) : ViewModel() {
    val podcastSearchState = TextFieldState()
    private val podcastSearchFlow = snapshotFlow { podcastSearchState.text }
        .map { it.toString().trim() }
        .debounce { searchTerm -> if (searchTerm.isEmpty()) 0 else 300 }
        .distinctUntilChanged()

    val episodeSearchState = TextFieldState()
    private val episodeSearchFlow = snapshotFlow { episodeSearchState.text }
        .map { it.toString().trim() }
        .debounce { searchTerm -> if (searchTerm.isEmpty()) 0 else 300 }
        .distinctUntilChanged()

    private val _messageQueue = MutableSharedFlow<Message>()
    val messageQueue = _messageQueue.asSharedFlow()

    private val addedEpisodeUuids = MutableStateFlow(emptySet<String>())

    val uiState = flow {
        val playlistFlow = playlistManager.observeManualPlaylist(playlistUuid)

        val uiStates = combine(
            playlistFlow,
            podcastSearchFlow,
            settings.artworkConfiguration.flow,
            addedEpisodeUuids,
        ) { playlist, searchTerm, artworkConfig, addedEpisodes ->
            if (playlist != null) {
                UiState(
                    playlist = playlist,
                    sources = playlistManager.getManualPlaylistEpisodeSources(searchTerm),
                    useEpisodeArtwork = artworkConfig.useEpisodeArtwork(ArtworkConfiguration.Element.Filters),
                    addedEpisodeUuids = addedEpisodes,
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
            val flow = episodeSearchFlow.flatMapLatest { searchTerm ->
                playlistManager.observeManualPlaylistAvailableEpisodes(playlistUuid, podcastUuid, searchTerm)
            }
            flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 300), initialValue = emptyList())
        }
    }

    fun addEpisode(episodeUuid: String) {
        viewModelScope.launch {
            val isAdded = playlistManager.addManualPlaylistEpisode(playlistUuid, episodeUuid)
            if (isAdded) {
                addedEpisodeUuids.update { value -> value + episodeUuid }
            } else {
                _messageQueue.emit(Message.FailedToAddEpisode)
            }
        }
    }

    data class UiState(
        val playlist: ManualPlaylist,
        val sources: List<ManualPlaylistEpisodeSource>,
        val useEpisodeArtwork: Boolean,
        val addedEpisodeUuids: Set<String>,
    )

    sealed interface Message {
        data object FailedToAddEpisode : Message
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String): AddEpisodesViewModel
    }
}
