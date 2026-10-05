package au.com.shiftyjelly.pocketcasts.chat

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EpisodeChatBetaSheetDismissedEvent
import com.automattic.eventhorizon.EpisodeChatBetaSheetShownEvent
import com.automattic.eventhorizon.EpisodeChatClearedEvent
import com.automattic.eventhorizon.EpisodeChatErrorType
import com.automattic.eventhorizon.EpisodeChatMessageFailedEvent
import com.automattic.eventhorizon.EpisodeChatMessageSentEvent
import com.automattic.eventhorizon.EpisodeChatShownEvent
import com.automattic.eventhorizon.EventHorizon
import java.io.IOException
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val chatManager = TestChatManager()
    private val playbackState = MutableStateFlow(PlaybackState())
    private val playbackManager = mock<PlaybackManager> {
        on { playbackStateFlow } doReturn playbackState
    }
    private val episode = MutableStateFlow<BaseEpisode>(
        PodcastEpisode(uuid = EPISODE_UUID, publishedDate = Date(), duration = 200.0, playedUpTo = 50.0),
    )
    private val episodeManager = mock<EpisodeManager> {
        on { findEpisodeByUuidFlow(EPISODE_UUID) } doReturn episode
    }
    private val betaSheetSeen = MutableStateFlow(false)
    private val betaSheetSeenSetting = mock<UserSetting<Boolean>> {
        on { value } doAnswer { betaSheetSeen.value }
    }
    private val settings = mock<Settings> {
        on { episodeChatBetaSheetSeen } doReturn betaSheetSeenSetting
    }
    private val networkConnectionWatcher = TestNetworkConnectionWatcher()

    private lateinit var eventSink: TestEventSink
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        eventSink = TestEventSink()
        viewModel = createViewModel()
    }

    private fun createViewModel() = ChatViewModel(
        networkConnectionWatcher = networkConnectionWatcher,
        chatManager = chatManager,
        playbackManager = playbackManager,
        episodeManager = episodeManager,
        eventHorizon = EventHorizon(eventSink),
        settings = settings,
        applicationScope = kotlinx.coroutines.CoroutineScope(coroutineRule.testDispatcher),
    )

    @Test
    fun `set episode info creates chat without messages`() = runTest {
        viewModel.setEpisodeInfo(
            episodeUuid = EPISODE_UUID,
            episodeTitle = "Episode title",
            episodeSubtitle = "Episode subtitle",
            podcastUuid = PODCAST_UUID,
            podcastTitle = "Podcast title",
            episodeDurationMs = 123_000,
            sourceView = SourceView.EPISODE_DETAILS,
            isBeta = false,
        )

        viewModel.uiState.test {
            val state = awaitItem()

            assertEquals("Episode title", state.episodeTitle)
            assertEquals("Episode subtitle", state.episodeSubtitle)
            assertEquals(PODCAST_UUID, state.podcastUuid)
            assertEquals("Podcast title", state.podcastTitle)
            assertEquals(123_000, state.episodeDurationMs)
            assertEquals(emptyList<ChatMessage>(), state.messages)
        }
        assertEquals(CreateChat(EPISODE_UUID, PODCAST_UUID), chatManager.createdChats.single())
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
    fun `set episode info tracks chat shown with the given source`() = runTest {
        setEpisodeInfo(sourceView = SourceView.PLAYER)

        assertEquals(
            EpisodeChatShownEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `set episode info shows stored messages`() = runTest {
        val message = ChatMessage.User(text = "Existing", uuid = "user-uuid")
        chatManager.messages.value = listOf(message)

        viewModel.setEpisodeInfo(
            episodeUuid = EPISODE_UUID,
            episodeTitle = "Episode title",
            episodeSubtitle = "Episode subtitle",
            podcastUuid = PODCAST_UUID,
            podcastTitle = "Podcast title",
            episodeDurationMs = 123_000,
            sourceView = SourceView.EPISODE_DETAILS,
            isBeta = false,
        )

        viewModel.uiState.test {
            assertEquals(listOf(message), awaitItem().messages)
        }
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
            ),
            chatManager.sentMessages.single(),
        )
        assertFalse(viewModel.uiState.value.isAwaitingReply)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `send passes only conversation messages as history`() = runTest {
        setEpisodeInfo()
        viewModel.onInputTextChange("First question")
        viewModel.onSend()
        advanceUntilIdle()
        viewModel.onInputTextChange("Second question")

        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(
            listOf(
                emptyList(),
                listOf("First question", "Response"),
            ),
            chatManager.sentHistories,
        )
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
        val failedMessage = viewModel.uiState.value.messages.last()
        assertTrue(failedMessage is ChatMessage.User)
        assertEquals("Question", (failedMessage as ChatMessage.User).text)
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
    fun `retry resends failed transient user message`() = runTest {
        setEpisodeInfo()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Last question")
        viewModel.onSend()
        advanceUntilIdle()
        chatManager.sendMessageException = null

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            SendMessage(
                episodeUuid = EPISODE_UUID,
                message = "Last question",
            ),
            chatManager.sentMessages.single(),
        )
    }

    @Test
    fun `failed user message is not restored in a new chat session`() = runTest {
        setEpisodeInfo()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Failed question")
        viewModel.onSend()
        advanceUntilIdle()
        val failedMessage = viewModel.uiState.value.messages.last()
        assertTrue(failedMessage is ChatMessage.User)
        assertEquals("Failed question", (failedMessage as ChatMessage.User).text)

        viewModel = createViewModel()
        setEpisodeInfo()
        advanceUntilIdle()

        assertEquals(emptyList<ChatMessage>(), viewModel.uiState.value.messages)
    }

    @Test
    fun `clear chat cancels waiting state and removes messages`() = runTest {
        setEpisodeInfo()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(ChatError.NetworkError, viewModel.uiState.value.error)

        viewModel.clearChat()
        advanceUntilIdle()

        assertEquals(EPISODE_UUID, chatManager.clearedEpisodeUuids.single())
        assertEquals(emptyList<ChatMessage>(), viewModel.uiState.value.messages)
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

    @Test
    fun `quote within episode can play`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)

        assertTrue(playableState(quote))
    }

    @Test
    fun `quote starting after episode end cannot play`() = runTest {
        val quote = createQuote(startMs = 49_020_000, endMs = 49_500_000)

        assertFalse(playableState(quote))
    }

    @Test
    fun `quote ending after episode end cannot play`() = runTest {
        val quote = createQuote(startMs = 120_000, endMs = 130_000)

        assertFalse(playableState(quote))
    }

    @Test
    fun `quote ending at episode end cannot play`() = runTest {
        val quote = createQuote(startMs = 120_000, endMs = 123_000)

        assertFalse(playableState(quote))
    }

    @Test
    fun `quote with unknown episode duration can play`() = runTest {
        val quote = createQuote(startMs = 49_020_000, endMs = 49_500_000)

        assertTrue(playableState(quote, episodeDurationMs = 0))
    }

    @Test
    fun `play quote outside episode does not touch playback`() = runTest {
        val quote = createQuote(startMs = 49_020_000, endMs = 49_500_000)
        playableState(quote)
        eventSink.skipEvent()

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        verifyNoSeek()
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `play quote outside stored episode duration does not seek`() = runTest {
        val quote = createQuote(startMs = 49_020_000, endMs = 49_500_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(
            PodcastEpisode(uuid = EPISODE_UUID, publishedDate = Date(), duration = 123.0),
        )
        playableState(quote, episodeDurationMs = 0)

        viewModel.playQuote(quote.uuid)
        viewModel.uiState.first { state -> state.messages.filterIsInstance<ChatMessage.Quote>().none { it.isPlaying } }

        verifyNoSeek()
    }

    @Test
    fun `playback shows the saved position when another episode is playing`() = runTest {
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid", positionMs = 10_000)

        setEpisodeInfo()
        advanceUntilIdle()

        assertEquals(ChatPlayback(isPlaying = false, positionMs = 50_000, durationMs = 200_000), viewModel.uiState.value.playback)
    }

    @Test
    fun `playback follows the player when the chat episode is current`() = runTest {
        setEpisodeInfo()
        playbackState.value = PlaybackState(
            state = PlaybackState.State.PLAYING,
            episodeUuid = EPISODE_UUID,
            positionMs = 80_000,
            durationMs = 190_000,
        )
        advanceUntilIdle()

        assertEquals(ChatPlayback(isPlaying = true, positionMs = 80_000, durationMs = 190_000), viewModel.uiState.value.playback)
    }

    @Test
    fun `play pause pauses when the chat episode is playing`() = runTest {
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
        setEpisodeInfo(sourceView = SourceView.PLAYER)
        advanceUntilIdle()

        viewModel.onPlayPauseClick()
        advanceUntilIdle()

        verify(playbackManager).pause(transientLoss = false, sourceView = SourceView.PLAYER)
        verify(playbackManager, never()).playNowSuspend(any<String>(), any(), any(), any())
    }

    @Test
    fun `play pause plays the chat episode when it is not playing`() = runTest {
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid")
        setEpisodeInfo(sourceView = SourceView.PLAYER)
        advanceUntilIdle()

        viewModel.onPlayPauseClick()
        advanceUntilIdle()

        verify(playbackManager).playNowSuspend(EPISODE_UUID, false, false, SourceView.PLAYER)
        verify(playbackManager, never()).pause(any(), any())
    }

    @Test
    fun `playback progress is clamped and safe without a duration`() {
        assertEquals(0.25f, ChatPlayback(positionMs = 50, durationMs = 200).progress)
        assertEquals(1f, ChatPlayback(positionMs = 300, durationMs = 200).progress)
        assertEquals(0f, ChatPlayback(positionMs = 300, durationMs = 0).progress)
    }

    private suspend fun verifyNoSeek() {
        verify(playbackManager, never()).playNowSuspend(any<BaseEpisode>(), any(), any(), any())
        verify(playbackManager, never()).seekToTimeMsSuspend(any(), anyOrNull())
    }

    private fun TestScope.playableState(
        quote: ChatMessage.Quote,
        episodeDurationMs: Int = 123_000,
    ): Boolean {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT_PLAYABLE_QUOTES, true)
        chatManager.messages.value = listOf(quote)
        setEpisodeInfo(episodeDurationMs)
        advanceUntilIdle()
        return viewModel.uiState.value.messages.filterIsInstance<ChatMessage.Quote>().single().canPlay
    }

    private fun createQuote(startMs: Int, endMs: Int) = ChatMessage.Quote(
        text = "Quote",
        start = "start",
        end = "end",
        startMs = startMs,
        endMs = endMs,
        uuid = "quote-uuid",
    )

    private fun setEpisodeInfo(
        episodeDurationMs: Int = 123_000,
        sourceView: SourceView = SourceView.EPISODE_DETAILS,
        isBeta: Boolean = false,
    ) {
        viewModel.setEpisodeInfo(
            episodeUuid = EPISODE_UUID,
            episodeTitle = "Episode title",
            episodeSubtitle = "Episode subtitle",
            podcastUuid = PODCAST_UUID,
            podcastTitle = "Podcast title",
            episodeDurationMs = episodeDurationMs,
            sourceView = sourceView,
            isBeta = isBeta,
        )
    }

    @Test
    fun `beta user sees the beta sheet and it is tracked`() = runTest {
        setEpisodeInfo(isBeta = true)
        eventSink.skipEvent()

        assertTrue(viewModel.uiState.value.isBetaSheetVisible)
        assertEquals(
            EpisodeChatBetaSheetShownEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `setting episode info again while the beta sheet is open does not track it twice`() = runTest {
        setEpisodeInfo(isBeta = true)
        eventSink.skipEvent(2)

        setEpisodeInfo(isBeta = true)
        eventSink.skipEvent()

        assertTrue(viewModel.uiState.value.isBetaSheetVisible)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `beta user who saw the sheet does not see it again`() = runTest {
        betaSheetSeen.value = true

        setEpisodeInfo(isBeta = true)

        assertFalse(viewModel.uiState.value.isBetaSheetVisible)
    }

    @Test
    fun `plus user does not see the beta sheet`() = runTest {
        setEpisodeInfo(isBeta = false)

        assertFalse(viewModel.uiState.value.isBetaSheetVisible)
        assertFalse(viewModel.uiState.value.isBeta)
    }

    @Test
    fun `dismissing the beta sheet saves it as seen and tracks it`() = runTest {
        setEpisodeInfo(isBeta = true)
        eventSink.skipEvent(2)

        viewModel.dismissBetaSheet()
        viewModel.dismissBetaSheet()

        assertFalse(viewModel.uiState.value.isBetaSheetVisible)
        verify(betaSheetSeenSetting, times(1)).set(true, updateModifiedAt = false)
        assertEquals(
            EpisodeChatBetaSheetDismissedEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
        assertTrue(eventSink.isEmpty())
    }

    private class TestNetworkConnectionWatcher : NetworkConnectionWatcher {
        override val networkCapabilities: StateFlow<android.net.NetworkCapabilities?> = MutableStateFlow(null)
    }

    private class TestChatManager : ChatManager {
        val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        val createdChats = mutableListOf<CreateChat>()
        val sentMessages = mutableListOf<SendMessage>()
        val sentHistories = mutableListOf<List<String>>()
        val clearedEpisodeUuids = mutableListOf<String>()
        var sendMessageException: Exception? = null

        override fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> = messages

        override suspend fun createChat(episodeUuid: String, podcastUuid: String) {
            createdChats += CreateChat(episodeUuid, podcastUuid)
        }

        override suspend fun sendMessage(
            episodeUuid: String,
            message: ChatMessage.User,
            allMessages: List<ChatMessage>,
        ) {
            sendMessageException?.let { throw it }
            sentMessages += SendMessage(episodeUuid, message.text)
            sentHistories += allMessages.map { it.text() }
            messages.value += listOf(message, ChatMessage.Assistant(text = "Response", uuid = "response-uuid-${sentMessages.size}"))
        }

        override suspend fun clearMessages(episodeUuid: String) {
            clearedEpisodeUuids += episodeUuid
            messages.value = emptyList()
        }

        private fun ChatMessage.text() = when (this) {
            is ChatMessage.User -> text
            is ChatMessage.Assistant -> text
            is ChatMessage.Quote -> text
        }
    }

    private data class CreateChat(val episodeUuid: String, val podcastUuid: String)
    private data class SendMessage(val episodeUuid: String, val message: String)

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val PODCAST_UUID = "podcast-uuid"
    }
}
