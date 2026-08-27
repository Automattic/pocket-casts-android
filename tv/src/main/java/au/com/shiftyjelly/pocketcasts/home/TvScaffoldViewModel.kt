package au.com.shiftyjelly.pocketcasts.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.TvLaunchRequests
import au.com.shiftyjelly.pocketcasts.auth.TvSignOutManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.ProfileShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel
class TvScaffoldViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val signOutManager: TvSignOutManager,
    private val eventHorizon: EventHorizon,
    launchRequests: TvLaunchRequests,
    upNextQueue: UpNextQueue,
) : ViewModel() {
    val openNowPlayingRequests = launchRequests.openNowPlaying

    private val selectedTab = MutableStateFlow<TvTab>(TvTab.Home)

    private val hasCurrentEpisode = upNextQueue.changesObservable.asFlow()
        .map { state -> state is UpNextQueue.State.Loaded }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val uiState: StateFlow<TvScaffoldUiState> = combine(
        selectedTab,
        hasCurrentEpisode,
        syncManager.isLoggedInObservable.asFlow(),
        syncManager.emailFlow(),
    ) { tab, hasEpisode, isLoggedIn, email ->
        TvScaffoldUiState(
            tabs = TvTab.tabs(showNowPlaying = hasEpisode || tab == TvTab.NowPlaying),
            selectedTab = tab,
            profile = profileState(isLoggedIn, email),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TvScaffoldUiState(profile = currentProfileState()),
    )

    init {
        viewModelScope.launch { leaveNowPlayingWhenEpisodeCleared() }
    }

    fun selectTab(tab: TvTab) {
        selectedTab.value = tab
    }

    fun openNowPlaying() {
        selectedTab.value = TvTab.NowPlaying
    }

    fun signOut() {
        signOutManager.signOutAndWipeData()
    }

    fun trackProfileShown() {
        eventHorizon.track(ProfileShownEvent)
    }

    @OptIn(FlowPreview::class)
    private suspend fun leaveNowPlayingWhenEpisodeCleared() {
        hasCurrentEpisode
            .drop(1)
            .debounce(NOW_PLAYING_EMPTY_DEBOUNCE_MS)
            .collect { hasEpisode ->
                if (!hasEpisode && selectedTab.value == TvTab.NowPlaying) {
                    selectedTab.value = TvTab.Home
                }
            }
    }

    private fun currentProfileState() = profileState(syncManager.isLoggedIn(), syncManager.getEmail())

    private fun profileState(isLoggedIn: Boolean, email: String?) = if (isLoggedIn) {
        TvProfileState.SignedIn(email = email?.takeIf(String::isNotBlank))
    } else {
        TvProfileState.SignedOut
    }
}

private const val NOW_PLAYING_EMPTY_DEBOUNCE_MS = 500L

data class TvScaffoldUiState(
    val tabs: List<TvTab> = TvTab.entries,
    val selectedTab: TvTab = TvTab.Home,
    val profile: TvProfileState = TvProfileState.SignedOut,
) {
    val selectedTabIndex: Int get() = tabs.indexOf(selectedTab).coerceAtLeast(0)
}

sealed interface TvProfileState {
    data object SignedOut : TvProfileState
    data class SignedIn(val email: String?) : TvProfileState
}
