package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.flow.combine
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistTooltip
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.FilterCreateButtonTappedEvent
import com.automattic.eventhorizon.FilterDeleteDismissedEvent
import com.automattic.eventhorizon.FilterDeleteTriggeredEvent
import com.automattic.eventhorizon.FilterDeletedEvent
import com.automattic.eventhorizon.FilterListReorderedEvent
import com.automattic.eventhorizon.FilterListShownEvent
import com.automattic.eventhorizon.FilterTooltipClosedEvent
import com.automattic.eventhorizon.FilterTooltipShownEvent
import com.automattic.eventhorizon.InformationalBannerViewCreateAccountTapEvent
import com.automattic.eventhorizon.InformationalBannerViewDismissedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistManager: PlaylistManager,
    private val userManager: UserManager,
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
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
        showFreeAccountBanner.onStart { emit(false) },
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
        eventHorizon.track(
            FilterListShownEvent(
                filterCount = playlistCount.toLong(),
            ),
        )
    }

    fun trackPlaylistsReordered() {
        eventHorizon.track(FilterListReorderedEvent)
    }

    fun trackPlaylistDeleteTriggered(playlist: PlaylistPreview) {
        eventHorizon.track(
            FilterDeleteTriggeredEvent(
                filterType = playlist.type.eventHorizonValue,
            ),
        )
    }

    fun trackPlaylistDeleteDismissed(playlist: PlaylistPreview) {
        eventHorizon.track(
            FilterDeleteDismissedEvent(
                filterType = playlist.type.eventHorizonValue,
            ),
        )
    }

    fun trackPlaylistDeleted(playlist: PlaylistPreview) {
        eventHorizon.track(
            FilterDeletedEvent(
                filterType = playlist.type.eventHorizonValue,
            ),
        )
    }

    fun trackCreatePlaylistClicked() {
        eventHorizon.track(FilterCreateButtonTappedEvent)
    }

    fun trackTooltipShown() {
        eventHorizon.track(FilterTooltipShownEvent)
    }

    fun trackTooltipDismissed() {
        eventHorizon.track(FilterTooltipClosedEvent)
    }

    fun trackFreeAccountCtaClicked() {
        eventHorizon.track(
            InformationalBannerViewCreateAccountTapEvent(
                source = SourceView.FILTERS.eventHorizonValue,
            ),
        )
    }

    fun trackFreeAccountBannerDismissed() {
        eventHorizon.track(
            InformationalBannerViewDismissedEvent(
                source = SourceView.FILTERS.eventHorizonValue,
            ),
        )
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
