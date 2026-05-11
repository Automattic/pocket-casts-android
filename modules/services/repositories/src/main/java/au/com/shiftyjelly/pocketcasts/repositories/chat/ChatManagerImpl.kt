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

    override suspend fun getMessages(episodeUuid: String): List<ChatMessage> {
        return episodeChatDao.getMessages(episodeUuid).toChatMessages(quoteMetadataAdapter)
    }

    override suspend fun createChat(episodeUuid: String, podcastUuid: String, welcomeMessage: ChatMessage) {
        episodeChatDao.insertChat(EpisodeChat(episodeUuid = episodeUuid, podcastUuid = podcastUuid))
        episodeChatDao.insertMessage(welcomeMessage.toEntity(episodeUuid, quoteMetadataAdapter))
    }

    override suspend fun sendMessage(
        episodeUuid: String,
        message: String,
        allMessages: List<ChatMessage>,
        isRetry: Boolean,
    ) {
        if (!isRetry) {
            val userMessage = ChatMessage.User(text = message)
            episodeChatDao.insertMessage(userMessage.toEntity(episodeUuid, quoteMetadataAdapter))
        }

        val history = allMessages.mapNotNull { msg ->
            val content = msg.textOrNull() ?: return@mapNotNull null
            ConversationMessage(role = msg.role.apiRole, content = content)
        }

        val transcript = checkNotNull(selectTranscript(episodeUuid)) {
            "Transcript URL is required to send episode chat messages"
        }
        val request = EpisodeChatRequest(
            transcriptUrl = transcript.url,
            message = message,
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
        episodeChatDao.insertMessage(aiReply.toEntity(episodeUuid, quoteMetadataAdapter))

        persistQuoteIfPresent(episodeUuid, response.quote)
    }

    private suspend fun persistQuoteIfPresent(episodeUuid: String, quote: EpisodeChatQuote?) {
        val quoteText = quote?.text?.takeIf { it.isNotBlank() } ?: return
        val quoteStart = quote.start.orEmpty()
        val quoteEnd = quote.end.orEmpty()
        val quoteMessage = ChatMessage.Quote(
            text = quoteText,
            start = quoteStart,
            end = quoteEnd,
            startMs = parseTimestampMs(quoteStart) ?: -1,
            endMs = parseTimestampMs(quoteEnd) ?: -1,
        )
        episodeChatDao.insertMessage(quoteMessage.toEntity(episodeUuid, quoteMetadataAdapter))
    }

    override suspend fun clearMessages(episodeUuid: String, welcomeMessage: ChatMessage) {
        episodeChatDao.deleteMessagesByEpisode(episodeUuid)
        episodeChatDao.insertMessage(welcomeMessage.toEntity(episodeUuid, quoteMetadataAdapter))
    }

    // Pick transcript for chat: prefer author-provided (non-generated) over Pocket Casts-generated.
    private suspend fun selectTranscript(episodeUuid: String): Transcript? {
        val transcripts = transcriptDao.observeTranscripts(episodeUuid).first()
        return transcripts.minWithOrNull(compareBy({ it.isGenerated }, { it.type }))
    }
}
