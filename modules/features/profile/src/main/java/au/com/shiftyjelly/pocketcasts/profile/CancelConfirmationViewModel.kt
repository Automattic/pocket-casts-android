package au.com.shiftyjelly.pocketcasts.profile

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CancelConfirmationViewModel
@Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    var expirationDate: String? = null
    private val paidSubscription: SubscriptionStatus.Paid?
        get() = settings.cachedSubscriptionStatus.value as? SubscriptionStatus.Paid

    init {
        onViewShown()
        expirationDate = paidSubscription?.expiryDate?.toLocalizedFormatLongStyle()
    }

    private fun onViewShown() {
        analyticsTracker.track(AnalyticsEvent.CANCEL_CONFIRMATION_VIEW_SHOWN)
    }

    fun onViewDismissed() {
        analyticsTracker.track(AnalyticsEvent.CANCEL_CONFIRMATION_VIEW_DISMISSED)
    }

    fun onStayClicked() {
        analyticsTracker.track(AnalyticsEvent.CANCEL_CONFIRMATION_STAY_BUTTON_TAPPED)
    }

    fun onCancelClicked() {
        analyticsTracker.track(AnalyticsEvent.CANCEL_CONFIRMATION_CANCEL_BUTTON_TAPPED)
    }
}
