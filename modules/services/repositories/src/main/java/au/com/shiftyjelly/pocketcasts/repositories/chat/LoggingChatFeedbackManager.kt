package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import javax.inject.Inject

class LoggingChatFeedbackManager @Inject constructor() : ChatFeedbackManager {
    override suspend fun submit(feedback: ChatFeedback) {
        LogBuffer.i(
            TAG,
            "Episode chat feedback not sent, no endpoint yet: episode=${feedback.episodeUuid} reason=${feedback.reason} " +
                "hasDetails=${feedback.details.isNotBlank()} messages=${feedback.conversation.size}",
        )
    }

    private companion object {
        const val TAG = "EpisodeChat"
    }
}
