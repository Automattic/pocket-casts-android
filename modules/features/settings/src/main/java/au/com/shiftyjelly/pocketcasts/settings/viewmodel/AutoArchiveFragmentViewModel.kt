package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AutoArchiveFragmentViewModel @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
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

    fun onPlayedEpisodesAfterChanged() {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_PLAYED_CHANGED,
            mapOf(
                "value" to when (settings.getAutoArchiveAfterPlaying()) {
                    Settings.AutoArchiveAfterPlaying.Never -> "never"
                    Settings.AutoArchiveAfterPlaying.AfterPlaying -> "after_playing"
                    Settings.AutoArchiveAfterPlaying.Hours24 -> "after_24_hours"
                    Settings.AutoArchiveAfterPlaying.Days2 -> "after_2_days"
                    Settings.AutoArchiveAfterPlaying.Weeks1 -> "after_1_week"
                }
            )
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
