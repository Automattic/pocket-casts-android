package au.com.shiftyjelly.pocketcasts.settings

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HeadphoneControlsSettingsPageViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {

    fun onNextActionChanged(action: Settings.HeadphoneAction) {
        trackHeadphoneAction(action, AnalyticsEvent.SETTINGS_HEADPHONE_CONTROLS_NEXT_CHANGED)
    }

    fun onPreviousActionChanged(action: Settings.HeadphoneAction) {
        trackHeadphoneAction(action, AnalyticsEvent.SETTINGS_HEADPHONE_CONTROLS_PREVIOUS_CHANGED)
    }

    private fun trackHeadphoneAction(action: Settings.HeadphoneAction, event: AnalyticsEvent) {
        analyticsTracker.track(event, mapOf("value" to action.analyticsValue))
    }
}
