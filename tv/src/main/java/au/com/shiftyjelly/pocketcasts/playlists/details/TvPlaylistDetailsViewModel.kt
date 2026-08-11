package au.com.shiftyjelly.pocketcasts.playlists.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.to.toPodcastEpisodes
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.preferences.TvPreferences
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayAllHandler
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayAllResponse
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel(assistedFactory = TvPlaylistDetailsViewModel.Factory::class)
class TvPlaylistDetailsViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    @Assisted private val playlistType: Playlist.Type,
    private val playlistManager: PlaylistManager,
    private val preferences: TvPreferences,
    playAllHandlerFactory: PlayAllHandler.Factory,
) : ViewModel() {

    private val playAllHandler = playAllHandlerFactory.create(SourceView.FILTERS)

    private val _events = MutableSharedFlow<TvPlaylistDetailsEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<TvPlaylistDetailsEvent> = _events.asSharedFlow()

    private var playAllJob: Job? = null
    private var replaceUpNextJob: Job? = null

    private val isBusy get() = playAllJob?.isActive == true || replaceUpNextJob?.isActive == true

    private val playlistFlow: Flow<Playlist?> = when (playlistType) {
        Playlist.Type.Manual -> playlistManager.manualPlaylistFlow(playlistUuid, includeArchived = true)
        Playlist.Type.Smart -> playlistManager.smartPlaylistFlow(playlistUuid, includeArchived = true)
    }

    private val isShowingArchivedFlow = MutableStateFlow(preferences.isPlaylistShowingArchived(playlistUuid))

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
        preferences.setPlaylistShowingArchived(playlistUuid, isShowingArchived)
        isShowingArchivedFlow.value = isShowingArchived
    }

    fun playAll() {
        if (isBusy) {
            return
        }
        playAllJob = viewModelScope.launch {
            try {
                val episodes = (uiState.value as? TvPlaylistDetailsUiState.Loaded)?.episodes.orEmpty()
                when (playAllHandler.handlePlayAllEpisodes(episodes)) {
                    PlayAllResponse.DoNothing -> _events.tryEmit(TvPlaylistDetailsEvent.OpenNowPlaying)
                    PlayAllResponse.ShowWarning -> _events.tryEmit(TvPlaylistDetailsEvent.ShowReplaceUpNextConfirmation)
                    PlayAllResponse.ShowNoEpisodesToPlay -> _events.tryEmit(TvPlaylistDetailsEvent.ShowNoEpisodesToPlay)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to play all episodes on TV")
            }
        }
    }

    fun replaceUpNextAndPlay(saveUpNext: Boolean, upNextName: String) {
        if (isBusy) {
            return
        }
        replaceUpNextJob = viewModelScope.launch {
            val played = withContext(NonCancellable) {
                if (saveUpNext) {
                    val saved = try {
                        playAllHandler.saveUpNextAsPlaylist(upNextName)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to save Up Next as a playlist on TV")
                        false
                    }
                    if (saved) {
                        _events.tryEmit(TvPlaylistDetailsEvent.ShowUpNextSavedToast)
                    }
                }
                try {
                    playAllHandler.playAllPendingEpisodes()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to play the Up Next queue on TV")
                    false
                }
            }
            if (played) {
                _events.tryEmit(TvPlaylistDetailsEvent.OpenNowPlaying)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String, playlistType: Playlist.Type): TvPlaylistDetailsViewModel
    }
}

sealed interface TvPlaylistDetailsEvent {
    data object OpenNowPlaying : TvPlaylistDetailsEvent

    data object ShowReplaceUpNextConfirmation : TvPlaylistDetailsEvent

    data object ShowUpNextSavedToast : TvPlaylistDetailsEvent

    data object ShowNoEpisodesToPlay : TvPlaylistDetailsEvent
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
