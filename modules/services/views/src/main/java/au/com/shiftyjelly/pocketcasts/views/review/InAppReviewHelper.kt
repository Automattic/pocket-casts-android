package au.com.shiftyjelly.pocketcasts.views.review

import androidx.appcompat.app.AppCompatActivity
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.eventhorizon.AppStoreReviewRequestedEvent
import com.automattic.eventhorizon.EventHorizon
import com.google.android.play.core.review.ReviewManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import timber.log.Timber

@Singleton
class InAppReviewHelper @Inject constructor(
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
    private val reviewManager: ReviewManager,
    private val crashLogging: CrashLogging,
) {
    suspend fun launchReviewDialog(
        activity: AppCompatActivity,
        delayInMs: Long,
    ) {
        if (settings.getReviewRequestedDates().isNotEmpty()) {
            return
        }
        delay(delayInMs)
        try {
            val flow = reviewManager.requestReviewFlow()
            flow.addOnCompleteListener { request ->
                if (request.isSuccessful) {
                    eventHorizon.track(AppStoreReviewRequestedEvent)
                    settings.addReviewRequestedDate()
                    reviewManager.launchReviewFlow(activity, request.result)
                }
            }
        } catch (e: Exception) {
            Timber.e("Could not launch review dialog.")
            crashLogging.sendReport(e)
        }
    }
}
