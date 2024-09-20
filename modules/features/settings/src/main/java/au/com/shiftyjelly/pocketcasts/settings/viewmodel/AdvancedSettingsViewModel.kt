package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AdvancedSettingsViewModel
@Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initState())
    val state: StateFlow<State> = mutableState

    private fun initState() = State(
        backgroundSyncOnMeteredState = State.BackgroundSyncOnMeteredState(
            isChecked = settings.syncOnMeteredNetwork(),
            isEnabled = settings.backgroundRefreshPodcasts.value,
            onCheckedChange = {
                // isEnabled controls the grey out of the function but not if it's actually called
                // here we disable the functionality
                if (settings.backgroundRefreshPodcasts.value) {
                    onSyncOnMeteredCheckedChange(it)
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_ADVANCED_SYNC_ON_METERED,
                        mapOf("enabled" to it),
                    )
                }
            },
        ),
        prioritizeSeekAccuracyState = State.PrioritizeSeekAccuracyState(
            isChecked = settings.prioritizeSeekAccuracy.value,
            onCheckedChange = {
                settings.prioritizeSeekAccuracy.set(it, updateModifiedAt = false)
                updatePrioritizeSeekAccuracyState()
                analyticsTracker.track(
                    AnalyticsEvent.SETTINGS_ADVANCED_PRIORITIZE_SEEK_ACCURACY,
                    mapOf("enabled" to it),
                )
            },
        ),
        cacheEntirePlayingEpisodeState = State.CacheEntirePlayingEpisodeState(
            isChecked = settings.cacheEntirePlayingEpisode.value,
            onCheckedChange = {
                settings.cacheEntirePlayingEpisode.set(it, updateModifiedAt = false)
                updateCacheEntirePlayingEpisodeState()
                analyticsTracker.track(
                    AnalyticsEvent.SETTINGS_ADVANCED_CACHE_ENTIRE_PLAYING_EPISODE,
                    mapOf("enabled" to it),
                )
            },
        ),
    )

    private fun onSyncOnMeteredCheckedChange(isChecked: Boolean) {
        settings.setSyncOnMeteredNetwork(isChecked)
        updateSyncOnMeteredState()

        // Update worker to take sync setting into account
        RefreshPodcastsTask.scheduleOrCancel(context, settings)
    }

    private fun updateSyncOnMeteredState() {
        mutableState.value = mutableState.value.copy(
            backgroundSyncOnMeteredState = mutableState.value.backgroundSyncOnMeteredState.copy(
                isChecked = settings.syncOnMeteredNetwork(),
            ),
        )
    }

    private fun updatePrioritizeSeekAccuracyState() {
        mutableState.value = mutableState.value.copy(
            prioritizeSeekAccuracyState = mutableState.value.prioritizeSeekAccuracyState.copy(
                isChecked = settings.prioritizeSeekAccuracy.value,
            ),
        )
    }

    private fun updateCacheEntirePlayingEpisodeState() {
        mutableState.value = mutableState.value.copy(
            cacheEntirePlayingEpisodeState = mutableState.value.cacheEntirePlayingEpisodeState.copy(
                isChecked = settings.cacheEntirePlayingEpisode.value,
            ),
        )
    }

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_ADVANCED_SHOWN)
    }

    data class State(
        val backgroundSyncOnMeteredState: BackgroundSyncOnMeteredState,
        val prioritizeSeekAccuracyState: PrioritizeSeekAccuracyState,
        val cacheEntirePlayingEpisodeState: CacheEntirePlayingEpisodeState,
    ) {

        data class BackgroundSyncOnMeteredState(
            val isChecked: Boolean,
            val isEnabled: Boolean,
            val onCheckedChange: (Boolean) -> Unit,
        )

        data class PrioritizeSeekAccuracyState(
            val isChecked: Boolean,
            val onCheckedChange: (Boolean) -> Unit,
        )

        data class CacheEntirePlayingEpisodeState(
            val isChecked: Boolean,
            val onCheckedChange: (Boolean) -> Unit,
        )
    }
}
