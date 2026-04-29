package au.com.shiftyjelly.pocketcasts.repositories.chat

import kotlinx.coroutines.flow.Flow

interface ChatManager {
    fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>>

    suspend fun getMessages(episodeUuid: String): List<ChatMessage>

    suspend fun createChat(
        episodeUuid: String,
        podcastUuid: String,
        welcomeMessage: ChatMessage,
    )

    suspend fun sendMessage(
        episodeUuid: String,
        podcastUuid: String,
        message: String,
        allMessages: List<ChatMessage>,
        isRetry: Boolean = false,
    )

    suspend fun clearMessages(
        episodeUuid: String,
        welcomeMessage: ChatMessage,
    )
}
