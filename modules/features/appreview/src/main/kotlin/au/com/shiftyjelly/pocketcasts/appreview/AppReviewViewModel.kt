package au.com.shiftyjelly.pocketcasts.appreview

import android.app.Activity
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AppReviewReason
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock

@HiltViewModel(assistedFactory = AppReviewViewModel.Factory::class)
class AppReviewViewModel @AssistedInject constructor(
    private val settings: Settings,
    private val clock: Clock,
    private val reviewManager: ReviewManager,
    private val tracker: AnalyticsTracker,
    @Assisted private val appReviewReason: AppReviewReason,
) : ViewModel() {
    fun declineAppReview() {
        val newTimestamps = buildList {
            addAll(settings.appReviewLastDeclineTimestamps.value.takeLast(1))
            add(clock.instant())
        }
        settings.appReviewLastDeclineTimestamps.set(newTimestamps, updateModifiedAt = false)
    }

    suspend fun launchReview(activity: Activity, reviewInfo: ReviewInfo) {
        tracker.track(AnalyticsEvent.APP_STORE_REVIEW_REQUESTED)
        runCatching { reviewManager.launchReview(activity, reviewInfo) }
    }

    fun trackPageShown() {
        tracker.track(
            AnalyticsEvent.USER_SATISFACTION_SURVEY_SHOWN,
            computeTrackingProperties(),
        )
    }

    fun trackPageDismissed() {
        tracker.track(
            AnalyticsEvent.USER_SATISFACTION_SURVEY_DISMISSED,
            computeTrackingProperties(),
        )
    }

    fun trackNoResponse() {
        tracker.track(
            AnalyticsEvent.USER_SATISFACTION_SURVEY_NO_RESPONSE,
            computeTrackingProperties(),
        )
    }

    fun trackYesResponse() {
        tracker.track(
            AnalyticsEvent.USER_SATISFACTION_SURVEY_YES_RESPONSE,
            computeTrackingProperties(),
        )
    }

    private fun computeTrackingProperties() = mapOf(
        "trigger_event" to appReviewReason.analyticsValue,
        "user_type" to when (settings.cachedMembership.value.subscription?.tier) {
            SubscriptionTier.Plus, SubscriptionTier.Patron -> "plus"
            null -> "free"
        },
    )

    @AssistedFactory
    interface Factory {
        fun create(reason: AppReviewReason): AppReviewViewModel
    }
}
