package au.com.shiftyjelly.pocketcasts.kids.feedback

import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import javax.inject.Inject
import retrofit2.Response

class FeedbackManager @Inject constructor(
    private val syncManager: SyncManager,
) {
    companion object {
        const val SUBJECT = "Pocket Casts - Kids Profile Ideas"
        const val INBOX = "research"
    }

    suspend fun sendAnonymousFeedback(message: String): FeedbackResult = try {
        val response: Response<Void> = syncManager.sendAnonymousFeedback(SUBJECT, INBOX, message)
        if (response.isSuccessful) {
            FeedbackResult.Success
        } else {
            FeedbackResult.Error
        }
    } catch (e: Exception) {
        FeedbackResult.Error
    }
}

sealed class FeedbackResult {
    data object Success : FeedbackResult()
    data object Error : FeedbackResult()
}
