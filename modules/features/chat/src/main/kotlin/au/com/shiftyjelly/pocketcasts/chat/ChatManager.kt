package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.servers.podcast.ConversationMessage
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatRequest
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ChatManager @Inject constructor(
    private val episodeChatDao: EpisodeChatDao,
    private val podcastCacheServiceManager: PodcastCacheServiceManager,
) {

    fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> {
        return episodeChatDao.observeMessages(episodeUuid).map { it.toChatMessages() }
    }

    suspend fun getMessages(episodeUuid: String): List<ChatMessage> {
        return episodeChatDao.getMessages(episodeUuid).toChatMessages()
    }

    suspend fun createChat(episodeUuid: String, podcastUuid: String?, welcomeMessage: ChatMessage) {
        episodeChatDao.insertChat(EpisodeChat(episodeUuid = episodeUuid, podcastUuid = podcastUuid))
        episodeChatDao.insertMessage(welcomeMessage.toEntity(episodeUuid))
    }

    suspend fun sendMessage(
        episodeUuid: String,
        podcastUuid: String,
        userMessage: ChatMessage,
        allMessages: List<ChatMessage>,
    ) {
        episodeChatDao.insertMessage(userMessage.toEntity(episodeUuid))

        val history = allMessages
            .map { msg ->
                ConversationMessage(
                    role = msg.role.value,
                    content = msg.text,
                )
            }

        val request = EpisodeChatRequest(
            episodeUuid = episodeUuid,
            podcastUuid = podcastUuid,
            message = userMessage.text,
            conversationHistory = history,
        )

        val response = podcastCacheServiceManager.episodeChat(request)

        val aiReply = ChatMessage(text = response.reply, role = ChatRole.Assistant)
        episodeChatDao.insertMessage(aiReply.toEntity(episodeUuid))
    }

    suspend fun clearMessages(episodeUuid: String, welcomeMessage: ChatMessage) {
        episodeChatDao.deleteMessagesByEpisode(episodeUuid)
        episodeChatDao.insertMessage(welcomeMessage.toEntity(episodeUuid))
    }
}
