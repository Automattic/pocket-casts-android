package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.text.SearchFieldState
import au.com.shiftyjelly.pocketcasts.models.to.toPodcastEpisodes
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = PlaylistViewModel.Factory::class)
class PlaylistViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    @Assisted private val playlistType: Playlist.Type,
    private val playlistManager: PlaylistManager,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val downloadManager: DownloadManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private var isNameChanged = false
    private var isAutoDownloadChanged = false
    private var isAutoDownloadLimitChanged = false

    val bottomInset = settings.bottomInset

    private val _startMultiSelectingSignal = MutableSharedFlow<Unit>()
    val startMultiSelectingSignal = _startMultiSelectingSignal.asSharedFlow()

    private val _chromeCastSignal = MutableSharedFlow<Unit>()
    val chromeCastSignal = _chromeCastSignal.asSharedFlow()

    private val _showSettingsSignal = MutableSharedFlow<Unit>()
    val showSettingsSignal = _showSettingsSignal.asSharedFlow()

    val searchState = SearchFieldState()

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

    fun shouldShowPlayAllWarning(): Boolean {
        val queueEpisodes = playbackManager.upNextQueue.allEpisodes
        return queueEpisodes.size >= PLAY_ALL_WARNING_EPISODE_COUNT
    }

    private var playAllJob: Job? = null

    fun playAll() {
        if (playAllJob?.isActive == true) {
            return
        }
        playAllJob = viewModelScope.launch(Dispatchers.Default) {
            val episodes = uiState.value.playlist
                ?.episodes
                ?.takeIf { it.isNotEmpty() }
                ?.toPodcastEpisodes()
                ?: return@launch
            playbackManager.upNextQueue.removeAll()
            playbackManager.playEpisodes(episodes, SourceView.FILTERS)
            episodeManager.unarchiveAllInListBlocking(episodes)
        }
    }

    fun downloadAll() {
        val episodes = uiState.value.playlist
            ?.episodes
            ?.take(DOWNLOAD_ALL_LIMIT)
            ?.toPodcastEpisodes()
        episodes?.forEach { episode ->
            downloadManager.addEpisodeToQueue(episode, "filter download all", fireEvent = false, source = SourceView.FILTERS)
        }
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

    fun trackFilterShown() {
        analyticsTracker.track(AnalyticsEvent.FILTER_SHOWN)
    }

    fun trackAddEpisodesTapped() {
        val episodeCount = uiState.value.playlist?.metadata?.totalEpisodeCount ?: Int.MAX_VALUE
        analyticsTracker.track(
            AnalyticsEvent.FILTER_ADD_EPISODES_TAPPED,
            mapOf("is_playlist_full" to (episodeCount >= PlaylistManager.MANUAL_PLAYLIST_EPISODE_LIMIT)),
        )
    }

    fun trackEditRulesTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_EDIT_RULES_TAPPED)
    }

    fun trackPlayAllTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_PLAY_ALL_TAPPED)
    }

    fun trackSelectEpisodesTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_SELECT_EPISODES_TAPPED)
    }

    fun trackSortByTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_SORT_BY_TAPPED)
    }

    fun trackDownloadAllTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_DOWNLOAD_ALL_TAPPED)
    }

    fun trackChromeCastTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_CHROME_CAST_TAPPED)
    }

    fun trackFilterOptionsTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_OPTIONS_TAPPED)
    }

    fun trackSortByChanged(type: PlaylistEpisodeSortType) {
        analyticsTracker.track(
            AnalyticsEvent.FILTER_SORT_BY_CHANGED,
            mapOf("sort_order" to type.analyticsValue),
        )
    }

    fun trackAutoDownloadChanged(isEnabled: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.FILTER_AUTO_DOWNLOAD_UPDATED,
            mapOf(
                "source" to "filters",
                "enabled" to isEnabled,
            ),
        )
    }

    fun trackAutoDownloadLimitChanged(limit: Int) {
        analyticsTracker.track(
            AnalyticsEvent.FILTER_AUTO_DOWNLOAD_LIMIT_UPDATED,
            mapOf("limit" to limit),
        )
    }

    fun trackEditDismissed() {
        analyticsTracker.track(
            AnalyticsEvent.FILTER_EDIT_DISMISSED,
            mapOf(
                "did_change_name" to isNameChanged,
                "did_change_auto_download" to isAutoDownloadChanged,
                "did_change_episode_count" to isAutoDownloadLimitChanged,
            ),
        )
    }

    fun trackArchiveAllTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_ARCHIVE_ALL_TAPPED)
    }

    fun trackUnarchiveAllTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_UNARCHIVE_ALL_TAPPED)
    }

    fun trackRearrangeEpisodesTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_REARRANGE_EPISODES_TAPPED)
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
        analyticsTracker.track(AnalyticsEvent.FILTER_SHOW_ARCHIVED_TAPPED)
    }

    fun trackHideArchivedTapped() {
        analyticsTracker.track(AnalyticsEvent.FILTER_HIDE_ARCHIVED_TAPPED)
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
        private const val PLAY_ALL_WARNING_EPISODE_COUNT = 4
    }
}
