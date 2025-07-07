package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingImportViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    fun onImportStartPageShown(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.ONBOARDING_IMPORT_SHOWN,
            mapOf(FLOW_KEY to flow.analyticsValue),
        )
    }

    fun onOpenApp(flow: OnboardingFlow, appName: String) {
        analyticsTracker.track(
            AnalyticsEvent.ONBOARDING_IMPORT_OPEN_APP_SELECTED,
            mapOf(
                FLOW_KEY to flow.analyticsValue,
                APP_NAME_KEY to appName,
            ),
        )
    }

    fun onAppSelected(flow: OnboardingFlow, appName: String) {
        analyticsTracker.track(
            AnalyticsEvent.ONBOARDING_IMPORT_APP_SELECTED,
            mapOf(FLOW_KEY to flow, APP_NAME_KEY to appName),
        )
    }

    fun onImportDismissed(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.ONBOARDING_IMPORT_DISMISSED,
            mapOf(FLOW_KEY to flow.analyticsValue),
        )
    }

    companion object {
        const val FLOW_KEY = "flow"
        const val APP_NAME_KEY = "app"
    }
}
