package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript
import au.com.shiftyjelly.pocketcasts.servers.podcast.ConversationMessage
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatQuote
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatRequest
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatResponse
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ChatManagerImplTest {
    private val episodeChatDao = TestEpisodeChatDao()
    private val transcripts = MutableStateFlow(emptyList<Transcript>())
    private val transcriptDao = mock<TranscriptDao> {
        on { observeTranscripts(any()) } doReturn transcripts
    }
    private val podcastCacheServiceManager = mock<PodcastCacheServiceManager>()
    private val manager = ChatManagerImpl(
        episodeChatDao = episodeChatDao,
        transcriptDao = transcriptDao,
        podcastCacheServiceManager = podcastCacheServiceManager,
        moshi = Moshi.Builder().build(),
    )

    @Test
    fun `create chat stores chat and welcome message`() = runTest {
        manager.createChat(
            episodeUuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
            welcomeMessage = ChatMessage.Assistant(text = "Welcome", uuid = "welcome-uuid"),
        )

        assertEquals(EPISODE_UUID, episodeChatDao.chats.single().episodeUuid)
        assertEquals(PODCAST_UUID, episodeChatDao.chats.single().podcastUuid)
        assertEquals(
            EpisodeChatMessage(
                uuid = "welcome-uuid",
                episodeUuid = EPISODE_UUID,
                text = "Welcome",
                role = ChatRole.Assistant.value,
                createdAt = episodeChatDao.messages.single().createdAt,
            ),
            episodeChatDao.messages.single(),
        )
    }

    @Test
    fun `send message stores user message and assistant response with quote`() = runTest {
        transcripts.value = listOf(
            createTranscript(type = "text/vtt", isGenerated = true, url = "generated-url"),
            createTranscript(type = "application/json", isGenerated = false, url = "author-url"),
        )
        whenever(podcastCacheServiceManager.episodeChat(any())).thenReturn(
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
            message = "What happened?",
            allMessages = listOf(
                ChatMessage.Assistant(text = "Welcome", uuid = "welcome-uuid"),
                ChatMessage.User(text = "Earlier question", uuid = "earlier-user-uuid"),
                ChatMessage.Quote(text = "Earlier quote", start = "00:00", end = "00:01", uuid = "quote-uuid"),
            ),
            isRetry = false,
        )

        val requestCaptor = argumentCaptor<EpisodeChatRequest>()
        verify(podcastCacheServiceManager).episodeChat(requestCaptor.capture())
        assertEquals(
            EpisodeChatRequest(
                transcriptUrl = "author-url",
                message = "What happened?",
                conversationHistory = listOf(
                    ConversationMessage(role = "assistant", content = "Welcome"),
                    ConversationMessage(role = "user", content = "Earlier question"),
                    ConversationMessage(role = "assistant", content = "Earlier quote"),
                ),
            ),
            requestCaptor.firstValue,
        )
        assertEquals(listOf(ChatRole.User.value, ChatRole.Assistant.value, ChatRole.Quote.value), episodeChatDao.messages.map { it.role })
        assertEquals("What happened?", episodeChatDao.messages[0].text)
        assertEquals("Assistant reply", episodeChatDao.messages[1].text)

        val quoteMessage = episodeChatDao.messages[2]
        assertEquals("Quoted text", quoteMessage.text)
        assertEquals(ChatRole.Quote.value, quoteMessage.role)
        val quote = listOf(quoteMessage).toChatMessages(quoteAdapter()).single() as ChatMessage.Quote
        assertEquals("Quoted text", quote.text)
        assertEquals("00:00:01", quote.start)
        assertEquals("00:00:03", quote.end)
        assertEquals(1_000, quote.startMs)
        assertEquals(3_000, quote.endMs)
    }

    @Test
    fun `send retry skips storing user message`() = runTest {
        transcripts.value = listOf(createTranscript())
        whenever(podcastCacheServiceManager.episodeChat(any())).thenReturn(EpisodeChatResponse(reply = "Assistant reply", quote = null))

        manager.sendMessage(
            episodeUuid = EPISODE_UUID,
            message = "Retry message",
            allMessages = emptyList(),
            isRetry = true,
        )

        assertEquals(listOf(ChatRole.Assistant.value), episodeChatDao.messages.map { it.role })
        assertEquals("Assistant reply", episodeChatDao.messages.single().text)
    }

    @Test
    fun `send message requires transcript`() = runTest {
        val exception = runCatching {
            manager.sendMessage(
                episodeUuid = EPISODE_UUID,
                message = "Question",
                allMessages = emptyList(),
                isRetry = false,
            )
        }.exceptionOrNull() as IllegalStateException

        assertEquals("Transcript URL is required to send episode chat messages", exception.message)
        assertEquals(listOf(ChatRole.User.value), episodeChatDao.messages.map { it.role })
    }

    @Test
    fun `clear messages deletes episode messages and stores welcome message`() = runTest {
        episodeChatDao.messages += listOf(
            createMessage(uuid = "old-message", episodeUuid = EPISODE_UUID, text = "Old message"),
            createMessage(uuid = "other-message", episodeUuid = "other-episode-uuid", text = "Other message"),
        )

        manager.clearMessages(
            episodeUuid = EPISODE_UUID,
            welcomeMessage = ChatMessage.Assistant(text = "Welcome", uuid = "welcome-uuid"),
        )

        assertEquals(listOf(EPISODE_UUID), episodeChatDao.deletedEpisodeUuids)
        assertEquals(listOf("other-message", "welcome-uuid"), episodeChatDao.messages.map { it.uuid })
        assertEquals(listOf("Other message", "Welcome"), episodeChatDao.messages.map { it.text })
    }

    @Test
    fun `blank quote text is ignored`() = runTest {
        transcripts.value = listOf(createTranscript())
        whenever(podcastCacheServiceManager.episodeChat(any())).thenReturn(
            EpisodeChatResponse(
                reply = "Assistant reply",
                quote = EpisodeChatQuote(text = " ", start = "00:01", end = "00:03"),
            ),
        )

        manager.sendMessage(
            episodeUuid = EPISODE_UUID,
            message = "Question",
            allMessages = emptyList(),
            isRetry = false,
        )

        assertEquals(listOf(ChatRole.User.value, ChatRole.Assistant.value), episodeChatDao.messages.map { it.role })
        assertNull(episodeChatDao.messages.single { it.role == ChatRole.Assistant.value }.metadata)
    }

    private fun quoteAdapter() = Moshi.Builder().build().adapter(QuoteMetadata::class.java)

    private class TestEpisodeChatDao : EpisodeChatDao() {
        val chats = mutableListOf<EpisodeChat>()
        val messages = mutableListOf<EpisodeChatMessage>()
        val deletedEpisodeUuids = mutableListOf<String>()

        override suspend fun insertChat(chat: EpisodeChat) {
            chats.removeAll { it.episodeUuid == chat.episodeUuid }
            chats += chat
        }

        override suspend fun getChatByEpisode(episodeUuid: String): EpisodeChat? {
            return chats.firstOrNull { it.episodeUuid == episodeUuid }
        }

        override suspend fun deleteChat(episodeUuid: String) {
            chats.removeAll { it.episodeUuid == episodeUuid }
        }

        override suspend fun insertMessage(message: EpisodeChatMessage) {
            messages.removeAll { it.uuid == message.uuid }
            messages += message
        }

        override fun observeMessages(episodeUuid: String): Flow<List<EpisodeChatMessage>> {
            return MutableStateFlow(messages.filter { it.episodeUuid == episodeUuid }.sortedBy { it.createdAt })
        }

        override suspend fun getMessages(episodeUuid: String): List<EpisodeChatMessage> {
            return messages.filter { it.episodeUuid == episodeUuid }.sortedBy { it.createdAt }
        }

        override suspend fun deleteMessagesByEpisode(episodeUuid: String) {
            deletedEpisodeUuids += episodeUuid
            messages.removeAll { it.episodeUuid == episodeUuid }
        }
    }

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val PODCAST_UUID = "podcast-uuid"

        fun createTranscript(
            type: String = "text/vtt",
            isGenerated: Boolean = false,
            url: String = "transcript-url",
        ) = Transcript(
            episodeUuid = EPISODE_UUID,
            url = url,
            type = type,
            isGenerated = isGenerated,
            language = "en",
        )

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
