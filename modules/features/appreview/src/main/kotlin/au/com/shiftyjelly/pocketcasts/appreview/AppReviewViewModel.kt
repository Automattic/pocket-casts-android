package au.com.shiftyjelly.pocketcasts.appreview

import android.app.Activity
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AppReviewReason
import com.automattic.eventhorizon.AppStoreReviewRequestedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.UserSatisfactionSurveyDismissedEvent
import com.automattic.eventhorizon.UserSatisfactionSurveyNoResponseEvent
import com.automattic.eventhorizon.UserSatisfactionSurveyShownEvent
import com.automattic.eventhorizon.UserSatisfactionSurveyYesResponseEvent
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
    private val eventHorizon: EventHorizon,
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
        eventHorizon.track(AppStoreReviewRequestedEvent)
        runCatching { reviewManager.launchReview(activity, reviewInfo) }
    }

    fun trackPageShown() {
        eventHorizon.track(
            UserSatisfactionSurveyShownEvent(
                triggerEvent = appReviewReason.analyticsValue,
                userType = settings.cachedMembership.value.eventHorizonValue,
            ),
        )
    }

    fun trackPageDismissed() {
        eventHorizon.track(
            UserSatisfactionSurveyDismissedEvent(
                triggerEvent = appReviewReason.analyticsValue,
                userType = settings.cachedMembership.value.eventHorizonValue,
            ),
        )
    }

    fun trackNoResponse() {
        eventHorizon.track(
            UserSatisfactionSurveyNoResponseEvent(
                triggerEvent = appReviewReason.analyticsValue,
                userType = settings.cachedMembership.value.eventHorizonValue,
            ),
        )
    }

    fun trackYesResponse() {
        eventHorizon.track(
            UserSatisfactionSurveyYesResponseEvent(
                triggerEvent = appReviewReason.analyticsValue,
                userType = settings.cachedMembership.value.eventHorizonValue,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(reason: AppReviewReason): AppReviewViewModel
    }
}
