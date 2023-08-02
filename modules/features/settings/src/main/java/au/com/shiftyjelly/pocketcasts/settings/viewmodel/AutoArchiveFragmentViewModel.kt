package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveAfterPlayingSetting
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

    fun onStarredChanged() {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_INCLUDE_STARRED_TOGGLED,
            mapOf("enabled" to settings.getAutoArchiveIncludeStarred())
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

    fun onInactiveChanged() {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_INACTIVE_CHANGED,
            mapOf(
                "value" to when (settings.getAutoArchiveInactive()) {
                    Settings.AutoArchiveInactive.Never -> "never"
                    Settings.AutoArchiveInactive.Hours24 -> "after_24_hours"
                    Settings.AutoArchiveInactive.Days2 -> "after_2_days"
                    Settings.AutoArchiveInactive.Weeks1 -> "after_1_week"
                    Settings.AutoArchiveInactive.Weeks2 -> "after_2_weeks"
                    Settings.AutoArchiveInactive.Days30 -> "after_30_days"
                    Settings.AutoArchiveInactive.Days90 -> "after 3 months"
                }
            )
        )
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }
}
