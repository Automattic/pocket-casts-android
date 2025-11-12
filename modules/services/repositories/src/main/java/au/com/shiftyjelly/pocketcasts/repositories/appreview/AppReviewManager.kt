package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.preferences.model.AppReviewReason
import com.google.android.play.core.review.ReviewInfo
import kotlinx.coroutines.flow.Flow

interface AppReviewManager {
    val showPromptSignal: Flow<AppReviewSignal>

    suspend fun monitorAppReviewReasons()
}

interface AppReviewSignal {
    val reason: AppReviewReason

    val reviewInfo: ReviewInfo

    fun consume()

    fun ignore()

    enum class Result {
        Consumed,
        Ignored,
    }
}
