package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingAccountBenefitsViewModel @Inject constructor(
    private val tracker: AnalyticsTracker,
) : ViewModel() {
    fun onScreenShown() {
        tracker.track(AnalyticsEvent.INFORMATIONAL_MODAL_VIEW_SHOWED)
    }

    fun onGetStartedClick() {
        tracker.track(AnalyticsEvent.INFORMATIONAL_MODAL_VIEW_GET_STARTED_TAP)
    }

    fun onLogInClick() {
        tracker.track(AnalyticsEvent.INFORMATIONAL_MODAL_VIEW_LOGIN_TAP)
    }

    fun onDismissClick() {
        tracker.track(AnalyticsEvent.INFORMATIONAL_MODAL_VIEW_DISMISSED)
    }

    fun onBenefitShown(cardAnalyticsValue: String) {
        tracker.track(AnalyticsEvent.INFORMATIONAL_MODAL_VIEW_CARD_SHOWED, mapOf("card" to cardAnalyticsValue))
    }
}
