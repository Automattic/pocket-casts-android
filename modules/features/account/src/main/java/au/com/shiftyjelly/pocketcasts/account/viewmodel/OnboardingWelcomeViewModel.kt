package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OnboardingWelcomeViewModel @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {

    private val _stateFlow = MutableStateFlow(OnboardingWelcomeState())
    val stateFlow: StateFlow<OnboardingWelcomeState> = _stateFlow

    fun updateNewsletter(isChecked: Boolean) {
        _stateFlow.update { it.copy(newsletter = isChecked) }
    }

    private fun persistNewsletterSelection(newsletter: Boolean) {
        settings.setMarketingOptIn(newsletter)
        settings.setMarketingOptInNeedsSync(true)
    }

    fun onShown(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.WELCOME_SHOWN,
            mapOf(AnalyticsProp.FLOW to flow)
        )
    }

    fun onBackPressed(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.WELCOME_DISMISSED,
            mapOf(AnalyticsProp.FLOW to flow)
        )
    }

    fun onContinueToDiscover(flow: String) {
        val newsletter = stateFlow.value.newsletter
        analyticsTracker.track(
            AnalyticsEvent.WELCOME_DISCOVER_TAPPED,
            mapOf(
                AnalyticsProp.FLOW to flow,
                AnalyticsProp.NEWSLETTER_OPT_IN to newsletter
            )
        )
        persistNewsletterSelection(newsletter)
    }

    fun onDone(flow: String) {
        val newsletter = stateFlow.value.newsletter
        analyticsTracker.track(
            AnalyticsEvent.WELCOME_DONE_TAPPED,
            mapOf(
                AnalyticsProp.FLOW to flow,
                AnalyticsProp.NEWSLETTER_OPT_IN to newsletter
            )
        )
        persistNewsletterSelection(newsletter)
    }

    fun onImportTapped(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.WELCOME_IMPORT_TAPPED,
            mapOf(AnalyticsProp.FLOW to flow)
        )
    }

    companion object {
        private object AnalyticsProp {
            const val SOURCE = "source"
            const val FLOW = "flow"
            const val NEWSLETTER_OPT_IN = "newsletter_opt_in"
            const val ENABLED = "enabled"
        }
    }
}

data class OnboardingWelcomeState(
    val newsletter: Boolean = true,
)
