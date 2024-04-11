package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
        settings.autoArchiveIncludesStarred.set(newValue, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_INCLUDE_STARRED_TOGGLED,
            mapOf("enabled" to newValue),
        )
    }

    fun onPlayedEpisodesAfterChanged(newStringValue: String) {
        val newValue = AutoArchiveAfterPlaying.fromString(newStringValue, context)
        settings.autoArchiveAfterPlaying.set(newValue, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_PLAYED_CHANGED,
            mapOf("value" to newValue.analyticsValue),
        )
    }

    fun onInactiveChanged(newStringValue: String) {
        val newValue = AutoArchiveInactive.fromString(newStringValue, context)
        settings.autoArchiveInactive.set(newValue, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_INACTIVE_CHANGED,
            mapOf("value" to newValue.analyticsValue),
        )
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }
}
