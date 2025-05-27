package au.com.shiftyjelly.pocketcasts.podcasts.view.notifications

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class EnableNotificationsPromptViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    fun reportShown() {
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_SHOWN)
    }

    fun reportDismissed() {
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_DISMISSED)
    }

    fun reportCtaTapped() {
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_ALLOW_TAPPED)
    }

    fun reportPermissionPromptShown() {
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_OPT_IN_SHOWN)
    }

    fun reportPermissionGranted() {
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_OPT_IN_ALLOWED)
    }

    fun reportPermissionDenied() {
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_OPT_IN_DENIED)
    }
}