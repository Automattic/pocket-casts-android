package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveAfterPlayingSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveInactiveSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class AutoArchiveFragmentViewModel @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private var isFragmentChangingConfigurations: Boolean = false

    fun onViewCreated() {
        if (!isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_SHOWN)
        }
    }

    fun onStarredChanged(newValue: Boolean) {
        settings.autoArchiveIncludeStarred.set(newValue)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_INCLUDE_STARRED_TOGGLED,
            mapOf("enabled" to newValue)
        )
    }

    fun onPlayedEpisodesAfterChanged(newStringValue: String) {
        val newValue = AutoArchiveAfterPlayingSetting.fromString(newStringValue, context)
        settings.autoArchiveAfterPlaying.set(newValue)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_PLAYED_CHANGED,
            mapOf("value" to newValue.analyticsValue)
        )
    }

    fun onInactiveChanged(newStringValue: String) {
        val newValue = AutoArchiveInactiveSetting.fromString(newStringValue, context)
        settings.autoArchiveInactive.set(newValue)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_INACTIVE_CHANGED,
            mapOf("value" to newValue.analyticsValue)
        )
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }
}
