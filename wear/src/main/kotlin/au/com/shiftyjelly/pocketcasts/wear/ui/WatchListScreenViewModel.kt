package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.WearMainListDownloadsTappedEvent
import com.automattic.eventhorizon.WearMainListFilesTappedEvent
import com.automattic.eventhorizon.WearMainListFiltersTappedEvent
import com.automattic.eventhorizon.WearMainListNowPlayingTappedEvent
import com.automattic.eventhorizon.WearMainListPodcastsTappedEvent
import com.automattic.eventhorizon.WearMainListSettingsTappedEvent
import com.automattic.eventhorizon.WearMainListShownEvent
import com.automattic.eventhorizon.WearMainListStarredTappedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel
class WatchListScreenViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
    episodeManager: EpisodeManager,
    playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
) : ViewModel() {

    data class State(
        val upNextQueue: UpNextQueue.State? = null,
        val refreshState: RefreshState = RefreshState.Never,
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
    }

    fun onShown() {
        eventHorizon.track(WearMainListShownEvent)
    }

    fun onNowPlayingClicked() {
        eventHorizon.track(WearMainListNowPlayingTappedEvent)
    }

    fun onPodcastsClicked() {
        eventHorizon.track(WearMainListPodcastsTappedEvent)
    }

    fun onDownloadsClicked() {
        eventHorizon.track(WearMainListDownloadsTappedEvent)
    }

    fun onPlaylistsClicked() {
        eventHorizon.track(WearMainListFiltersTappedEvent)
    }

    fun onFilesClicked() {
        eventHorizon.track(WearMainListFilesTappedEvent)
    }

    fun onStarredClicked() {
        eventHorizon.track(WearMainListStarredTappedEvent)
    }

    fun onSettingsClicked() {
        eventHorizon.track(WearMainListSettingsTappedEvent)
    }

    fun refreshPodcasts() {
        // Prevent multiple simultaneous refresh requests
        if (_state.value.refreshState is RefreshState.Refreshing) {
            return
        }
        podcastManager.refreshPodcasts("watch - list screen")
    }
}
