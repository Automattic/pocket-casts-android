package au.com.shiftyjelly.pocketcasts.views.review

import androidx.appcompat.app.AppCompatActivity
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppReviewHelper @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val reviewManager: ReviewManager,
) {
    /* Request in-app review from the user
       Right now, this method only allow requesting it once per user */
    suspend fun launchReviewDialog(
        activity: AppCompatActivity,
        delayInMs: Long,
        sourceView: SourceView,
    ) {
        if (settings.getReviewRequestedDates().isNotEmpty()) return
        delay(delayInMs)
        try {
            val flow = reviewManager.requestReviewFlow()
            flow.addOnCompleteListener { request ->
                if (request.isSuccessful) {
                    analyticsTracker.track(
                        AnalyticsEvent.APP_STORE_REVIEW_REQUESTED,
                        AnalyticsProp.addSource(sourceView)
                    )
                    settings.addReviewRequestedDate()
                    reviewManager.launchReviewFlow(activity, request.result)
                }
            }
        } catch (e: Exception) {
            Timber.e("Could not launch review dialog.")
            SentryHelper.recordException(e)
        }
    }

    private object AnalyticsProp {
        private const val source = "source"
        fun addSource(sourceView: SourceView) =
            mapOf(source to sourceView.analyticsValue)
    }
}
