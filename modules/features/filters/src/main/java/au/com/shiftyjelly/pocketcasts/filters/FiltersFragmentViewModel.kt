package au.com.shiftyjelly.pocketcasts.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SmartPlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Collections
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@HiltViewModel
class FiltersFragmentViewModel @Inject constructor(
    val smartPlaylistManager: SmartPlaylistManager,
    private val analyticsTracker: AnalyticsTracker,
    private val settings: Settings,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val userManager: UserManager,
) : ViewModel(),
    CoroutineScope {

    companion object {
        private const val FILTER_COUNT_KEY = "filter_count"
    }

    var isFragmentChangingConfigurations: Boolean = false
        private set

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    val filters: LiveData<List<SmartPlaylist>> = smartPlaylistManager.findAllRxFlowable().toLiveData()

    val countGenerator = { smartPlaylist: SmartPlaylist ->
        smartPlaylistManager.countEpisodesRxFlowable(smartPlaylist, episodeManager, playbackManager).onErrorReturn { 0 }
    }

    var adapterState: MutableList<SmartPlaylist> = mutableListOf()
    fun movePlaylist(fromPosition: Int, toPosition: Int): List<SmartPlaylist> {
        if (fromPosition < toPosition) {
            for (index in fromPosition until toPosition) {
                Collections.swap(adapterState, index, index + 1)
            }
        } else {
            for (index in fromPosition downTo toPosition + 1) {
                Collections.swap(adapterState, index, index - 1)
            }
        }
        return adapterState.toList()
    }

    fun commitMoves(moved: Boolean) {
        val playlists = adapterState

        playlists.forEachIndexed { index, playlist ->
            playlist.sortPosition = index
            playlist.syncStatus = SmartPlaylist.SYNC_STATUS_NOT_SYNCED
        }

        runBlocking(Dispatchers.Default) {
            smartPlaylistManager.updateAllBlocking(playlists)
            if (moved) {
                analyticsTracker.track(AnalyticsEvent.FILTER_LIST_REORDERED)
            }
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun trackFilterListShown(filterCount: Int) {
        val properties = mapOf(FILTER_COUNT_KEY to filterCount)
        analyticsTracker.track(AnalyticsEvent.FILTER_LIST_SHOWN, properties)
    }

    fun findPlaylistByUuid(playlistUuid: String, onSuccess: (SmartPlaylist) -> Unit) {
        viewModelScope.launch {
            val playlist = smartPlaylistManager.findByUuid(playlistUuid) ?: return@launch
            onSuccess(playlist)
        }
    }

    fun trackOnCreateFilterTap() {
        analyticsTracker.track(AnalyticsEvent.FILTER_CREATE_BUTTON_TAPPED)
    }

    fun trackTooltipShown() {
        analyticsTracker.track(AnalyticsEvent.FILTER_TOOLTIP_SHOWN)
    }

    fun shouldShowTooltip(filters: List<SmartPlaylist>, onShowTooltip: () -> Unit) {
        viewModelScope.launch {
            shouldShowTooltipSuspend(filters, onShowTooltip)
        }
    }

    suspend fun shouldShowTooltipSuspend(filters: List<SmartPlaylist>, onShowTooltip: () -> Unit) {
        if (!settings.showEmptyFiltersListTooltip.value) return
        if (filters.size > 2) return

        val requiredUuids = setOf(Playlist.NEW_RELEASES_UUID, Playlist.IN_PROGRESS_UUID)
        val filterUuids = filters.map { it.uuid }.toSet()

        if (filterUuids != requiredUuids) return

        withContext(Dispatchers.IO) {
            val showTooltip = filters.all { playlist ->
                val episodeCount = smartPlaylistManager.countEpisodesBlocking(playlist.id, episodeManager, playbackManager)
                episodeCount == 0
            }
            if (showTooltip) {
                withContext(Dispatchers.Main) {
                    onShowTooltip()
                }
            }
        }
    }

    fun onTooltipClosed() {
        settings.showEmptyFiltersListTooltip.set(false, updateModifiedAt = false)
        analyticsTracker.track(AnalyticsEvent.FILTER_TOOLTIP_CLOSED)
    }

    internal val isFreeAccountBannerVisible = combine(
        userManager.getSignInState().asFlow().map { it.isSignedIn },
        settings.isFreeAccountFiltersBannerDismissed.flow,
    ) { isSignedIn, isBannerDismissed ->
        !isSignedIn && !isBannerDismissed && FeatureFlag.isEnabled(Feature.ENCOURAGE_ACCOUNT_CREATION)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    internal fun onCreateFreeAccountClick() {
        analyticsTracker.track(AnalyticsEvent.INFORMATIONAL_BANNER_VIEW_CREATE_ACCOUNT_TAP, mapOf("source" to "filters"))
    }

    internal fun dismissFreeAccountBanner() {
        analyticsTracker.track(AnalyticsEvent.INFORMATIONAL_BANNER_VIEW_DISMISSED, mapOf("source" to "filters"))
        settings.isFreeAccountFiltersBannerDismissed.set(true, updateModifiedAt = true)
    }
}
