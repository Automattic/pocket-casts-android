package au.com.shiftyjelly.pocketcasts.settings

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HeadphoneControlsSettingsPageViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_HEADPHONE_CONTROLS_SHOWN)
    }

    fun onConfirmationSoundChanged(playConfirmationSound: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_HEADPHONE_CONTROLS_BOOKMARK_CONFIRMATION_SOUND,
            mapOf("value" to playConfirmationSound)
        )
    }

    fun onNextActionChanged(action: HeadphoneAction) {
        trackHeadphoneAction(action, AnalyticsEvent.SETTINGS_HEADPHONE_CONTROLS_NEXT_CHANGED)
    }

    fun onPreviousActionChanged(action: HeadphoneAction) {
        trackHeadphoneAction(action, AnalyticsEvent.SETTINGS_HEADPHONE_CONTROLS_PREVIOUS_CHANGED)
    }

    private fun trackHeadphoneAction(action: HeadphoneAction, event: AnalyticsEvent) {
        analyticsTracker.track(event, mapOf("value" to action.analyticsValue))
    }
}
