package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript
import au.com.shiftyjelly.pocketcasts.servers.podcast.ConversationMessage
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatQuote
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatRequest
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import com.squareup.moshi.Moshi
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class ChatManagerImpl @Inject constructor(
    private val episodeChatDao: EpisodeChatDao,
    private val transcriptDao: TranscriptDao,
    private val podcastCacheServiceManager: PodcastCacheServiceManager,
    private val tokenHandler: TokenHandler,
    moshi: Moshi,
) : ChatManager {
    private val quoteMetadataAdapter = moshi.adapter(QuoteMetadata::class.java)

    override fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> {
        return episodeChatDao.observeMessages(episodeUuid).map { it.toChatMessages(quoteMetadataAdapter) }
    }

    override suspend fun createChat(episodeUuid: String, podcastUuid: String) {
        episodeChatDao.insertChatIfAbsent(EpisodeChat(episodeUuid = episodeUuid, podcastUuid = podcastUuid))
    }

    override suspend fun sendMessage(
        episodeUuid: String,
        message: ChatMessage.User,
        allMessages: List<ChatMessage>,
    ) {
        val history = allMessages.mapNotNull { msg ->
            val content = msg.textOrNull() ?: return@mapNotNull null
            ConversationMessage(role = msg.role.apiRole, content = content)
        }

        val transcript = checkNotNull(selectTranscript(episodeUuid)) {
            "Transcript URL is required to send episode chat messages"
        }
        val request = EpisodeChatRequest(
            transcriptUrl = transcript.url,
            message = message.text,
            conversationHistory = history,
        )

        val accessToken = checkNotNull(tokenHandler.getAccessToken()) {
            "Access token is required to send episode chat messages"
        }
        val response = podcastCacheServiceManager.episodeChat(
            authorization = "Bearer ${accessToken.value}",
            request = request,
        )

        val aiReply = ChatMessage.Assistant(text = response.reply)
        val messages = listOfNotNull(message, aiReply, response.quote?.toQuoteMessage())
        val createdAtMs = System.currentTimeMillis()
        episodeChatDao.insertMessages(
            messages.mapIndexed { index, chatMessage ->
                chatMessage.toEntity(episodeUuid, quoteMetadataAdapter).copy(createdAt = Date(createdAtMs + index))
            },
        )
    }

    private fun EpisodeChatQuote.toQuoteMessage(): ChatMessage.Quote? {
        val quoteText = text.takeIf { it.isNotBlank() } ?: return null
        val quoteStart = start.orEmpty()
        val quoteEnd = end.orEmpty()
        return ChatMessage.Quote(
            text = quoteText,
            start = quoteStart,
            end = quoteEnd,
            startMs = parseTimestampMs(quoteStart) ?: -1,
            endMs = parseTimestampMs(quoteEnd) ?: -1,
        )
    }

    override suspend fun clearMessages(episodeUuid: String) {
        episodeChatDao.deleteMessagesByEpisode(episodeUuid)
    }

    // Pick transcript for chat: prefer author-provided (non-generated) over Pocket Casts-generated.
    private suspend fun selectTranscript(episodeUuid: String): Transcript? {
        val transcripts = transcriptDao.observeTranscripts(episodeUuid).first()
        return transcripts.minWithOrNull(compareBy({ it.isGenerated }, { it.type }))
    }
}
