package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.servers.podcast.ConversationMessage
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatQuote
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatRequest
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatResponse
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatStarterQuestionsRequest
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatStarterQuestionsResponse
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import com.squareup.moshi.Moshi
import java.io.IOException
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ChatManagerImplTest {
    private val episodeChatDao = TestEpisodeChatDao()
    private val podcastCacheServiceManager = mock<PodcastCacheServiceManager>()
    private val tokenHandler = TestTokenHandler()
    private val manager = ChatManagerImpl(
        episodeChatDao = episodeChatDao,
        podcastCacheServiceManager = podcastCacheServiceManager,
        tokenHandler = tokenHandler,
        moshi = Moshi.Builder().build(),
    )

    @Test
    fun `create chat stores chat without messages`() = runTest {
        manager.createChat(
            episodeUuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
        )

        assertEquals(EPISODE_UUID, episodeChatDao.chats.single().episodeUuid)
        assertEquals(PODCAST_UUID, episodeChatDao.chats.single().podcastUuid)
        assertEquals(emptyList<EpisodeChatMessage>(), episodeChatDao.messages)
    }

    @Test
    fun `create chat keeps existing chat`() = runTest {
        val existingChat = EpisodeChat(episodeUuid = EPISODE_UUID, podcastUuid = PODCAST_UUID, createdAt = Date(100))
        episodeChatDao.chats += existingChat

        manager.createChat(
            episodeUuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
        )

        assertEquals(listOf(existingChat), episodeChatDao.chats)
    }

    @Test
    fun `send message stores user message and assistant response with quote`() = runTest {
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenReturn(
            EpisodeChatResponse(
                reply = "Assistant reply",
                quote = EpisodeChatQuote(
                    text = "Quoted text",
                    start = "00:00:01",
                    end = "00:00:03",
                ),
            ),
        )

        manager.sendMessage(
            episodeUuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
            message = ChatMessage.User(text = "What happened?", uuid = "user-uuid"),
            allMessages = listOf(
                ChatMessage.User(text = "Earlier question", uuid = "earlier-user-uuid"),
                ChatMessage.Quote(text = "Earlier quote", start = "00:00", end = "00:01", uuid = "quote-uuid"),
            ),
        )

        val requestCaptor = argumentCaptor<EpisodeChatRequest>()
        val authorizationCaptor = argumentCaptor<String>()
        verify(podcastCacheServiceManager).episodeChat(authorizationCaptor.capture(), requestCaptor.capture())
        assertEquals("Bearer access-token", authorizationCaptor.firstValue)
        assertEquals(
            EpisodeChatRequest(
                podcastUuid = PODCAST_UUID,
                episodeUuid = EPISODE_UUID,
                message = "What happened?",
                conversationHistory = listOf(
                    ConversationMessage(role = "user", content = "Earlier question"),
                    ConversationMessage(role = "assistant", content = "Earlier quote"),
                ),
            ),
            requestCaptor.firstValue,
        )
        assertEquals(listOf(ChatRole.User.value, ChatRole.Assistant.value, ChatRole.Quote.value), episodeChatDao.messages.map { it.role })
        assertTrue(episodeChatDao.messages.zipWithNext().all { (first, second) -> first.createdAt < second.createdAt })
        assertEquals("user-uuid", episodeChatDao.messages[0].uuid)
        assertEquals("What happened?", episodeChatDao.messages[0].text)
        assertEquals("Assistant reply", episodeChatDao.messages[1].text)

        val quoteMessage = episodeChatDao.messages[2]
        assertEquals("Quoted text", quoteMessage.text)
        assertEquals(ChatRole.Quote.value, quoteMessage.role)
        val quote = listOf(quoteMessage).toChatMessages(quoteAdapter(), userAdapter(), assistantAdapter()).single() as ChatMessage.Quote
        assertEquals("Quoted text", quote.text)
        assertEquals("00:00:01", quote.start)
        assertEquals("00:00:03", quote.end)
        assertEquals(1_000, quote.startMs)
        assertEquals(3_000, quote.endMs)
        assertEquals(true, quote.isFromGeneratedTranscript)
    }

    @Test
    fun `send message marks quotes from the generated transcript`() = runTest {
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenReturn(
            EpisodeChatResponse(
                reply = "Assistant reply",
                quote = EpisodeChatQuote(text = "Quoted text", start = "00:00:01", end = "00:00:03"),
            ),
        )

        manager.sendMessage(
            episodeUuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
            message = ChatMessage.User(text = "Question", uuid = "user-uuid"),
            allMessages = emptyList(),
        )

        val quote = episodeChatDao.messages.toChatMessages(quoteAdapter(), userAdapter(), assistantAdapter()).filterIsInstance<ChatMessage.Quote>().single()
        assertEquals(true, quote.isFromGeneratedTranscript)
    }

    @Test
    fun `quotes stored before the generated transcript flag read as not generated`() {
        val entity = EpisodeChatMessage(
            uuid = "quote-uuid",
            episodeUuid = EPISODE_UUID,
            text = "Quoted text",
            role = ChatRole.Quote.value,
            metadata = """{"start":"00:00:01","end":"00:00:03"}""",
        )

        val quote = listOf(entity).toChatMessages(quoteAdapter(), userAdapter(), assistantAdapter()).single() as ChatMessage.Quote

        assertEquals(1_000, quote.startMs)
        assertEquals(false, quote.isFromGeneratedTranscript)
    }

    @Test
    fun `send message stores the follow-up questions on the answer`() = runTest {
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenReturn(
            EpisodeChatResponse(
                reply = "Assistant reply",
                quote = null,
                suggestedQuestions = listOf(" Who is the guest? ", "", "What changed?", "What changed?"),
            ),
        )

        manager.sendMessage(EPISODE_UUID, PODCAST_UUID, ChatMessage.User(text = "Question", uuid = "user-uuid"), emptyList())

        val answer = manager.observeMessages(EPISODE_UUID).first().filterIsInstance<ChatMessage.Assistant>().single()
        assertEquals(listOf("Who is the guest?", "What changed?"), answer.followUps)
    }

    @Test
    fun `answers stored before follow-ups read without them`() = runTest {
        episodeChatDao.messages += createMessage(uuid = "answer-uuid", episodeUuid = EPISODE_UUID, text = "Answer")

        val answer = manager.observeMessages(EPISODE_UUID).first().single() as ChatMessage.Assistant

        assertEquals(emptyList<String>(), answer.followUps)
    }

    @Test
    fun `send message stores user message after successful response`() = runTest {
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenReturn(
            EpisodeChatResponse(reply = "Assistant reply", quote = null),
        )

        manager.sendMessage(
            episodeUuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
            message = ChatMessage.User(text = "Retry message", uuid = "retry-user-uuid"),
            allMessages = emptyList(),
        )

        assertEquals(listOf(ChatRole.User.value, ChatRole.Assistant.value), episodeChatDao.messages.map { it.role })
        assertEquals("retry-user-uuid", episodeChatDao.messages[0].uuid)
        assertEquals("Retry message", episodeChatDao.messages[0].text)
        assertEquals("Assistant reply", episodeChatDao.messages[1].text)
    }

    @Test
    fun `send message keeps the question as failed on our end when the server fails`() = runTest {
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenAnswer { throw IllegalStateException() }

        runCatching {
            manager.sendMessage(
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                message = ChatMessage.User(text = "Question", uuid = "user-uuid"),
                allMessages = emptyList(),
            )
        }

        assertEquals(listOf(ChatSendFailure.Server), storedQuestions().map { it.failure })
    }

    @Test
    fun `send message keeps the question as not sent when the network fails`() = runTest {
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenAnswer { throw IOException() }

        val exception = runCatching {
            manager.sendMessage(
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                message = ChatMessage.User(text = "Question", uuid = "user-uuid"),
                allMessages = emptyList(),
            )
        }.exceptionOrNull()

        assertEquals(IOException::class, exception!!::class)
        assertEquals(listOf(ChatMessage.User(text = "Question", failure = ChatSendFailure.Network, uuid = "user-uuid")), storedQuestions())
    }

    @Test
    fun `a failed summarize question keeps its input type`() = runTest {
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenAnswer { throw IOException() }
        val question = ChatMessage.User(text = "Summarize", inputType = ChatInputType.SummaryPrompt, uuid = "user-uuid")

        runCatching { manager.sendMessage(EPISODE_UUID, PODCAST_UUID, question, emptyList()) }

        assertEquals(listOf(question.copy(failure = ChatSendFailure.Network)), storedQuestions())
    }

    @Test
    fun `a question stored before input types were saved reads as typed with its failure`() = runTest {
        episodeChatDao.messages += createMessage(uuid = "user-uuid", episodeUuid = EPISODE_UUID, text = "Question")
            .copy(role = ChatRole.User.value, metadata = """{"failure":"network"}""")

        assertEquals(
            listOf(ChatMessage.User(text = "Question", failure = ChatSendFailure.Network, inputType = ChatInputType.Typed, uuid = "user-uuid")),
            storedQuestions(),
        )
    }

    @Test
    fun `send message stores the question before the reply arrives`() = runTest {
        var storedBeforeReply = emptyList<String>()
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenAnswer {
            storedBeforeReply = episodeChatDao.messages.map { it.uuid }
            EpisodeChatResponse(reply = "Assistant reply", quote = null)
        }

        manager.sendMessage(
            episodeUuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
            message = ChatMessage.User(text = "Question", uuid = "user-uuid"),
            allMessages = emptyList(),
        )

        assertEquals(listOf("user-uuid"), storedBeforeReply)
    }

    @Test
    fun `resending a failed question clears its failure once answered`() = runTest {
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenAnswer { throw IOException() }
        runCatching {
            manager.sendMessage(EPISODE_UUID, PODCAST_UUID, ChatMessage.User(text = "Question", uuid = "user-uuid"), emptyList())
        }
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenReturn(
            EpisodeChatResponse(reply = "Assistant reply", quote = null),
        )

        manager.sendMessage(EPISODE_UUID, PODCAST_UUID, ChatMessage.User(text = "Question", uuid = "user-uuid"), emptyList())

        assertEquals(listOf(ChatMessage.User(text = "Question", uuid = "user-uuid")), storedQuestions())
        assertEquals(listOf(ChatRole.User.value, ChatRole.Assistant.value), episodeChatDao.messages.sortedBy { it.createdAt }.map { it.role })
    }

    @Test
    fun `starter questions are requested for the episode`() = runTest {
        whenever(podcastCacheServiceManager.episodeChatStarterQuestions(any(), any())).thenReturn(
            EpisodeChatStarterQuestionsResponse(suggestedQuestions = listOf(" Who is the guest? ", "", "What changed?", "Who is the guest?")),
        )

        val questions = manager.getStarterQuestions(EPISODE_UUID, PODCAST_UUID)

        verify(podcastCacheServiceManager).episodeChatStarterQuestions(
            "Bearer access-token",
            EpisodeChatStarterQuestionsRequest(podcastUuid = PODCAST_UUID, episodeUuid = EPISODE_UUID),
        )
        assertEquals(listOf("Who is the guest?", "What changed?"), questions)
    }

    @Test
    fun `starter questions are kept for the episode`() = runTest {
        whenever(podcastCacheServiceManager.episodeChatStarterQuestions(any(), any())).thenReturn(
            EpisodeChatStarterQuestionsResponse(suggestedQuestions = listOf("Who is the guest?")),
        )

        manager.getStarterQuestions(EPISODE_UUID, PODCAST_UUID)
        val questions = manager.getStarterQuestions(EPISODE_UUID, PODCAST_UUID)

        verify(podcastCacheServiceManager, times(1)).episodeChatStarterQuestions(any(), any())
        assertEquals(listOf("Who is the guest?"), questions)
    }

    @Test
    fun `starter questions are empty and retried when the request fails`() = runTest {
        whenever(podcastCacheServiceManager.episodeChatStarterQuestions(any(), any())).thenAnswer { throw IOException() }

        assertEquals(emptyList<String>(), manager.getStarterQuestions(EPISODE_UUID, PODCAST_UUID))
        assertEquals(emptyList<String>(), manager.getStarterQuestions(EPISODE_UUID, PODCAST_UUID))

        verify(podcastCacheServiceManager, times(2)).episodeChatStarterQuestions(any(), any())
    }

    @Test
    fun `discarding a message deletes it`() = runTest {
        episodeChatDao.messages += createMessage(uuid = "message-uuid", episodeUuid = EPISODE_UUID, text = "Question")

        manager.discardMessage("message-uuid")

        assertEquals(emptyList<EpisodeChatMessage>(), episodeChatDao.messages)
    }

    private suspend fun storedQuestions() = manager.observeMessages(EPISODE_UUID).first().filterIsInstance<ChatMessage.User>()

    @Test
    fun `clear messages deletes episode messages`() = runTest {
        episodeChatDao.messages += listOf(
            createMessage(uuid = "old-message", episodeUuid = EPISODE_UUID, text = "Old message"),
            createMessage(uuid = "other-message", episodeUuid = "other-episode-uuid", text = "Other message"),
        )

        manager.clearMessages(episodeUuid = EPISODE_UUID)

        assertEquals(listOf(EPISODE_UUID), episodeChatDao.deletedEpisodeUuids)
        assertEquals(listOf("other-message"), episodeChatDao.messages.map { it.uuid })
    }

    @Test
    fun `an episode with chat messages has a chat`() = runTest {
        episodeChatDao.chats += EpisodeChat(episodeUuid = EPISODE_UUID, podcastUuid = PODCAST_UUID)
        episodeChatDao.messages += createMessage(uuid = "message-uuid", episodeUuid = EPISODE_UUID, text = "Question")

        assertTrue(manager.hasChatWithMessages(EPISODE_UUID))
    }

    @Test
    fun `an episode with an empty chat has no chat`() = runTest {
        episodeChatDao.chats += EpisodeChat(episodeUuid = EPISODE_UUID, podcastUuid = PODCAST_UUID)

        assertFalse(manager.hasChatWithMessages(EPISODE_UUID))
    }

    @Test
    fun `an episode without a chat has no chat`() = runTest {
        assertFalse(manager.hasChatWithMessages(EPISODE_UUID))
    }

    @Test
    fun `restoring a cleared chat brings back its messages and ratings`() = runTest {
        val answer = createMessage(uuid = "answer-uuid", episodeUuid = EPISODE_UUID, text = "Answer").copy(rating = "positive")
        episodeChatDao.messages += answer

        val clearedChat = manager.clearMessages(episodeUuid = EPISODE_UUID)

        assertEquals(listOf("answer-uuid"), clearedChat.messages.map { it.uuid })
        assertEquals(emptyList<EpisodeChatMessage>(), episodeChatDao.messages)

        manager.restoreMessages(clearedChat)

        assertEquals(listOf(answer), episodeChatDao.messages)
    }

    @Test
    fun `rate answer stores and clears the rating`() = runTest {
        episodeChatDao.messages += createMessage(uuid = "answer-uuid", episodeUuid = EPISODE_UUID, text = "Answer")

        manager.rateAnswer(answerUuid = "answer-uuid", rating = ChatAnswerRating.Negative)

        assertEquals("negative", episodeChatDao.messages.single().rating)

        manager.rateAnswer(answerUuid = "answer-uuid", rating = null)

        assertNull(episodeChatDao.messages.single().rating)
    }

    @Test
    fun `observed answers carry their stored rating`() = runTest {
        episodeChatDao.messages += createMessage(uuid = "answer-uuid", episodeUuid = EPISODE_UUID, text = "Answer")
            .copy(rating = "positive")

        val messages = manager.observeMessages(EPISODE_UUID).first()

        assertEquals(ChatAnswerRating.Positive, messages.single().answerRating)
    }

    @Test
    fun `blank quote text is ignored`() = runTest {
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenReturn(
            EpisodeChatResponse(
                reply = "Assistant reply",
                quote = EpisodeChatQuote(text = " ", start = "00:01", end = "00:03"),
            ),
        )

        manager.sendMessage(
            episodeUuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
            message = ChatMessage.User(text = "Question", uuid = "user-uuid"),
            allMessages = emptyList(),
        )

        assertEquals(listOf(ChatRole.User.value, ChatRole.Assistant.value), episodeChatDao.messages.map { it.role })
        assertNull(episodeChatDao.messages.single { it.role == ChatRole.Assistant.value }.metadata)
    }

    @Test
    fun `delete all chats removes every chat and message`() = runTest {
        episodeChatDao.chats += EpisodeChat(episodeUuid = EPISODE_UUID, podcastUuid = PODCAST_UUID)
        episodeChatDao.chats += EpisodeChat(episodeUuid = "other-episode-uuid", podcastUuid = PODCAST_UUID)
        episodeChatDao.messages += createMessage(uuid = "message-1", episodeUuid = EPISODE_UUID, text = "Answer")
        episodeChatDao.messages += createMessage(uuid = "message-2", episodeUuid = "other-episode-uuid", text = "Answer")

        manager.deleteAllChats()

        assertEquals(emptyList<EpisodeChat>(), episodeChatDao.chats)
        assertEquals(emptyList<EpisodeChatMessage>(), episodeChatDao.messages)
    }

    private fun quoteAdapter() = Moshi.Builder().build().adapter(QuoteMetadata::class.java)

    private fun userAdapter() = Moshi.Builder().build().adapter(UserMessageMetadata::class.java)

    private fun assistantAdapter() = Moshi.Builder().build().adapter(AssistantMessageMetadata::class.java)

    private class TestTokenHandler : TokenHandler {
        override suspend fun getAccessToken() = AccessToken("access-token")

        override fun invalidateAccessToken() = Unit
    }

    private class TestEpisodeChatDao : EpisodeChatDao() {
        val chats = mutableListOf<EpisodeChat>()
        val messages = mutableListOf<EpisodeChatMessage>()
        val deletedEpisodeUuids = mutableListOf<String>()
        override suspend fun insertChatIfAbsent(chat: EpisodeChat) {
            if (chats.none { it.episodeUuid == chat.episodeUuid }) {
                chats += chat
            }
        }

        override suspend fun getChatByEpisode(episodeUuid: String): EpisodeChat? {
            return chats.firstOrNull { it.episodeUuid == episodeUuid }
        }

        override suspend fun deleteChat(episodeUuid: String) {
            chats.removeAll { it.episodeUuid == episodeUuid }
        }

        override suspend fun deleteAllChats() {
            chats.clear()
            messages.clear()
        }

        override suspend fun insertMessage(message: EpisodeChatMessage) {
            insertMessages(listOf(message))
        }

        override suspend fun insertMessages(messages: List<EpisodeChatMessage>) {
            val uuids = messages.map { it.uuid }.toSet()
            this.messages.removeAll { it.uuid in uuids }
            this.messages += messages
        }

        override fun observeMessages(episodeUuid: String): Flow<List<EpisodeChatMessage>> {
            return MutableStateFlow(messages.filter { it.episodeUuid == episodeUuid }.sortedBy { it.createdAt })
        }

        override suspend fun getMessages(episodeUuid: String): List<EpisodeChatMessage> {
            return messages.filter { it.episodeUuid == episodeUuid }.sortedBy { it.createdAt }
        }

        override suspend fun deleteMessage(uuid: String) {
            messages.removeAll { it.uuid == uuid }
        }

        override suspend fun deleteMessagesByEpisode(episodeUuid: String) {
            deletedEpisodeUuids += episodeUuid
            messages.removeAll { it.episodeUuid == episodeUuid }
        }

        override fun observeChatHistory(): Flow<List<EpisodeChatHistoryItem>> = flowOf(emptyList())

        override suspend fun hasChatWithMessages(episodeUuid: String): Boolean {
            return chats.any { it.episodeUuid == episodeUuid } && messages.any { it.episodeUuid == episodeUuid }
        }

        override suspend fun updateRating(uuid: String, rating: String?) {
            messages.replaceAll { if (it.uuid == uuid) it.copy(rating = rating) else it }
        }
    }

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val PODCAST_UUID = "podcast-uuid"

        fun createMessage(
            uuid: String,
            episodeUuid: String,
            text: String,
        ) = EpisodeChatMessage(
            uuid = uuid,
            episodeUuid = episodeUuid,
            text = text,
            role = ChatRole.Assistant.value,
        )
    }
}
