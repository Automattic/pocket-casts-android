package au.com.shiftyjelly.pocketcasts.playlists.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.to.toPodcastEpisodes
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = TvPlaylistDetailsViewModel.Factory::class)
class TvPlaylistDetailsViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    @Assisted private val playlistType: Playlist.Type,
    private val playlistManager: PlaylistManager,
    private val preferences: TvPlaylistPreferences,
) : ViewModel() {

    private val playlistFlow: Flow<Playlist?> = when (playlistType) {
        Playlist.Type.Manual -> playlistManager.manualPlaylistFlow(playlistUuid, includeArchived = true)
        Playlist.Type.Smart -> playlistManager.smartPlaylistFlow(playlistUuid, includeArchived = true)
    }

    private val isShowingArchivedFlow = MutableStateFlow(preferences.isShowingArchived(playlistUuid))

    val uiState: StateFlow<TvPlaylistDetailsUiState> = combine(
        playlistFlow,
        isShowingArchivedFlow,
    ) { playlist, isShowingArchived ->
        if (playlist == null) {
            TvPlaylistDetailsUiState.NotFound
        } else {
            val allEpisodes = playlist.episodes.toPodcastEpisodes()
            TvPlaylistDetailsUiState.Loaded(
                playlist = playlist,
                episodes = if (isShowingArchived) allEpisodes else allEpisodes.filterNot(PodcastEpisode::isArchived),
                isShowingArchivedOnDevice = isShowingArchived,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeout = 300.milliseconds, replayExpiration = Duration.ZERO),
        TvPlaylistDetailsUiState.Loading,
    )

    fun changeSortType(sortType: PlaylistEpisodeSortType) {
        viewModelScope.launch {
            playlistManager.updateSortType(playlistUuid, sortType)
        }
    }

    fun toggleArchiveFilter() {
        val isShowingArchived = !isShowingArchivedFlow.value
        preferences.setShowingArchived(playlistUuid, isShowingArchived)
        isShowingArchivedFlow.value = isShowingArchived
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String, playlistType: Playlist.Type): TvPlaylistDetailsViewModel
    }
}

sealed interface TvPlaylistDetailsUiState {
    data object Loading : TvPlaylistDetailsUiState

    data object NotFound : TvPlaylistDetailsUiState

    data class Loaded(
        val playlist: Playlist,
        val episodes: List<PodcastEpisode>,
        val isShowingArchivedOnDevice: Boolean,
    ) : TvPlaylistDetailsUiState {
        val availableEpisodeCount get() = playlist.episodes.count { it is PlaylistEpisode.Available }
    }
}
