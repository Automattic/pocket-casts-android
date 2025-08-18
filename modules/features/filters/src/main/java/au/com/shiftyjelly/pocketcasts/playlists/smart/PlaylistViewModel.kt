package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylist
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = PlaylistViewModel.Factory::class)
class PlaylistViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    private val playlistManager: PlaylistManager,
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

    val searchState = TextFieldState()

    val uiState = snapshotFlow { searchState.text }
        .map { it.toString().trim() }
        .debounce { searchTerm -> if (searchTerm.isEmpty()) 0 else 300 }
        .distinctUntilChanged()
        .flatMapLatest { searchTerm -> playlistManager.observeSmartPlaylist(playlistUuid, searchTerm) }
        .map { UiState(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = UiState.Empty)

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
            val episodes = uiState.value.smartPlaylist?.episodes?.takeIf { it.isNotEmpty() } ?: return@launch
            playbackManager.upNextQueue.removeAll()
            playbackManager.playEpisodes(episodes, SourceView.FILTERS)
        }
    }

    fun downloadAll() {
        val episodes = uiState.value.smartPlaylist?.episodes?.take(DOWNLOAD_ALL_LIMIT)
        episodes?.forEach { episode ->
            downloadManager.addEpisodeToQueue(episode, "filter download all", fireEvent = false, source = SourceView.FILTERS)
        }
    }

    fun updateSortType(type: PlaylistEpisodeSortType) {
        viewModelScope.launch(NonCancellable) {
            playlistManager.updateSortType(playlistUuid, type)
            trackSortByChanged(type)
        }
    }

    fun updateAutoDownload(isEnabled: Boolean) {
        viewModelScope.launch(NonCancellable) {
            playlistManager.updateAutoDownload(playlistUuid, isEnabled)
            isAutoDownloadChanged = true
            trackAutoDownloadChanged(isEnabled)
        }
    }

    fun updateAutoDownloadLimit(limit: Int) {
        viewModelScope.launch(NonCancellable) {
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
        viewModelScope.launch(NonCancellable) {
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

    fun showSettings() {
        viewModelScope.launch {
            isNameChanged = false
            isAutoDownloadChanged = false
            isAutoDownloadLimitChanged = false
            _showSettingsSignal.emit(Unit)
        }
    }

    fun trackFilterShown() {
        analyticsTracker.track(AnalyticsEvent.FILTER_SHOWN)
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

    data class UiState(
        val smartPlaylist: SmartPlaylist?,
    ) {
        companion object {
            val Empty = UiState(
                smartPlaylist = null,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String): PlaylistViewModel
    }

    companion object {
        const val DOWNLOAD_ALL_LIMIT = 100
        private const val PLAY_ALL_WARNING_EPISODE_COUNT = 4
    }
}
