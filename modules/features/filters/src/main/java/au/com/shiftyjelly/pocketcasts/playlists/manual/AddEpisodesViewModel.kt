package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.text.SearchFieldState
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistFolderSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistPodcastSource
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
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
    private val tracker: AnalyticsTracker,
    @Assisted private val playlistUuid: String,
) : ViewModel() {
    val homeSearchState = SearchFieldState()
    val folderSearchState = SearchFieldState()
    val podcastSearchState = SearchFieldState()

    private val _messageQueue = MutableSharedFlow<Message>()
    val messageQueue = _messageQueue.asSharedFlow()

    private val addedEpisodeUuids = MutableStateFlow(emptySet<String>())

    val uiState = flow {
        val playlistFlow = playlistManager.manualPlaylistFlow(playlistUuid)

        val uiStates = combine(
            playlistFlow,
            homeSearchState.textFlow,
            settings.artworkConfiguration.flow,
            addedEpisodeUuids,
        ) { playlist, searchTerm, artworkConfig, addedEpisodes ->
            if (playlist != null) {
                UiState(
                    playlist = playlist,
                    sources = playlistManager.getManualEpisodeSources(searchTerm),
                    hasAnyFolders = playlistManager.getManualEpisodeSources().any { it is ManualPlaylistFolderSource },
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

    fun addEpisode(episodeUuid: String) {
        viewModelScope.launch {
            val isAdded = playlistManager.addManualEpisode(playlistUuid, episodeUuid)
            if (isAdded) {
                addedEpisodeUuids.update { value -> value + episodeUuid }
            } else {
                _messageQueue.emit(Message.FailedToAddEpisode)
            }
        }
    }

    private val folderSourcesFlowCache = mutableMapOf<String, StateFlow<List<ManualPlaylistPodcastSource>?>>()

    fun getFolderSourcesFlow(folderUuid: String): StateFlow<List<ManualPlaylistPodcastSource>?> {
        return folderSourcesFlowCache.getOrPut(folderUuid) {
            val flow = folderSearchState.textFlow.map { searchTerm ->
                playlistManager.getManualEpisodeSourcesForFolder(folderUuid, searchTerm)
            }
            flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 300, replayExpirationMillis = 300), initialValue = null)
        }
    }

    private val podcastEpisodesFlowCache = mutableMapOf<String, StateFlow<PodcastEpisodesUiState?>>()

    fun getPodcastEpisodesFlow(podcastUuid: String): StateFlow<PodcastEpisodesUiState?> {
        return podcastEpisodesFlowCache.getOrPut(podcastUuid) {
            val flow = podcastSearchState.textFlow.flatMapLatest { searchTerm ->
                combine(
                    playlistManager.notAddedManualEpisodesFlow(playlistUuid, podcastUuid, searchTerm),
                    playlistManager.notAddedManualEpisodesFlow(playlistUuid, podcastUuid),
                ) { filteredEpisodes, unfilteredEpisodes ->
                    PodcastEpisodesUiState(
                        episodes = filteredEpisodes,
                        unfilteredEpisodeCount = unfilteredEpisodes.size,
                    )
                }
            }
            flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 300, replayExpirationMillis = 300), initialValue = null)
        }
    }

    fun trackFolderTapped() {
        tracker.track(AnalyticsEvent.FILTER_ADD_EPISODES_FOLDER_TAPPED)
    }

    fun trackPodcastTapped() {
        tracker.track(AnalyticsEvent.FILTER_ADD_EPISODES_PODCAST_TAPPED)
    }

    fun trackEpisodeTapped() {
        val episodeCount = uiState.value?.playlist?.metadata?.totalEpisodeCount ?: Int.MAX_VALUE
        tracker.track(
            AnalyticsEvent.FILTER_ADD_EPISODES_EPISODE_TAPPED,
            mapOf("is_playlist_full" to (episodeCount > PlaylistManager.MANUAL_PLAYLIST_EPISODE_LIMIT))
        )
    }

    data class UiState(
        val playlist: ManualPlaylist,
        val sources: List<ManualPlaylistEpisodeSource>,
        val hasAnyFolders: Boolean,
        val useEpisodeArtwork: Boolean,
        val addedEpisodeUuids: Set<String>,
    )

    data class PodcastEpisodesUiState(
        val unfilteredEpisodeCount: Int,
        val episodes: List<PodcastEpisode>,
    )

    sealed interface Message {
        data object FailedToAddEpisode : Message
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String): AddEpisodesViewModel
    }
}
