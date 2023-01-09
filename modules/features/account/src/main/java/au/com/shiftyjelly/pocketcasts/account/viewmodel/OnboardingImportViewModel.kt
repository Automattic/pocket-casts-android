package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingImportViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {

    fun onImportStartPageShown(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.ONBOARDING_IMPORT_SHOWN,
            mapOf(flowKey to flow.analyticsValue)
        )
    }

    fun onOpenApp(flow: OnboardingFlow, appName: String) {
        analyticsTracker.track(
            AnalyticsEvent.ONBOARDING_IMPORT_OPEN_APP_SELECTED,
            mapOf(
                flowKey to flow.analyticsValue,
                appNameKey to appName
            )
        )
    }

    fun onAppSelected(flow: OnboardingFlow, appName: String) {
        analyticsTracker.track(
            AnalyticsEvent.ONBOARDING_IMPORT_APP_SELECTED,
            mapOf(flowKey to flow, appNameKey to appName)
        )
    }

    fun onImportDismissed(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.ONBOARDING_IMPORT_DISMISSED,
            mapOf(flowKey to flow.analyticsValue)
        )
    }

    companion object {
        const val flowKey = "flow"
        const val appNameKey = "app"
    }
}
