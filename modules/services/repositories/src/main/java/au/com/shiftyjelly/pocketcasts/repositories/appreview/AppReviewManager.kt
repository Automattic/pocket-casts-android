package au.com.shiftyjelly.pocketcasts.repositories.appreview

import kotlinx.coroutines.flow.Flow

interface AppReviewManager {
    val showPromptSignal: Flow<Unit>
}
