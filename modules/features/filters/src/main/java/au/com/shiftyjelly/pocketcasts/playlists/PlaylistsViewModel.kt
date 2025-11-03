package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistTooltip
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
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
    ) { isBannerDismissed, isSignedOut ->
        isSignedOut && !isBannerDismissed
    }

    internal val uiState = combine(
        playlistManager.playlistPreviewsFlow(),
        settings.showPlaylistsOnboarding.flow,
        showFreeAccountBanner,
        settings.showPremadePlaylistsTooltip.flow,
        settings.showRearrangePlaylistsTooltip.flow,
        settings.bottomInset,
    ) { playlists, showOnboarding, showFreeAccountBanner, showPremadeTooltip, showRearrangeTooltip, bottomInset ->
        UiState(
            playlists = PlaylistsState.Loaded(playlists),
            showOnboarding = showOnboarding,
            showFreeAccountBanner = showFreeAccountBanner,
            displayedTooltips = buildList {
                if (shouldShowPremadePlaylistsTooltip(showPremadeTooltip, playlists)) {
                    add(PlaylistTooltip.Premade)
                }
                if (shouldShowRearrangePlaylistsTooltip(showRearrangeTooltip, playlists)) {
                    add(PlaylistTooltip.Rearrange)
                }
            },
            miniPlayerInset = bottomInset,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeout = 300.milliseconds), UiState.Empty)

    fun getArtworkUuidsFlow(playlistUuid: String): StateFlow<List<String>?> {
        return playlistManager.getArtworkUuidsFlow(playlistUuid)
    }

    fun getEpisodeCountFlow(playlistUuid: String): StateFlow<Int?> {
        return playlistManager.getEpisodeCountFlow(playlistUuid)
    }

    suspend fun refreshArtworkUuids(playlistUuid: String) {
        playlistManager.refreshArtworkUuids(playlistUuid)
    }

    suspend fun refreshEpisodeCount(playlistUuid: String) {
        playlistManager.refreshEpisodeCount(playlistUuid)
    }

    fun deletePlaylist(playlist: PlaylistPreview) {
        viewModelScope.launch {
            playlistManager.deletePlaylist(playlist.uuid)
            trackPlaylistDeleted(playlist)
        }
    }

    fun updatePlaylistsOrder(playlistUuids: List<String>) {
        viewModelScope.launch {
            playlistManager.sortPlaylists(playlistUuids)
            trackPlaylistsReordered()
        }
    }

    fun dismissFreeAccountBanner() {
        trackFreeAccountBannerDismissed()
        settings.isFreeAccountFiltersBannerDismissed.set(true, updateModifiedAt = true)
    }

    internal fun dismissTooltip(tooltip: PlaylistTooltip) {
        when (tooltip) {
            PlaylistTooltip.Premade -> {
                // We track this only due to legacy reasons
                trackTooltipDismissed()
                settings.showPremadePlaylistsTooltip.set(false, updateModifiedAt = false)
            }

            PlaylistTooltip.Rearrange -> {
                settings.showRearrangePlaylistsTooltip.set(false, updateModifiedAt = false)
            }
        }
    }

    fun trackPlaylistsShown(playlistCount: Int) {
        analyticsTracker.track(AnalyticsEvent.FILTER_LIST_SHOWN, mapOf("filter_count" to playlistCount))
    }

    fun trackPlaylistsReordered() {
        analyticsTracker.track(AnalyticsEvent.FILTER_LIST_REORDERED)
    }

    fun trackPlaylistDeleteTriggered(playlist: PlaylistPreview) {
        analyticsTracker.track(
            AnalyticsEvent.FILTER_DELETE_TRIGGERED,
            mapOf("filter_type" to playlist.type.analyticsValue),
        )
    }

    fun trackPlaylistDeleteDismissed(playlist: PlaylistPreview) {
        analyticsTracker.track(
            AnalyticsEvent.FILTER_DELETE_DISMISSED,
            mapOf("filter_type" to playlist.type.analyticsValue),
        )
    }

    fun trackPlaylistDeleted(playlist: PlaylistPreview) {
        analyticsTracker.track(
            AnalyticsEvent.FILTER_DELETED,
            mapOf("filter_type" to playlist.type.analyticsValue),
        )
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

    private fun shouldShowRearrangePlaylistsTooltip(
        tooltipFlag: Boolean,
        playlists: List<PlaylistPreview>,
    ) = tooltipFlag && playlists.size > 1

    internal data class UiState(
        val playlists: PlaylistsState,
        val showOnboarding: Boolean,
        val showFreeAccountBanner: Boolean,
        val displayedTooltips: List<PlaylistTooltip>,
        val miniPlayerInset: Int,
    ) {
        val showEmptyState
            get() = when (playlists) {
                is PlaylistsState.Loading -> false
                is PlaylistsState.Loaded -> playlists.value.isEmpty()
            }

        companion object {
            val Empty = UiState(
                playlists = PlaylistsState.Loading,
                showOnboarding = false,
                showFreeAccountBanner = false,
                displayedTooltips = emptyList(),
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
