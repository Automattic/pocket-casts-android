package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import au.com.shiftyjelly.pocketcasts.servers.podcast.ConversationMessage
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatQuote
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatRequest
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatResponse
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import com.squareup.moshi.Moshi
import java.io.IOException
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val userMetadataAdapter = moshi.adapter(UserMessageMetadata::class.java)

    override fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> {
        return episodeChatDao.observeMessages(episodeUuid).map { it.toChatMessages(quoteMetadataAdapter, userMetadataAdapter) }
    }

    override fun observeHistory(): Flow<List<EpisodeChatHistoryItem>> {
        return episodeChatDao.observeChatHistory().distinctUntilChanged()
    }

    override suspend fun hasChatWithMessages(episodeUuid: String): Boolean {
        return episodeChatDao.hasChatWithMessages(episodeUuid)
    }

    override suspend fun createChat(episodeUuid: String, podcastUuid: String) {
        episodeChatDao.insertChatIfAbsent(EpisodeChat(episodeUuid = episodeUuid, podcastUuid = podcastUuid))
    }

    override suspend fun sendMessage(
        episodeUuid: String,
        podcastUuid: String,
        message: ChatMessage.User,
        allMessages: List<ChatMessage>,
    ) {
        val question = message.copy(failure = null).toEntity(episodeUuid, quoteMetadataAdapter, userMetadataAdapter)
        episodeChatDao.insertMessage(question)
        val reply = try {
            requestReply(episodeUuid, podcastUuid, message, allMessages)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val failure = if (e is IOException) ChatSendFailure.Network else ChatSendFailure.Server
            val failedQuestion = message.copy(failure = failure).toEntity(episodeUuid, quoteMetadataAdapter, userMetadataAdapter)
            runCatching { episodeChatDao.insertMessage(failedQuestion.copy(createdAt = question.createdAt)) }
            throw e
        }

        val aiReply = ChatMessage.Assistant(text = reply.reply)
        val replies = listOfNotNull(
            aiReply,
            reply.quote?.toQuoteMessage(),
        )
        val createdAtMs = maxOf(System.currentTimeMillis(), question.createdAt.time + 1)
        episodeChatDao.insertMessages(
            replies.mapIndexed { index, chatMessage ->
                chatMessage.toEntity(episodeUuid, quoteMetadataAdapter, userMetadataAdapter)
                    .copy(createdAt = Date(createdAtMs + index))
            },
        )
    }

    private suspend fun requestReply(
        episodeUuid: String,
        podcastUuid: String,
        message: ChatMessage.User,
        allMessages: List<ChatMessage>,
    ): EpisodeChatResponse {
        val history = allMessages.mapNotNull { msg ->
            val content = msg.textOrNull() ?: return@mapNotNull null
            ConversationMessage(role = msg.role.apiRole, content = content)
        }

        val transcript = checkNotNull(selectTranscript(episodeUuid)) {
            "Transcript URL is required to send episode chat messages"
        }
        val request = EpisodeChatRequest(
            podcastUuid = podcastUuid,
            episodeUuid = episodeUuid,
            transcriptUrl = transcript.url,
            message = message.text,
            conversationHistory = history,
        )

        val accessToken = checkNotNull(tokenHandler.getAccessToken()) {
            "Access token is required to send episode chat messages"
        }
        return podcastCacheServiceManager.episodeChat(
            authorization = "Bearer ${accessToken.value}",
            request = request,
        )
    }

    override suspend fun discardMessage(uuid: String) {
        episodeChatDao.deleteMessage(uuid)
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
            isFromGeneratedTranscript = true,
        )
    }

    override suspend fun clearMessages(episodeUuid: String): ClearedChat {
        val entities = episodeChatDao.takeMessages(episodeUuid)
        return StoredClearedChat(entities, entities.toChatMessages(quoteMetadataAdapter, userMetadataAdapter))
    }

    override suspend fun restoreMessages(clearedChat: ClearedChat) {
        if (clearedChat is StoredClearedChat) {
            episodeChatDao.insertMessages(clearedChat.entities)
        }
    }

    override suspend fun rateAnswer(answerUuid: String, rating: ChatAnswerRating?) {
        episodeChatDao.updateRating(answerUuid, rating?.value)
    }

    override suspend fun deleteAllChats() {
        episodeChatDao.deleteAllChats()
    }

    private suspend fun selectTranscript(episodeUuid: String): Transcript? {
        return transcriptDao.observeTranscripts(episodeUuid).first().firstOrNull { it.isGenerated }
    }
}
