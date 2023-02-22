package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoDownloadSettingsViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val downloadManager: DownloadManager,
    private val podcastManager: PodcastManager,
) : ViewModel(), CoroutineScope {

    override val coroutineContext = Dispatchers.Default
    private var isFragmentChangingConfigurations: Boolean = false

    fun onShown() {
        if (!isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_SHOWN)
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun onUpNextChange(newValue: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_UP_NEXT_TOGGLED,
            mapOf("enabled" to newValue)
        )
    }

    fun onNewEpisodesChange(newValue: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_NEW_EPISODES_TOGGLED,
            mapOf("enabled" to newValue)
        )
    }

    fun stopAllDownloads() {
        downloadManager.stopAllDownloads()
        analyticsTracker.track(AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_STOP_ALL_DOWNLOADS)
    }

    fun clearDownloadErrors() {
        launch {
            podcastManager.clearAllDownloadErrors()
        }
        analyticsTracker.track(AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_CLEAR_DOWNLOAD_ERRORS)
    }

    fun onDownloadOnlyOnUnmeteredChange(enabled: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_ONLY_ON_WIFI_TOGGLED,
            mapOf("enabled" to enabled),
        )
    }

    fun onDownloadOnlyWhenChargingChange(enabled: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_ONLY_WHEN_CHARGING_TOGGLED,
            mapOf("enabled" to enabled),
        )
    }
}
