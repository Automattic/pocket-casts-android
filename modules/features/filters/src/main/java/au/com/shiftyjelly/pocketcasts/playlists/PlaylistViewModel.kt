package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.compose.text.SearchFieldState
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.toPodcastEpisodes
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.preferences.model.SelectedPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayAllHandler
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayAllResponse
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EpisodeRemovedFromListEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.FilterAddEpisodesCtaEmptyTappedEvent
import com.automattic.eventhorizon.FilterAddEpisodesTappedEvent
import com.automattic.eventhorizon.FilterArchiveAllTappedEvent
import com.automattic.eventhorizon.FilterAutoDownloadLimitUpdatedEvent
import com.automattic.eventhorizon.FilterAutoDownloadUpdatedEvent
import com.automattic.eventhorizon.FilterBrowseShowsCtaEmptyTappedEvent
import com.automattic.eventhorizon.FilterChromeCastTappedEvent
import com.automattic.eventhorizon.FilterDeleteDismissedEvent
import com.automattic.eventhorizon.FilterDeleteTriggeredEvent
import com.automattic.eventhorizon.FilterDeletedEvent
import com.automattic.eventhorizon.FilterDownloadAllTappedEvent
import com.automattic.eventhorizon.FilterEditDismissedEvent
import com.automattic.eventhorizon.FilterEditRulesCtaEmptyTappedEvent
import com.automattic.eventhorizon.FilterEditRulesTappedEvent
import com.automattic.eventhorizon.FilterHideArchivedTappedEvent
import com.automattic.eventhorizon.FilterNameUpdatedEvent
import com.automattic.eventhorizon.FilterOptionsButtonTappedEvent
import com.automattic.eventhorizon.FilterOptionsTappedEvent
import com.automattic.eventhorizon.FilterPlayAllDismissedEvent
import com.automattic.eventhorizon.FilterPlayAllReplaceAndPlayTappedEvent
import com.automattic.eventhorizon.FilterPlayAllTappedEvent
import com.automattic.eventhorizon.FilterRearrangeEpisodesTappedEvent
import com.automattic.eventhorizon.FilterSelectEpisodesTappedEvent
import com.automattic.eventhorizon.FilterShowArchivedCtaEmptyTappedEvent
import com.automattic.eventhorizon.FilterShowArchivedTappedEvent
import com.automattic.eventhorizon.FilterShownEvent
import com.automattic.eventhorizon.FilterSortByChangedEvent
import com.automattic.eventhorizon.FilterSortByTappedEvent
import com.automattic.eventhorizon.FilterUnarchiveAllTappedEvent
import com.automattic.eventhorizon.PlaylistRemoveEpisodeSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = PlaylistViewModel.Factory::class)
class PlaylistViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    @Assisted private val playlistType: Playlist.Type,
    private val playlistManager: PlaylistManager,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val downloadQueue: DownloadQueue,
    private val settings: Settings,
    playAllHandlerFactory: PlayAllHandler.Factory,
    private val eventHorizon: EventHorizon,
) : ViewModel() {
    private val playAllHandler = playAllHandlerFactory.create(SourceView.FILTERS)

    private var isNameChanged = false
    private var isAutoDownloadChanged = false
    private var isAutoDownloadLimitChanged = false

    val bottomInset = settings.bottomInset

    private val _startMultiSelectingSignal = MutableSharedFlow<Unit>()
    val startMultiSelectingSignal = _startMultiSelectingSignal.asSharedFlow()

    private val _chromeCastSignal = MutableSharedFlow<Unit>()
    val chromeCastSignal = _chromeCastSignal.asSharedFlow()

    private val _playAllResponseSignal = MutableSharedFlow<PlayAllResponse>()
    val playAllResponseSignal = _playAllResponseSignal.asSharedFlow()

    private val _showSettingsSignal = MutableSharedFlow<Unit>()
    val showSettingsSignal = _showSettingsSignal.asSharedFlow()

    private val _upNextSavedAsPlaylistSignal = MutableSharedFlow<Unit>()
    val upNextSavedAsPlaylistSignal = _upNextSavedAsPlaylistSignal.asSharedFlow()

    val searchState = SearchFieldState()

    val saveUpNextDefaultValue get() = settings.saveUpNextAsPlaylist.value

    private val playlistFlow = searchState.textFlow.flatMapLatest { searchTerm ->
        when (playlistType) {
            Playlist.Type.Manual -> playlistManager.manualPlaylistFlow(playlistUuid, searchTerm)
            Playlist.Type.Smart -> playlistManager.smartPlaylistFlow(playlistUuid, searchTerm)
        }
    }

    val uiState = combine(
        playlistFlow,
        podcastManager.countSubscribedFlow(),
    ) { playlist, followedCount ->
        UiState(
            playlist = playlist,
            isAnyPodcastFollowed = followedCount > 0,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, initialValue = UiState.Empty)

    private var resolvePlayAllJob: Job? = null

    fun handlePlayAllAction() {
        if (resolvePlayAllJob?.isActive == true) {
            return
        }
        resolvePlayAllJob = viewModelScope.launch {
            val playlistEpisodes = uiState.value.playlist?.episodes.orEmpty()
            val response = playAllHandler.handlePlayAllAction(playlistEpisodes)
            _playAllResponseSignal.emit(response)
        }
    }

    private var saveUpNextJob: Job? = null
    private var playAllJob: Job? = null

    fun saveUpNextAsPlaylist(saveUpNext: Boolean, upNextTranslation: String) {
        eventHorizon.track(
            FilterPlayAllReplaceAndPlayTappedEvent(
                filterType = playlistType.eventHorizonValue,
                saveUpNext = saveUpNext,
            ),
        )
        settings.saveUpNextAsPlaylist.set(saveUpNext, updateModifiedAt = false)
        if (saveUpNext && saveUpNextJob?.isActive != true) {
            saveUpNextJob = viewModelScope.launch(NonCancellable) {
                playAllHandler.saveUpNextAsPlaylist(upNextTranslation)
                _upNextSavedAsPlaylistSignal.emit(Unit)
            }
        }
        if (playAllJob?.isActive != true) {
            playAllJob = viewModelScope.launch(NonCancellable) {
                playAllHandler.playAllPendingEpisodes()
            }
        }
    }

    fun downloadAll() {
        val episodes = uiState.value.playlist
            ?.episodes
            ?.toPodcastEpisodes()
            ?.take(DOWNLOAD_ALL_LIMIT)
            ?.map(PodcastEpisode::uuid)
            .orEmpty()
        downloadQueue.enqueueAll(episodes, DownloadType.UserTriggered(waitForWifi = false), SourceView.FILTERS)
    }

    fun deleteEpisode(episodeUuid: String) {
        viewModelScope.launch {
            playlistManager.deleteManualEpisode(playlistUuid, episodeUuid)
        }
    }

    fun updateSortType(type: PlaylistEpisodeSortType) {
        viewModelScope.launch {
            playlistManager.updateSortType(playlistUuid, type)
            trackSortByChanged(type)
        }
    }

    fun updateAutoDownload(isEnabled: Boolean) {
        viewModelScope.launch {
            playlistManager.updateAutoDownload(playlistUuid, isEnabled)
            isAutoDownloadChanged = true
            trackAutoDownloadChanged(isEnabled)
        }
    }

    fun updateAutoDownloadLimit(limit: Int) {
        viewModelScope.launch {
            playlistManager.updateAutoDownloadLimit(playlistUuid, limit)
            isAutoDownloadLimitChanged = true
            trackAutoDownloadLimitChanged(limit)
        }
    }

    fun updateName(name: String) {
        val sanitizedName = name.trim()
        if (sanitizedName.isEmpty()) {
            return
        }
        viewModelScope.launch {
            isNameChanged = true
            playlistManager.updateName(playlistUuid, sanitizedName)
        }
    }

    fun startMultiSelecting() {
        viewModelScope.launch {
            _startMultiSelectingSignal.emit(Unit)
        }
    }

    fun startChromeCast() {
        viewModelScope.launch {
            _chromeCastSignal.emit(Unit)
        }
    }

    fun archiveAllEpisodes() {
        viewModelScope.launch(Dispatchers.Default) {
            val episodes = uiState.value.playlist
                ?.episodes
                ?.toPodcastEpisodes()
                .orEmpty()

            if (episodes.isNotEmpty()) {
                episodeManager.archiveAllInList(episodes, playbackManager)
            }
        }
    }

    fun unarchiveAllEpisodes() {
        viewModelScope.launch(Dispatchers.Default) {
            val episodes = uiState.value.playlist
                ?.episodes
                ?.toPodcastEpisodes()
                .orEmpty()

            if (episodes.isNotEmpty()) {
                episodeManager.unarchiveAllInListBlocking(episodes)
            }
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch(NonCancellable) {
            delay(300) // Some small delay to navigate back to the main UI first.
            playlistManager.deletePlaylist(playlistUuid)
            eventHorizon.track(
                FilterDeletedEvent(
                    filterType = playlistType.eventHorizonValue,
                ),
            )
        }
    }

    fun showSettings() {
        viewModelScope.launch {
            isNameChanged = false
            isAutoDownloadChanged = false
            isAutoDownloadLimitChanged = false
            _showSettingsSignal.emit(Unit)
        }
    }

    fun toggleShowArchived() {
        viewModelScope.launch {
            playlistManager.toggleShowArchived(playlistUuid)
        }
    }

    fun trackDeleteTriggered() {
        eventHorizon.track(
            FilterDeleteTriggeredEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackDeleteDismissed() {
        eventHorizon.track(
            FilterDeleteDismissedEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackFilterShown() {
        eventHorizon.track(
            FilterShownEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackAddEpisodesTapped() {
        val episodeCount = uiState.value.playlist?.metadata?.totalEpisodeCount ?: Int.MAX_VALUE
        eventHorizon.track(
            FilterAddEpisodesTappedEvent(
                isPlaylistFull = episodeCount >= PlaylistManager.MANUAL_PLAYLIST_EPISODE_LIMIT,
            ),
        )
    }

    fun trackEditRulesTapped() {
        eventHorizon.track(FilterEditRulesTappedEvent)
    }

    fun trackPlayAllTapped() {
        eventHorizon.track(
            FilterPlayAllTappedEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackPlayAllDismissed() {
        eventHorizon.track(
            FilterPlayAllDismissedEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackSelectEpisodesTapped() {
        eventHorizon.track(
            FilterSelectEpisodesTappedEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackSortByTapped() {
        eventHorizon.track(
            FilterSortByTappedEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackDownloadAllTapped() {
        eventHorizon.track(
            FilterDownloadAllTappedEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackChromeCastTapped() {
        eventHorizon.track(
            FilterChromeCastTappedEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackFilterOptionsButtonTapped() {
        eventHorizon.track(
            FilterOptionsButtonTappedEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackFilterOptionsTapped() {
        eventHorizon.track(
            FilterOptionsTappedEvent(
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackSortByChanged(type: PlaylistEpisodeSortType) {
        eventHorizon.track(
            FilterSortByChangedEvent(
                sortOrder = type.eventHorizonValue,
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackAutoDownloadChanged(isEnabled: Boolean) {
        eventHorizon.track(
            FilterAutoDownloadUpdatedEvent(
                source = SourceView.FILTERS.eventHorizonValue,
                enabled = isEnabled,
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackAutoDownloadLimitChanged(limit: Int) {
        eventHorizon.track(
            FilterAutoDownloadLimitUpdatedEvent(
                limit = limit.toLong(),
            ),
        )
    }

    fun trackEditDismissed() {
        if (isNameChanged) {
            eventHorizon.track(FilterNameUpdatedEvent)
        }
        eventHorizon.track(
            FilterEditDismissedEvent(
                didChangeName = isNameChanged,
                didChangeAutoDownload = isAutoDownloadChanged,
                didChangeEpisodeCount = isAutoDownloadLimitChanged,
                filterType = playlistType.eventHorizonValue,
            ),
        )
    }

    fun trackArchiveAllTapped() {
        eventHorizon.track(FilterArchiveAllTappedEvent)
    }

    fun trackUnarchiveAllTapped() {
        eventHorizon.track(FilterUnarchiveAllTappedEvent)
    }

    fun trackRearrangeEpisodesTapped() {
        eventHorizon.track(FilterRearrangeEpisodesTappedEvent)
    }

    fun trackToggleShowArchived() {
        val isShowingArchived = uiState.value.playlist?.metadata?.isShowingArchived == true
        if (isShowingArchived) {
            trackHideArchivedTapped()
        } else {
            trackShowArchivedTapped()
        }
    }

    fun trackShowArchivedTapped() {
        eventHorizon.track(FilterShowArchivedTappedEvent)
    }

    fun trackHideArchivedTapped() {
        eventHorizon.track(FilterHideArchivedTappedEvent)
    }

    fun trackAddEpisodeCtaTapped() {
        eventHorizon.track(FilterAddEpisodesCtaEmptyTappedEvent)
    }

    fun trackBrowseShowsCtaTapped() {
        eventHorizon.track(FilterBrowseShowsCtaEmptyTappedEvent)
    }

    fun trackEditRulesCtaTapped() {
        eventHorizon.track(FilterEditRulesCtaEmptyTappedEvent)
    }

    fun trackShowArchivedCtaTapped() {
        eventHorizon.track(FilterShowArchivedCtaEmptyTappedEvent)
    }

    fun trackDeleteUnavailableEpisode(episodeUuid: String, podcastUuid: String) {
        val playlistName = uiState.value.playlist?.title
        eventHorizon.track(
            EpisodeRemovedFromListEvent(
                playlistName = playlistName ?: Tracker.INVALID_OR_NULL_VALUE,
                playlistUuid = playlistUuid,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                source = PlaylistRemoveEpisodeSource.UnavailableEpisode,
            ),
        )
    }

    fun updateAutoPlaySource() {
        settings.trackingAutoPlaySource.set(AutoPlaySource.fromId(playlistUuid), updateModifiedAt = false)
    }

    fun setSelectedPlaylist() {
        val playlist = SelectedPlaylist(playlistUuid, playlistType.analyticsValue)
        settings.selectedPlaylist.set(playlist, updateModifiedAt = false)
    }

    fun clearSelectedPlaylist() {
        if (settings.selectedPlaylist.value?.uuid == playlistUuid) {
            settings.selectedPlaylist.set(null, updateModifiedAt = false)
        }
    }

    data class UiState(
        val playlist: Playlist?,
        val isAnyPodcastFollowed: Boolean,
    ) {
        companion object {
            val Empty = UiState(
                playlist = null,
                isAnyPodcastFollowed = false,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            playlistUuid: String,
            playlistType: Playlist.Type,
        ): PlaylistViewModel
    }

    companion object {
        const val DOWNLOAD_ALL_LIMIT = 100
    }
}
