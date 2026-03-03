package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.text.SearchFieldState
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeUuidPair
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistPreviewForEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory.Source
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = AddToPlaylistViewModel.Factory::class)
class AddToPlaylistViewModel @AssistedInject constructor(
    private val playlistManager: PlaylistManager,
    private val tracker: AnalyticsTracker,
    @Assisted private val source: Source,
    @Assisted("uuids") private val episodeUuids: List<EpisodeUuidPair>,
    @Assisted("title") initialPlaylistTitle: String,
) : ViewModel() {
    private val previewsFlow = MutableStateFlow<List<PlaylistPreviewForEpisode>?>(null)

    val searchFieldState = SearchFieldState()

    init {
        viewModelScope.launch {
            searchFieldState.textFlow.collectLatest { searchTerm ->
                previewsFlow.value = playlistManager.playlistPreviewsForEpisodeFlow(searchTerm).first()
            }
        }
    }

    private val _createdPlaylist = CompletableDeferred<CreatedPlaylist>(viewModelScope.coroutineContext[Job])
    val createdPlaylist: Deferred<CreatedPlaylist> get() = _createdPlaylist

    val newPlaylistNameState = TextFieldState(
        initialText = initialPlaylistTitle,
        initialSelection = TextRange(0, initialPlaylistTitle.length),
    )

    val uiState = flow {
        val unfilteredSize = playlistManager.playlistPreviewsForEpisodeFlow().map { it.size }
        val uiStates = combine(previewsFlow, unfilteredSize) { filtered, unfilteredSize ->
            if (filtered != null) {
                UiState(
                    playlistPreviews = filtered,
                    unfilteredPlaylistsCount = unfilteredSize,
                    episodeLimit = PlaylistManager.MANUAL_PLAYLIST_EPISODE_LIMIT,
                )
            } else {
                null
            }
        }

        // Add a small delay to initial filtered playlists to avoid bottom sheet animation stutter
        delay(350)
        emitAll(uiStates)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    private val playlistsChanges = mutableMapOf<String, Boolean>()

    private fun cachePlaylistChange(uuid: String, shouldAdd: Boolean) {
        // If the change is not present add it.
        playlistsChanges.merge(uuid, shouldAdd) { isCurrentlyAdded, _ ->
            if (isCurrentlyAdded == shouldAdd) {
                // If the change is already stored keep it.
                isCurrentlyAdded
            } else {
                // If playlist was added but will be removed or vice-versa remove the change value.
                null
            }
        }
    }

    fun getPlaylistsAddedTo(): Set<PlaylistPreviewForEpisode> {
        val playlists = uiState.value?.playlistPreviews.orEmpty()
        val uuidsAddedTo = playlistsChanges.filterValues { it }.keys
        return playlists.filterTo(mutableSetOf()) { playlist -> playlist.uuid in uuidsAddedTo }
    }

    fun getArtworkUuidsFlow(playlistUuid: String): StateFlow<List<String>?> {
        return playlistManager.getArtworkUuidsFlow(playlistUuid)
    }

    suspend fun refreshArtworkUuids(playlistUuid: String) {
        playlistManager.refreshArtworkUuids(playlistUuid)
    }

    private var isCreationTriggered = false

    fun createPlaylist() {
        val sanitizedName = newPlaylistNameState.text.toString().trim()
        if (isCreationTriggered || sanitizedName.isEmpty()) {
            return
        }
        isCreationTriggered = true
        viewModelScope.launch {
            val playlistUuid = playlistManager.createManualPlaylist(sanitizedName)
            val uuids = episodeUuids.map(EpisodeUuidPair::episodeUuid)
            playlistManager.addManualEpisodes(playlistUuid, uuids)
            tracker.track(AnalyticsEvent.FILTER_CREATED)

            val playlist = CreatedPlaylist(uuid = playlistUuid, title = sanitizedName)
            _createdPlaylist.complete(playlist)
        }
    }

    fun addToPlaylist(playlistUuid: String) {
        cachePlaylistChange(playlistUuid, shouldAdd = true)

        viewModelScope.launch(Dispatchers.Default) {
            previewsFlow.update { previews ->
                previews?.map { preview ->
                    if (preview.uuid == playlistUuid) {
                        val newUuids = preview.episodeUuids + episodeUuids.map(EpisodeUuidPair::episodeUuid)
                        preview.copy(episodeUuids = newUuids)
                    } else {
                        preview
                    }
                }
            }
        }
    }

    fun removeFromPlaylist(playlistUuid: String) {
        cachePlaylistChange(playlistUuid, shouldAdd = false)

        viewModelScope.launch(Dispatchers.Default) {
            previewsFlow.update { previews ->
                previews?.map { preview ->
                    if (preview.uuid == playlistUuid) {
                        val newUuids = preview.episodeUuids - episodeUuids.mapTo(mutableSetOf(), EpisodeUuidPair::episodeUuid)
                        preview.copy(episodeUuids = newUuids)
                    } else {
                        preview
                    }
                }
            }
        }
    }

    fun commitPlaylistChanges() {
        viewModelScope.launch(NonCancellable) {
            val uuids = episodeUuids.map(EpisodeUuidPair::episodeUuid)
            for ((playlistUuid, isAdded) in playlistsChanges) {
                if (isAdded) {
                    playlistManager.addManualEpisodes(playlistUuid, uuids)
                } else {
                    playlistManager.deleteManualEpisodes(playlistUuid, uuids)
                }
            }
        }
    }

    fun trackScreenShown() {
        tracker.track(
            AnalyticsEvent.ADD_TO_PLAYLISTS_SHOWN,
            mapOf("source" to source.analyticsValue),
        )
    }

    fun trackEpisodeAddTapped(
        playlist: PlaylistPreviewForEpisode,
        isPlaylistFull: Boolean,
    ) {
        tracker.track(
            AnalyticsEvent.ADD_TO_PLAYLISTS_EPISODE_ADD_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
                "is_playlist_full" to isPlaylistFull,
            ),
        )
        if (!isPlaylistFull) {
            when (episodeUuids.size) {
                1 -> {
                    val uuids = episodeUuids.first()
                    tracker.track(
                        AnalyticsEvent.EPISODE_ADDED_TO_LIST,
                        buildMap {
                            put("playlist_name", playlist.title)
                            put("playlist_uuid", playlist.uuid)
                            put("episode_uuid", uuids.episodeUuid)
                            put("podcast_uuid", uuids.podcastUuid)
                            put("source", source.episodeEditAnalyticsValue)
                        },
                    )
                }

                else -> {
                    tracker.track(
                        AnalyticsEvent.EPISODE_ADDED_TO_LIST_BULK,
                        buildMap {
                            put("playlist_name", playlist.title)
                            put("playlist_uuid", playlist.uuid)
                            put("episode_count", episodeUuids.size)
                            put("source", source.episodeEditAnalyticsValue)
                        },
                    )
                }
            }
        }
    }

    fun trackEpisodeRemoveTapped(playlist: PlaylistPreviewForEpisode) {
        tracker.track(
            AnalyticsEvent.ADD_TO_PLAYLISTS_EPISODE_REMOVE_TAPPED,
            mapOf("source" to source.analyticsValue),
        )
        when (episodeUuids.size) {
            1 -> {
                val uuids = episodeUuids.first()
                tracker.track(
                    AnalyticsEvent.EPISODE_REMOVED_FROM_LIST,
                    buildMap {
                        put("playlist_name", playlist.title)
                        put("playlist_uuid", playlist.uuid)
                        put("episode_uuid", uuids.episodeUuid)
                        put("podcast_uuid", uuids.podcastUuid)
                        put("source", source.episodeEditAnalyticsValue)
                    },
                )
            }

            else -> {
                tracker.track(
                    AnalyticsEvent.EPISODE_REMOVED_FROM_LIST_BULK,
                    buildMap {
                        put("playlist_name", playlist.title)
                        put("playlist_uuid", playlist.uuid)
                        put("episode_count", episodeUuids.size)
                        put("source", source.episodeEditAnalyticsValue)
                    },
                )
            }
        }
    }

    fun trackNewPlaylistTapped() {
        tracker.track(
            AnalyticsEvent.ADD_TO_PLAYLISTS_NEW_PLAYLIST_TAPPED,
            mapOf("source" to source.analyticsValue),
        )
    }

    fun trackCreateNewPlaylistTapped() {
        tracker.track(
            AnalyticsEvent.ADD_TO_PLAYLISTS_CREATE_NEW_PLAYLIST_TAPPED,
            mapOf("source" to source.analyticsValue),
        )
    }

    data class UiState(
        val playlistPreviews: List<PlaylistPreviewForEpisode>,
        val unfilteredPlaylistsCount: Int,
        val episodeLimit: Int,
    )

    data class CreatedPlaylist(
        val uuid: String,
        val title: String,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            source: Source,
            @Assisted("uuids") episodeUuids: List<EpisodeUuidPair>,
            @Assisted("title") initialPlaylistTitle: String,
        ): AddToPlaylistViewModel
    }
}
