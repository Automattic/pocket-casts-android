package au.com.shiftyjelly.pocketcasts.repositories.chat

interface ChatFeedbackManager {
    suspend fun submit(feedback: ChatFeedback)
}

data class ChatFeedback(
    val episodeUuid: String,
    val podcastUuid: String,
    val reason: Reason,
    val details: String,
    val conversation: List<ChatMessage>,
) {
    enum class Reason {
        NotInteresting,
        WrongFacts,
        OutOfDate,
        Offensive,
        Other,
    }
}
