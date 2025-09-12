package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
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
        playlistManager.playlistPreviewsFlow(),
        settings.showPlaylistsOnboarding.flow,
        showFreeAccountBanner,
        settings.showEmptyFiltersListTooltip.flow,
        settings.bottomInset,
    ) { playlists, showOnboarding, showFreeAccountBanner, showTooltip, bottomInset ->
        UiState(
            playlists = PlaylistsState.Loaded(playlists),
            showOnboarding = showOnboarding,
            showFreeAccountBanner = showFreeAccountBanner,
            showPremadePlaylistsTooltip = shouldShowPremadePlaylistsTooltip(showTooltip, playlists),
            miniPlayerInset = bottomInset,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Empty)

    fun deletePlaylist(uuid: String) {
        viewModelScope.launch {
            playlistManager.deletePlaylist(uuid)
            trackPlaylistDeleted()
        }
    }

    fun updatePlaylistsOrder(playlistUuids: List<String>) {
        viewModelScope.launch {
            playlistManager.sortPlaylists(playlistUuids)
            trackPlaylistsReodered()
        }
    }

    fun dismissFreeAccountBanner() {
        trackFreeAccountBannerDismissed()
        settings.isFreeAccountFiltersBannerDismissed.set(true, updateModifiedAt = true)
    }

    fun dismissPremadePlaylistsTooltip() {
        trackTooltipDismissed()
        settings.showEmptyFiltersListTooltip.set(false, updateModifiedAt = false)
    }

    fun trackPlaylistsShown(playlistCount: Int) {
        analyticsTracker.track(AnalyticsEvent.FILTER_LIST_SHOWN, mapOf("filter_count" to playlistCount))
    }

    fun trackPlaylistsReodered() {
        analyticsTracker.track(AnalyticsEvent.FILTER_LIST_REORDERED)
    }

    fun trackPlaylistDeleted() {
        analyticsTracker.track(AnalyticsEvent.FILTER_DELETED)
    }

    fun trackCreatePlaylistClicked() {
        analyticsTracker.track(AnalyticsEvent.FILTER_CREATE_BUTTON_TAPPED)
    }

    fun trackTooltipShown() {
        analyticsTracker.track(AnalyticsEvent.FILTER_TOOLTIP_SHOWN)
    }

    fun trackTooltipDismissed() {
        analyticsTracker.track(AnalyticsEvent.FILTER_TOOLTIP_CLOSED)
    }

    fun trackFreeAccountCtaClicked() {
        analyticsTracker.track(AnalyticsEvent.INFORMATIONAL_BANNER_VIEW_CREATE_ACCOUNT_TAP, mapOf("source" to "filters"))
    }

    fun trackFreeAccountBannerDismissed() {
        analyticsTracker.track(AnalyticsEvent.INFORMATIONAL_BANNER_VIEW_DISMISSED, mapOf("source" to "filters"))
    }

    private fun shouldShowPremadePlaylistsTooltip(
        tooltipFlag: Boolean,
        playlists: List<PlaylistPreview>,
    ) = tooltipFlag &&
        playlists.size == PremadePlaylistUuids.size &&
        playlists.all { playlist -> playlist.uuid in PremadePlaylistUuids }

    internal data class UiState(
        val playlists: PlaylistsState,
        val showOnboarding: Boolean,
        val showFreeAccountBanner: Boolean,
        val showPremadePlaylistsTooltip: Boolean,
        val miniPlayerInset: Int,
    ) {
        val showEmptyState get() = when (playlists) {
            is PlaylistsState.Loading -> false
            is PlaylistsState.Loaded -> playlists.value.isEmpty()
        }

        companion object {
            val Empty = UiState(
                playlists = PlaylistsState.Loading,
                showOnboarding = false,
                showFreeAccountBanner = false,
                showPremadePlaylistsTooltip = false,
                miniPlayerInset = 0,
            )
        }
    }

    internal sealed interface PlaylistsState {
        data object Loading : PlaylistsState

        @JvmInline
        value class Loaded(val value: List<PlaylistPreview>) : PlaylistsState
    }

    private companion object {
        val PremadePlaylistUuids = setOf(Playlist.NEW_RELEASES_UUID, Playlist.IN_PROGRESS_UUID)
    }
}
