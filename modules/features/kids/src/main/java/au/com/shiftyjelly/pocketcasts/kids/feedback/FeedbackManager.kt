package au.com.shiftyjelly.pocketcasts.kids.feedback

import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import javax.inject.Inject

class FeedbackManager @Inject constructor(
    private val syncManager: SyncManager,
) {
    companion object {
        const val SUBJECT = "Pocket Casts - Kids Profile Ideas"
        const val INBOX = "research"
    }

    suspend fun sendFeedback(message: String): FeedbackResult {
        return processFeedback(message, isAnonymous = !syncManager.isLoggedIn())
    }

    private suspend fun processFeedback(message: String, isAnonymous: Boolean): FeedbackResult = try {
        val response = if (isAnonymous) syncManager.sendAnonymousFeedback(SUBJECT, INBOX, message) else syncManager.sendFeedback(SUBJECT, INBOX, message)
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
