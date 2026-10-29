package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import au.com.shiftyjelly.pocketcasts.servers.podcast.ConversationMessage
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatQuote
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatRequest
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatResponse
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatStarterQuestionsRequest
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.squareup.moshi.Moshi
import java.io.IOException
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class ChatManagerImpl @Inject constructor(
    private val episodeChatDao: EpisodeChatDao,
    private val podcastCacheServiceManager: PodcastCacheServiceManager,
    private val tokenHandler: TokenHandler,
    moshi: Moshi,
) : ChatManager {
    private val quoteMetadataAdapter = moshi.adapter(QuoteMetadata::class.java)
    private val userMetadataAdapter = moshi.adapter(UserMessageMetadata::class.java)
    private val assistantMetadataAdapter = moshi.adapter(AssistantMessageMetadata::class.java)
    private val starterQuestions = ConcurrentHashMap<String, List<String>>()

    override fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> {
        return episodeChatDao.observeMessages(episodeUuid).map { it.toChatMessages() }
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
        val question = message.copy(failure = null).toEntity(episodeUuid)
        episodeChatDao.insertMessage(question)
        val reply = try {
            requestReply(episodeUuid, podcastUuid, message, allMessages)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val failure = if (e is IOException) ChatSendFailure.Network else ChatSendFailure.Server
            val failedQuestion = message.copy(failure = failure).toEntity(episodeUuid)
            runCatching { episodeChatDao.insertMessage(failedQuestion.copy(createdAt = question.createdAt)) }
            throw e
        }

        val aiReply = ChatMessage.Assistant(
            text = reply.reply,
            followUps = reply.suggestedQuestions.orEmpty().map(String::trim).filter(String::isNotEmpty).distinct(),
        )
        val replies = listOfNotNull(
            aiReply,
            reply.quote?.toQuoteMessage(),
        )
        val createdAtMs = maxOf(System.currentTimeMillis(), question.createdAt.time + 1)
        episodeChatDao.insertMessages(
            replies.mapIndexed { index, chatMessage ->
                chatMessage.toEntity(episodeUuid)
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

        val request = EpisodeChatRequest(
            podcastUuid = podcastUuid,
            episodeUuid = episodeUuid,
            message = message.text,
            conversationHistory = history,
        )

        return podcastCacheServiceManager.episodeChat(
            authorization = authorization(),
            request = request,
        )
    }

    override suspend fun getStarterQuestions(episodeUuid: String, podcastUuid: String): List<String> {
        starterQuestions[episodeUuid]?.let { return it }
        val questions = try {
            podcastCacheServiceManager.episodeChatStarterQuestions(
                authorization = authorization(),
                request = EpisodeChatStarterQuestionsRequest(podcastUuid = podcastUuid, episodeUuid = episodeUuid),
            ).suggestedQuestions.map(String::trim).filter(String::isNotEmpty).distinct()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogBuffer.e(LOG_TAG, e, "Failed to load episode chat starter questions")
            emptyList()
        }
        if (questions.isNotEmpty()) {
            starterQuestions[episodeUuid] = questions
        }
        return questions
    }

    private suspend fun authorization(): String {
        val accessToken = checkNotNull(tokenHandler.getAccessToken()) {
            "Access token is required for episode chat"
        }
        return "Bearer ${accessToken.value}"
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
        return StoredClearedChat(entities, entities.toChatMessages())
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

    private fun List<EpisodeChatMessage>.toChatMessages() = toChatMessages(
        quoteMetadataAdapter,
        userMetadataAdapter,
        assistantMetadataAdapter,
    )

    private fun ChatMessage.toEntity(episodeUuid: String) = toEntity(
        episodeUuid,
        quoteMetadataAdapter,
        userMetadataAdapter,
        assistantMetadataAdapter,
    )

    private companion object {
        const val LOG_TAG = "EpisodeChat"
    }
}
