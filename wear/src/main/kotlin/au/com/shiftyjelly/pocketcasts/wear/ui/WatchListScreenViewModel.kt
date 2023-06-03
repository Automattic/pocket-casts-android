package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

@HiltViewModel
class WatchListScreenViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    episodeManager: EpisodeManager,
    playbackManager: PlaybackManager,
    podcastManager: PodcastManager,
) : ViewModel() {

    data class State(
        val upNextQueue: UpNextQueue.State? = null,
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

    fun onFiltersClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_FILTERS_TAPPED)
    }

    fun onFilesClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_FILES_TAPPED)
    }

    fun onSettingsClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_MAIN_LIST_SETTINGS_TAPPED)
    }
}
