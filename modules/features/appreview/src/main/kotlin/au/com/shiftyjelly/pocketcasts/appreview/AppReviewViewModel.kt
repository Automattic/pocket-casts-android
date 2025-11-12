package au.com.shiftyjelly.pocketcasts.appreview

import android.app.Activity
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class AppReviewViewModel @Inject constructor(
    private val settings: Settings,
    private val clock: Clock,
    private val reviewManager: ReviewManager,
) : ViewModel() {
    fun declineAppReview() {
        val newTimestamps = buildList {
            addAll(settings.appReviewLastDeclineTimestamps.value.takeLast(1))
            add(clock.instant())
        }
        settings.appReviewLastDeclineTimestamps.set(newTimestamps, updateModifiedAt = false)
    }

    suspend fun launchReview(activity: Activity, reviewInfo: ReviewInfo) {
        runCatching { reviewManager.launchReview(activity, reviewInfo) }
    }
}
