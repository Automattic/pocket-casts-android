package au.com.shiftyjelly.pocketcasts.repositories.appreview

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

@Singleton
class AppReviewManagerImpl @Inject constructor() : AppReviewManager {
    private val signal = MutableSharedFlow<Unit>()
    override val showPromptSignal: Flow<Unit> get() = signal

    suspend fun showPrompt() {
        signal.emit(Unit)
    }
}
