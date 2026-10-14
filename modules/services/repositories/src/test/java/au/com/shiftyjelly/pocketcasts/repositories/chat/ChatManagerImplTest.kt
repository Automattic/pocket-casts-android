package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.servers.podcast.ConversationMessage
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatQuote
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatRequest
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeChatResponse
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import com.squareup.moshi.Moshi
import java.io.IOException
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    private val tokenHandler = TestTokenHandler()
    private val manager = ChatManagerImpl(
        episodeChatDao = episodeChatDao,
        transcriptDao = transcriptDao,
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
        transcripts.value = listOf(
            createTranscript(type = "text/vtt", isGenerated = true, url = "generated-url"),
            createTranscript(type = "application/json", isGenerated = false, url = "author-url"),
        )
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
                transcriptUrl = "author-url",
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
        val quote = listOf(quoteMessage).toChatMessages(quoteAdapter()).single() as ChatMessage.Quote
        assertEquals("Quoted text", quote.text)
        assertEquals("00:00:01", quote.start)
        assertEquals("00:00:03", quote.end)
        assertEquals(1_000, quote.startMs)
        assertEquals(3_000, quote.endMs)
    }

    @Test
    fun `send message stores user message after successful response`() = runTest {
        transcripts.value = listOf(createTranscript())
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenReturn(
            EpisodeChatResponse(reply = "Assistant reply", quote = null),
        )

        manager.sendMessage(
            episodeUuid = EPISODE_UUID,
            message = ChatMessage.User(text = "Retry message", uuid = "retry-user-uuid"),
            allMessages = emptyList(),
        )

        assertEquals(listOf(ChatRole.User.value, ChatRole.Assistant.value), episodeChatDao.messages.map { it.role })
        assertEquals("retry-user-uuid", episodeChatDao.messages[0].uuid)
        assertEquals("Retry message", episodeChatDao.messages[0].text)
        assertEquals("Assistant reply", episodeChatDao.messages[1].text)
    }

    @Test
    fun `send message requires transcript`() = runTest {
        val exception = runCatching {
            manager.sendMessage(
                episodeUuid = EPISODE_UUID,
                message = ChatMessage.User(text = "Question", uuid = "user-uuid"),
                allMessages = emptyList(),
            )
        }.exceptionOrNull() as IllegalStateException

        assertEquals("Transcript URL is required to send episode chat messages", exception.message)
        assertEquals(emptyList<String>(), episodeChatDao.messages.map { it.role })
    }

    @Test
    fun `send message does not store user message when request fails`() = runTest {
        transcripts.value = listOf(createTranscript())
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenAnswer { throw IOException() }

        val exception = runCatching {
            manager.sendMessage(
                episodeUuid = EPISODE_UUID,
                message = ChatMessage.User(text = "Question", uuid = "user-uuid"),
                allMessages = emptyList(),
            )
        }.exceptionOrNull()

        assertEquals(IOException::class, exception!!::class)
        assertEquals(emptyList<String>(), episodeChatDao.messages.map { it.role })
    }

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
    fun `blank quote text is ignored`() = runTest {
        transcripts.value = listOf(createTranscript())
        whenever(podcastCacheServiceManager.episodeChat(any(), any())).thenReturn(
            EpisodeChatResponse(
                reply = "Assistant reply",
                quote = EpisodeChatQuote(text = " ", start = "00:01", end = "00:03"),
            ),
        )

        manager.sendMessage(
            episodeUuid = EPISODE_UUID,
            message = ChatMessage.User(text = "Question", uuid = "user-uuid"),
            allMessages = emptyList(),
        )

        assertEquals(listOf(ChatRole.User.value, ChatRole.Assistant.value), episodeChatDao.messages.map { it.role })
        assertNull(episodeChatDao.messages.single { it.role == ChatRole.Assistant.value }.metadata)
    }

    private fun quoteAdapter() = Moshi.Builder().build().adapter(QuoteMetadata::class.java)

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
