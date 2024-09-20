package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.WEAR_SIGNIN_SHOWN)
    }

    fun onGoogleLoginClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_SIGNIN_GOOGLE_TAPPED)
    }

    fun onPhoneLoginClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_SIGNIN_PHONE_TAPPED)
    }

    fun onEmailLoginClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_SIGNIN_EMAIL_TAPPED)
    }
}
