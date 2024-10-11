package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class AutoDownloadSettingsViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val downloadManager: DownloadManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
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
        settings.autoDownloadUpNext.set(newValue, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_UP_NEXT_TOGGLED,
            mapOf("enabled" to newValue),
        )
    }

    fun getAutoDownloadUpNext() = settings.autoDownloadUpNext.value

    fun onNewEpisodesChange(newValue: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_NEW_EPISODES_TOGGLED,
            mapOf("enabled" to newValue),
        )
    }

    fun setLimitDownloads(value: AutoDownloadLimitSetting) {
        settings.autoDownloadLimit.set(value, updateModifiedAt = true)
    }

    fun onLimitDownloadsChange(value: AutoDownloadLimitSetting) {
        setLimitDownloads(value)

        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_LIMIT_DOWNLOADS_TOGGLED,
            mapOf("value" to value.analyticsString),
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
        settings.autoDownloadUnmeteredOnly.set(enabled, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_ONLY_ON_WIFI_TOGGLED,
            mapOf("enabled" to enabled),
        )
    }

    fun getAutoDownloadUnmeteredOnly() = settings.autoDownloadUnmeteredOnly.value

    fun onDownloadOnlyWhenChargingChange(enabled: Boolean) {
        settings.autoDownloadOnlyWhenCharging.set(enabled, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_ONLY_WHEN_CHARGING_TOGGLED,
            mapOf("enabled" to enabled),
        )
    }

    fun getAutoDownloadOnlyWhenCharging() = settings.autoDownloadOnlyWhenCharging.value
}
