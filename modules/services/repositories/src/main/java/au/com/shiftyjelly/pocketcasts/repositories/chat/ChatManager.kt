package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import kotlinx.coroutines.flow.Flow

interface ChatManager {
    fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>>

    fun observeHistory(): Flow<List<EpisodeChatHistoryItem>>

    suspend fun createChat(
        episodeUuid: String,
        podcastUuid: String,
    )

    suspend fun sendMessage(
        episodeUuid: String,
        message: ChatMessage.User,
        allMessages: List<ChatMessage>,
    )

    suspend fun discardMessage(uuid: String)

    suspend fun clearMessages(episodeUuid: String): ClearedChat

    suspend fun restoreMessages(clearedChat: ClearedChat)

    suspend fun rateAnswer(answerUuid: String, rating: ChatAnswerRating?)

    suspend fun deleteAllChats()
}
