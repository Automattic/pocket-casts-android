package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.wear.networking.ConnectivityStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber

@HiltViewModel
class WatchListScreenViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val settings: Settings,
    episodeManager: EpisodeManager,
    playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    connectivityStateManager: ConnectivityStateManager,
) : ViewModel() {

    data class State(
        val upNextQueue: UpNextQueue.State? = null,
        val refreshState: RefreshState = RefreshState.Never,
        val isConnected: Boolean = true,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            playbackManager
                .upNextQueue
                .getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)
                .asFlow()
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)
                .collect { upNextQueue ->
                    _state.update {
                        it.copy(upNextQueue = upNextQueue)
                    }
                }
        }

        viewModelScope.launch {
            settings.refreshStateFlow.collect { refreshState ->
                _state.update {
                    it.copy(refreshState = refreshState)
                }
            }
        }

        viewModelScope.launch {
            connectivityStateManager.isConnected.collect { isConnected ->
                Timber.d("WatchListScreenViewModel: connectivity changed to $isConnected")
                _state.update {
                    it.copy(isConnected = isConnected)
                }
            }
        }
    }

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_SHOWN)
    }

    fun onNowPlayingClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_NOW_PLAYING_TAPPED)
    }

    fun onPodcastsClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_PODCASTS_TAPPED)
    }

    fun onDownloadsClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_DOWNLOADS_TAPPED)
    }

    fun onPlaylistsClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_FILTERS_TAPPED)
    }

    fun onFilesClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_FILES_TAPPED)
    }

    fun onStarredClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_STARRED_TAPPED)
    }

    fun onSettingsClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_SETTINGS_TAPPED)
    }

    fun refreshPodcasts() {
        // Prevent multiple simultaneous refresh requests
        if (_state.value.refreshState is RefreshState.Refreshing) {
            return
        }
        podcastManager.refreshPodcasts("watch - list screen")
    }
}
