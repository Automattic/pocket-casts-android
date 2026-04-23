package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ChatManager @Inject constructor(
    private val episodeChatDao: EpisodeChatDao,
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

    suspend fun saveMessage(episodeUuid: String, message: ChatMessage) {
        episodeChatDao.insertMessage(message.toEntity(episodeUuid))
    }

    suspend fun clearMessages(episodeUuid: String, welcomeMessage: ChatMessage) {
        episodeChatDao.deleteMessagesByEpisode(episodeUuid)
        episodeChatDao.insertMessage(welcomeMessage.toEntity(episodeUuid))
    }
}
