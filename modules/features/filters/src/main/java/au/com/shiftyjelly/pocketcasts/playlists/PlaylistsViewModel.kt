package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistManager: PlaylistManager,
    private val userManager: UserManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val showFreeAccountBanner = combine(
        settings.isFreeAccountFiltersBannerDismissed.flow,
        userManager.getSignInState().asFlow().map { it.isSignedOut },
        FeatureFlag.isEnabledFlow(Feature.ENCOURAGE_ACCOUNT_CREATION),
    ) { isBannerDismissed, isSignedOut, isFeatureEnabled ->
        isSignedOut && !isBannerDismissed && isFeatureEnabled
    }

    internal val uiState = combine(
        playlistManager.observePlaylistsPreview(),
        settings.showPlaylistsOnboarding.flow,
        showFreeAccountBanner,
        settings.bottomInset,
        ::UiState,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Empty)

    fun deletePlaylist(uuid: String) {
        viewModelScope.launch {
            playlistManager.deletePlaylist(uuid)
        }
    }

    fun trackFreeAccountCtaClick() {
        analyticsTracker.track(AnalyticsEvent.INFORMATIONAL_BANNER_VIEW_CREATE_ACCOUNT_TAP, mapOf("source" to "filters"))
    }

    fun dismissFreeAccountBanner() {
        analyticsTracker.track(AnalyticsEvent.INFORMATIONAL_BANNER_VIEW_DISMISSED, mapOf("source" to "filters"))
        settings.isFreeAccountFiltersBannerDismissed.set(true, updateModifiedAt = true)
    }

    internal data class UiState(
        val playlists: List<PlaylistPreview>,
        val showOnboarding: Boolean,
        val showFreeAccountBanner: Boolean,
        val miniPlayerInset: Int,
    ) {
        companion object {
            val Empty = UiState(
                playlists = emptyList(),
                showOnboarding = false,
                showFreeAccountBanner = false,
                miniPlayerInset = 0,
            )
        }
    }
}
