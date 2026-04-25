package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript
import au.com.shiftyjelly.pocketcasts.servers.podcast.ConversationMessage
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatRequest
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class ChatManager @Inject constructor(
    private val episodeChatDao: EpisodeChatDao,
    private val transcriptDao: TranscriptDao,
    private val podcastCacheServiceManager: PodcastCacheServiceManager,
    moshi: Moshi,
) {
    private val quoteMetadataAdapter = moshi.adapter(QuoteMetadata::class.java)

    fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> {
        return episodeChatDao.observeMessages(episodeUuid).map { it.toChatMessages(quoteMetadataAdapter) }
    }

    suspend fun getMessages(episodeUuid: String): List<ChatMessage> {
        return episodeChatDao.getMessages(episodeUuid).toChatMessages(quoteMetadataAdapter)
    }

    suspend fun createChat(episodeUuid: String, podcastUuid: String, welcomeMessage: ChatMessage) {
        episodeChatDao.insertChat(EpisodeChat(episodeUuid = episodeUuid, podcastUuid = podcastUuid))
        episodeChatDao.insertMessage(welcomeMessage.toEntity(episodeUuid, quoteMetadataAdapter))
    }

    suspend fun sendMessage(
        episodeUuid: String,
        podcastUuid: String,
        message: String,
        allMessages: List<ChatMessage>,
        isRetry: Boolean = false,
    ) {
        if (!isRetry) {
            val userMessage = ChatMessage.User(text = message)
            episodeChatDao.insertMessage(userMessage.toEntity(episodeUuid, quoteMetadataAdapter))
        }

        val history = allMessages.mapNotNull { msg ->
            val content = msg.textOrNull() ?: return@mapNotNull null
            ConversationMessage(role = msg.role.apiRole, content = content)
        }

        val transcript = selectTranscript(episodeUuid)
        val request = if (transcript != null && !transcript.isGenerated) {
            EpisodeChatRequest(
                transcriptUrl = transcript.url,
                message = message,
                conversationHistory = history,
            )
        } else {
            EpisodeChatRequest(
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                message = message,
                conversationHistory = history,
            )
        }

        val response = podcastCacheServiceManager.episodeChat(request)

        val aiReply = ChatMessage.Assistant(text = response.reply)
        episodeChatDao.insertMessage(aiReply.toEntity(episodeUuid, quoteMetadataAdapter))

        val quote = response.quote
        val quoteText = quote?.text?.takeIf { it.isNotBlank() }
        val quoteStart = quote?.start?.takeIf { it.isNotBlank() }
        val quoteEnd = quote?.end?.takeIf { it.isNotBlank() }
        if (quoteText != null && quoteStart != null && quoteEnd != null) {
            val quoteMessage = ChatMessage.Quote(
                text = quoteText,
                start = quoteStart,
                end = quoteEnd,
            )
            episodeChatDao.insertMessage(quoteMessage.toEntity(episodeUuid, quoteMetadataAdapter))
        }
    }

    suspend fun clearMessages(episodeUuid: String, welcomeMessage: ChatMessage) {
        episodeChatDao.deleteMessagesByEpisode(episodeUuid)
        episodeChatDao.insertMessage(welcomeMessage.toEntity(episodeUuid, quoteMetadataAdapter))
    }

    // Pick the best transcript for chat: prefer author-provided (non-generated) over Pocket Casts-generated.
    // Matches the priority used by TranscriptManagerImpl so chat and transcript UI pick the same source.
    private suspend fun selectTranscript(episodeUuid: String): Transcript? {
        val transcripts = transcriptDao.observeTranscripts(episodeUuid).first()
        return transcripts.minWithOrNull(compareBy({ it.isGenerated }, { it.type }))
    }
}
