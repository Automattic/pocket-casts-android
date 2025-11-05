package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.preferences.model.AppReviewReason
import kotlinx.coroutines.flow.Flow

interface AppReviewManager {
    val showPromptSignal: Flow<AppReviewSignal>

    suspend fun monitorAppReviewReasons()
}

interface AppReviewSignal {
    val reason: AppReviewReason

    fun consume()

    fun ignore()

    enum class Result {
        Consumed,
        Ignored,
    }
}
