package au.com.shiftyjelly.pocketcasts.chat

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EpisodeChatClearedEvent
import com.automattic.eventhorizon.EpisodeChatErrorType
import com.automattic.eventhorizon.EpisodeChatMessageFailedEvent
import com.automattic.eventhorizon.EpisodeChatMessageSentEvent
import com.automattic.eventhorizon.EpisodeChatShownEvent
import com.automattic.eventhorizon.EventHorizon
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val chatManager = TestChatManager()
    private val networkConnectionWatcher = TestNetworkConnectionWatcher()

    private lateinit var eventSink: TestEventSink
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        eventSink = TestEventSink()
        viewModel = ChatViewModel(
            networkConnectionWatcher = networkConnectionWatcher,
            chatManager = chatManager,
            playbackManager = mock<PlaybackManager> {
                on { playbackStateFlow } doReturn MutableStateFlow(PlaybackState())
            },
            episodeManager = mock<EpisodeManager>(),
            eventHorizon = EventHorizon(eventSink),
            applicationScope = kotlinx.coroutines.CoroutineScope(coroutineRule.testDispatcher),
        )
    }

    @Test
    fun `set episode info creates chat with welcome message when empty`() = runTest {
        viewModel.setEpisodeInfo(
            episodeUuid = EPISODE_UUID,
            episodeTitle = "Episode title",
            episodeSubtitle = "Episode subtitle",
            podcastUuid = PODCAST_UUID,
            podcastTitle = "Podcast title",
            episodeDurationMs = 123_000,
            welcomeMessage = "Welcome",
        )

        viewModel.uiState.test {
            val state = awaitItem()

            assertEquals("Episode title", state.episodeTitle)
            assertEquals("Episode subtitle", state.episodeSubtitle)
            assertEquals(PODCAST_UUID, state.podcastUuid)
            assertEquals("Podcast title", state.podcastTitle)
            assertEquals(123_000, state.episodeDurationMs)
            assertEquals(listOf(ChatMessage.Assistant(text = "Welcome", uuid = "welcome-uuid")), state.messages)
        }
        assertEquals(CreateChat(EPISODE_UUID, PODCAST_UUID, "Welcome"), chatManager.createdChats.single())
    }

    @Test
    fun `set episode info tracks chat shown`() = runTest {
        setEpisodeInfo()

        assertEquals(
            EpisodeChatShownEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `set episode info does not create chat when messages exist`() = runTest {
        val message = ChatMessage.User(text = "Existing", uuid = "user-uuid")
        chatManager.messages.value = listOf(message)

        viewModel.setEpisodeInfo(
            episodeUuid = EPISODE_UUID,
            episodeTitle = "Episode title",
            episodeSubtitle = "Episode subtitle",
            podcastUuid = PODCAST_UUID,
            podcastTitle = "Podcast title",
            episodeDurationMs = 123_000,
            welcomeMessage = "Welcome",
        )

        viewModel.uiState.test {
            assertEquals(listOf(message), awaitItem().messages)
        }
        assertTrue(chatManager.createdChats.isEmpty())
    }

    @Test
    fun `input text updates can send state`() = runTest {
        viewModel.uiState.test {
            assertFalse(awaitItem().canSend)

            viewModel.onInputTextChange("Question")

            val state = awaitItem()
            assertEquals("Question", state.inputText)
            assertFalse(state.canSend)
        }
    }

    @Test
    fun `send trims input and delegates to chat manager`() = runTest {
        setEpisodeInfo()
        viewModel.onInputTextChange("  What happened?  ")

        viewModel.onSend()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.inputText)
        assertEquals(
            SendMessage(
                episodeUuid = EPISODE_UUID,
                message = "What happened?",
                isRetry = false,
            ),
            chatManager.sentMessages.single(),
        )
        assertFalse(viewModel.uiState.value.isAwaitingReply)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `send tracks message sent after success`() = runTest {
        setEpisodeInfo()
        eventSink.skipEvent()
        viewModel.onInputTextChange("Question")

        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatMessageSentEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                messageLength = "Question".length.toLong(),
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `network failure sets network error`() = runTest {
        setEpisodeInfo()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Question")

        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(ChatError.NetworkError, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isAwaitingReply)
    }

    @Test
    fun `network failure tracks message failed`() = runTest {
        setEpisodeInfo()
        eventSink.skipEvent()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Question")

        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatMessageFailedEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                error = EpisodeChatErrorType.Network,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `retry resends last user message`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User(text = "First question", uuid = "first-user-uuid"),
            ChatMessage.Assistant(text = "Failure", uuid = "assistant-uuid"),
            ChatMessage.User(text = "Last question", uuid = "last-user-uuid"),
        )
        setEpisodeInfo()

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            SendMessage(
                episodeUuid = EPISODE_UUID,
                message = "Last question",
                isRetry = true,
            ),
            chatManager.sentMessages.single(),
        )
    }

    @Test
    fun `clear chat cancels waiting state and restores welcome message`() = runTest {
        setEpisodeInfo()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(ChatError.NetworkError, viewModel.uiState.value.error)

        viewModel.clearChat()
        advanceUntilIdle()

        assertEquals(ClearMessages(EPISODE_UUID, "Welcome"), chatManager.clearedMessages.single())
        assertEquals(listOf(ChatMessage.Assistant(text = "Welcome", uuid = "welcome-uuid")), viewModel.uiState.value.messages)
        assertEquals(null, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isAwaitingReply)
    }

    @Test
    fun `clear chat tracks chat cleared`() = runTest {
        setEpisodeInfo()
        eventSink.skipEvent()

        viewModel.clearChat()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatClearedEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
    }

    private fun setEpisodeInfo() {
        viewModel.setEpisodeInfo(
            episodeUuid = EPISODE_UUID,
            episodeTitle = "Episode title",
            episodeSubtitle = "Episode subtitle",
            podcastUuid = PODCAST_UUID,
            podcastTitle = "Podcast title",
            episodeDurationMs = 123_000,
            welcomeMessage = "Welcome",
        )
    }

    private class TestNetworkConnectionWatcher : NetworkConnectionWatcher {
        override val networkCapabilities: StateFlow<android.net.NetworkCapabilities?> = MutableStateFlow(null)
    }

    private class TestChatManager : ChatManager {
        val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        val createdChats = mutableListOf<CreateChat>()
        val sentMessages = mutableListOf<SendMessage>()
        val clearedMessages = mutableListOf<ClearMessages>()
        var sendMessageException: Exception? = null

        override fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> = messages

        override suspend fun getMessages(episodeUuid: String): List<ChatMessage> = messages.value

        override suspend fun createChat(episodeUuid: String, podcastUuid: String, welcomeMessage: ChatMessage) {
            createdChats += CreateChat(episodeUuid, podcastUuid, (welcomeMessage as ChatMessage.Assistant).text)
            messages.value = listOf(welcomeMessage.copy(uuid = "welcome-uuid"))
        }

        override suspend fun sendMessage(
            episodeUuid: String,
            message: String,
            allMessages: List<ChatMessage>,
            isRetry: Boolean,
        ) {
            sendMessageException?.let { throw it }
            sentMessages += SendMessage(episodeUuid, message, isRetry)
        }

        override suspend fun clearMessages(episodeUuid: String, welcomeMessage: ChatMessage) {
            clearedMessages += ClearMessages(episodeUuid, (welcomeMessage as ChatMessage.Assistant).text)
            messages.value = listOf(welcomeMessage.copy(uuid = "welcome-uuid"))
        }
    }

    private data class CreateChat(val episodeUuid: String, val podcastUuid: String, val welcomeMessage: String)
    private data class SendMessage(val episodeUuid: String, val message: String, val isRetry: Boolean)
    private data class ClearMessages(val episodeUuid: String, val welcomeMessage: String)

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val PODCAST_UUID = "podcast-uuid"
    }
}
