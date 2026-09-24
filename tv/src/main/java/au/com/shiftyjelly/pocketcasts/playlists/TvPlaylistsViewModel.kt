package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.FilterCreateButtonTappedEvent
import com.automattic.eventhorizon.FilterListShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TvPlaylistsViewModel @Inject constructor(
    private val playlistManager: PlaylistManager,
    private val podcastManager: PodcastManager,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    val uiState: StateFlow<TvPlaylistsUiState> = playlistManager.playlistPreviewsFlow()
        .map<List<PlaylistPreview>, TvPlaylistsUiState> { previews ->
            TvPlaylistsUiState.Loaded(previews.sortedBy(::isDownloadPlaylist))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 300.milliseconds), TvPlaylistsUiState.Loading)

    fun getArtworkUuidsFlow(playlistUuid: String): StateFlow<List<String>?> {
        return playlistManager.getArtworkUuidsFlow(playlistUuid)
    }

    fun getEpisodeCountFlow(playlistUuid: String): StateFlow<Int?> {
        return playlistManager.getEpisodeCountFlow(playlistUuid)
    }

    suspend fun refreshArtworkUuids(playlistUuid: String) {
        playlistManager.refreshArtworkUuids(playlistUuid)
    }

    suspend fun refreshEpisodeCount(playlistUuid: String) {
        playlistManager.refreshEpisodeCount(playlistUuid)
    }

    suspend fun findPodcastTint(podcastUuid: String): Int? {
        return podcastManager.findPodcastByUuid(podcastUuid)?.tintColorForLightBg?.takeIf { it != 0 }
    }

    fun trackPlaylistsListShown(filterCount: Int) {
        eventHorizon.track(FilterListShownEvent(filterCount = filterCount.toLong()))
    }

    fun trackCreateButtonTapped() {
        eventHorizon.track(FilterCreateButtonTappedEvent)
    }

    private fun isDownloadPlaylist(preview: PlaylistPreview): Boolean {
        return preview is SmartPlaylistPreview && preview.smartRules.downloadStatus == SmartRules.DownloadStatusRule.Downloaded
    }
}

sealed interface TvPlaylistsUiState {
    data object Loading : TvPlaylistsUiState
    data class Loaded(val playlists: List<PlaylistPreview>) : TvPlaylistsUiState
}
