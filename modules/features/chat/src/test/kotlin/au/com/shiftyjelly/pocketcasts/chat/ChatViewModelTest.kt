package au.com.shiftyjelly.pocketcasts.chat

import android.net.NetworkCapabilities
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
import com.automattic.eventhorizon.EpisodeChatInputType
import com.automattic.eventhorizon.EpisodeChatMessageFailedEvent
import com.automattic.eventhorizon.EpisodeChatMessageSentEvent
import com.automattic.eventhorizon.EpisodeChatQuotePlayTappedEvent
import com.automattic.eventhorizon.EpisodeChatQuoteSourceType
import com.automattic.eventhorizon.EpisodeChatQuoteStopTappedEvent
import com.automattic.eventhorizon.EpisodeChatResponseRatedEvent
import com.automattic.eventhorizon.EpisodeChatResponseReceivedEvent
import com.automattic.eventhorizon.EpisodeChatSentimentType
import com.automattic.eventhorizon.EpisodeChatShownEvent
import com.automattic.eventhorizon.EventHorizon
import java.io.IOException
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private val surveySeen = MutableStateFlow(false)
    private val surveySeenSetting = mock<UserSetting<Boolean>> {
        on { value } doAnswer { surveySeen.value }
        on { set(any(), any(), any(), any()) } doAnswer { surveySeen.value = it.getArgument(0) }
    }
    private val settings = mock<Settings> {
        on { episodeChatBetaSheetSeen } doReturn betaSheetSeenSetting
        on { episodeChatSurveySeen } doReturn surveySeenSetting
    }
    private val networkConnectionWatcher = TestNetworkConnectionWatcher()
    private val timeSource = TestTimeSource()

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
        ioDispatcher = coroutineRule.testDispatcher,
        timeSource = timeSource,
    )

    @Test
    fun `set episode info creates chat without messages`() = runTest {
        viewModel.setEpisodeInfo(
            episodeUuid = EPISODE_UUID,
            episodeTitle = "Episode title",
            podcastUuid = PODCAST_UUID,
            podcastTitle = "Podcast title",
            episodeDurationMs = 123_000,
            sourceView = SourceView.EPISODE_DETAILS,
            isBeta = false,
        )

        viewModel.uiState.test {
            val state = awaitItem()

            assertEquals("Episode title", state.episodeTitle)
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
    fun `summarize click sends the summary prompt and keeps the typed input`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        viewModel.onInputTextChange("Draft")

        viewModel.onSummarizeClick("Summarize this episode")
        advanceUntilIdle()

        assertEquals(SendMessage(EPISODE_UUID, "Summarize this episode"), chatManager.sentMessages.single())
        assertEquals("Draft", viewModel.uiState.value.inputText)
        eventSink.skipEvent()
        assertEquals(
            EpisodeChatMessageSentEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                messageLength = "Summarize this episode".length.toLong(),
                inputType = EpisodeChatInputType.SummaryPrompt,
                messageIndex = 1,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `summarize click is ignored while awaiting a reply`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        val gate = CompletableDeferred<Unit>()
        chatManager.sendMessageGate = gate
        viewModel.onInputTextChange("First question")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onSummarizeClick("Summarize this episode")
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(SendMessage(EPISODE_UUID, "First question")), chatManager.sentMessages)
    }

    @Test
    fun `summarize click is ignored before the messages load`() = runTest {
        chatManager.observedMessages = emptyFlow()
        setEpisodeInfo()
        connectToInternet()

        viewModel.onSummarizeClick("Summarize this episode")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.areMessagesLoaded)
        assertTrue(chatManager.sentMessages.isEmpty())
    }

    @Test
    fun `summarize click is ignored while offline`() = runTest {
        setEpisodeInfo()
        networkConnectionWatcher.networkCapabilities.value = null
        advanceUntilIdle()

        viewModel.onSummarizeClick("Summarize this episode")
        advanceUntilIdle()

        assertTrue(chatManager.sentMessages.isEmpty())
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
                inputType = EpisodeChatInputType.Typed,
                messageIndex = 1,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `send tracks how long the response took`() = runTest {
        setEpisodeInfo()
        chatManager.sendMessageGate = CompletableDeferred()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        advanceUntilIdle()

        timeSource += 2_500.milliseconds
        chatManager.sendMessageGate?.complete(Unit)
        advanceUntilIdle()

        eventSink.skipEvent(2)
        assertEquals(
            EpisodeChatResponseReceivedEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                durationMs = 2_500,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `failed send does not track a response`() = runTest {
        setEpisodeInfo()
        eventSink.skipEvent()
        chatManager.sendMessageException = IOException()

        sendQuestion("Question")

        assertTrue(eventSink.pollEvent() is EpisodeChatMessageFailedEvent)
        assertTrue(eventSink.isEmpty())
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
    fun `retry keeps the summarize input type`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        chatManager.sendMessageException = IOException()
        viewModel.onSummarizeClick("Summarize this episode")
        advanceUntilIdle()
        chatManager.sendMessageException = null
        eventSink.skipEvent(eventSink.size)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatInputType.SummaryPrompt,
            (eventSink.pollEvent() as EpisodeChatMessageSentEvent).inputType,
        )
    }

    @Test
    fun `rating an answer stores the rating`() = runTest {
        setEpisodeInfo()

        val result = viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Positive)

        assertEquals(ChatAnswerRating.Positive, result)
        assertEquals(mapOf("answer-uuid" to ChatAnswerRating.Positive), viewModel.uiState.value.answerRatings)
    }

    @Test
    fun `rating an answer again with the same rating clears it`() = runTest {
        setEpisodeInfo()
        viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Negative)

        val result = viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Negative)

        assertNull(result)
        assertEquals(emptyMap<String, ChatAnswerRating>(), viewModel.uiState.value.answerRatings)
    }

    @Test
    fun `rating an answer with the other rating switches it`() = runTest {
        setEpisodeInfo()
        viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Positive)

        val result = viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Negative)

        assertEquals(ChatAnswerRating.Negative, result)
        assertEquals(mapOf("answer-uuid" to ChatAnswerRating.Negative), viewModel.uiState.value.answerRatings)
    }

    @Test
    fun `ratings are kept per answer`() = runTest {
        setEpisodeInfo()

        viewModel.rateAnswer("first-answer", ChatAnswerRating.Positive)
        viewModel.rateAnswer("second-answer", ChatAnswerRating.Negative)

        assertEquals(
            mapOf("first-answer" to ChatAnswerRating.Positive, "second-answer" to ChatAnswerRating.Negative),
            viewModel.uiState.value.answerRatings,
        )
    }

    @Test
    fun `a thumbs up thanks the user`() = runTest {
        setEpisodeInfo()

        viewModel.feedbackThanks.test {
            viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Positive)

            awaitItem()
        }
    }

    @Test
    fun `a thumbs down, a switch to thumbs down or a cleared rating does not thank the user`() = runTest {
        setEpisodeInfo()
        viewModel.rateAnswer("first-answer", ChatAnswerRating.Positive)

        viewModel.feedbackThanks.test {
            awaitItem()
            viewModel.rateAnswer("first-answer", ChatAnswerRating.Positive)
            viewModel.rateAnswer("second-answer", ChatAnswerRating.Negative)
            viewModel.rateAnswer("third-answer", ChatAnswerRating.Positive)
            awaitItem()
            viewModel.rateAnswer("third-answer", ChatAnswerRating.Negative)

            expectNoEvents()
        }
    }

    @Test
    fun `rating an answer tracks the rating with the number of the question it answers`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User("Q1", uuid = "q1"),
            ChatMessage.Assistant("A1", uuid = "a1"),
            ChatMessage.User("Q2", uuid = "q2"),
            ChatMessage.Assistant("A2", uuid = "a2"),
        )
        setEpisodeInfo()
        advanceUntilIdle()
        eventSink.skipEvent()

        viewModel.rateAnswer("a2", ChatAnswerRating.Negative)

        assertEquals(
            EpisodeChatResponseRatedEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                rating = EpisodeChatSentimentType.Negative,
                messageIndex = 2,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `clearing a rating is not tracked`() = runTest {
        setEpisodeInfo()
        eventSink.skipEvent()
        viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Positive)
        eventSink.skipEvent()

        viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Positive)

        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `clear chat removes the ratings`() = runTest {
        setEpisodeInfo()
        viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Positive)

        viewModel.clearChat()

        assertEquals(emptyMap<String, ChatAnswerRating>(), viewModel.uiState.value.answerRatings)
    }

    @Test
    fun `an answer ends before the next question and spans its quotes`() {
        val state = ChatUiState(
            messages = listOf(
                ChatMessage.User("Q1", uuid = "q1"),
                ChatMessage.Assistant("A1", uuid = "a1"),
                ChatMessage.Quote("Quote", start = "", end = "", uuid = "a1-quote"),
                ChatMessage.User("Q2", uuid = "q2"),
                ChatMessage.Assistant("A2", uuid = "a2"),
            ),
        )

        assertEquals(mapOf(2 to "a1", 4 to "a2"), state.answerUuidsByLastIndex)
    }

    @Test
    fun `a question without an answer has nothing to rate`() {
        val state = ChatUiState(
            messages = listOf(
                ChatMessage.User("Q1", uuid = "q1"),
                ChatMessage.Assistant("A1", uuid = "a1"),
                ChatMessage.User("Q2", uuid = "q2"),
            ),
        )

        assertEquals(mapOf(1 to "a1"), state.answerUuidsByLastIndex)
    }

    @Test
    fun `message index counts the questions in the conversation`() = runTest {
        setEpisodeInfo()

        sendQuestion("First")
        sendQuestion("Second")

        assertEquals(listOf(1L, 2L), eventSink.messageIndexes())
    }

    @Test
    fun `message index continues from the stored conversation`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User("Earlier"), ChatMessage.Assistant("Answer"))
        setEpisodeInfo()
        advanceUntilIdle()

        sendQuestion("Next")

        assertEquals(listOf(2L), eventSink.messageIndexes())
    }

    @Test
    fun `message index restarts after clearing the chat`() = runTest {
        setEpisodeInfo()
        sendQuestion("First")

        viewModel.clearChat()
        advanceUntilIdle()
        sendQuestion("Again")

        assertEquals(listOf(1L, 1L), eventSink.messageIndexes())
    }

    @Test
    fun `a new question after a failed one does not count the failed one`() = runTest {
        setEpisodeInfo()
        sendQuestion("First")
        chatManager.sendMessageException = IOException()
        sendQuestion("Failed")
        chatManager.sendMessageException = null

        sendQuestion("Second")

        assertEquals(listOf(1L, 2L), eventSink.messageIndexes())
        assertEquals(listOf("First", "Response"), chatManager.sentHistories.last())
    }

    @Test
    fun `retry keeps the message index`() = runTest {
        setEpisodeInfo()
        sendQuestion("First")
        chatManager.sendMessageException = IOException()
        sendQuestion("Second")
        chatManager.sendMessageException = null

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), eventSink.messageIndexes())
    }

    @Test
    fun `survey is not offered when no answer was received`() = runTest {
        setEpisodeInfo()

        assertFalse(viewModel.consumeSurveyEligibility())
    }

    @Test
    fun `survey is not offered when sending failed`() = runTest {
        setEpisodeInfo()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        advanceUntilIdle()

        assertFalse(viewModel.consumeSurveyEligibility())
    }

    @Test
    fun `survey is offered once after an answer`() = runTest {
        setEpisodeInfo()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        advanceUntilIdle()

        assertTrue(viewModel.consumeSurveyEligibility())
        assertTrue(surveySeen.value)
        assertFalse(viewModel.consumeSurveyEligibility())
    }

    @Test
    fun `survey is not offered after feedback was submitted in the chat`() = runTest {
        setEpisodeInfo()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onFeedbackSubmitted()

        assertFalse(viewModel.consumeSurveyEligibility())
        assertFalse(surveySeen.value)
    }

    @Test
    fun `survey is not offered when it was already seen`() = runTest {
        surveySeen.value = true
        setEpisodeInfo()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        advanceUntilIdle()

        assertFalse(viewModel.consumeSurveyEligibility())
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
    fun `playing and stopping a quote tracks the inline timestamp as the source`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
        playableState(quote)
        eventSink.skipEvent()

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        assertEquals(
            EpisodeChatQuotePlayTappedEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                quoteSource = EpisodeChatQuoteSourceType.InlineTimestamp,
            ),
            eventSink.pollEvent(),
        )
        assertEquals(
            EpisodeChatQuoteStopTappedEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                quoteSource = EpisodeChatQuoteSourceType.InlineTimestamp,
            ),
            eventSink.pollEvent(),
        )
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
    fun `play pause resumes the paused chat episode without reloading it`() = runTest {
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID)
        setEpisodeInfo(sourceView = SourceView.PLAYER)
        advanceUntilIdle()

        viewModel.onPlayPauseClick()
        advanceUntilIdle()

        verify(playbackManager).playQueueSuspend(SourceView.PLAYER, false)
        verify(playbackManager, never()).playNowSuspend(any<String>(), any(), any(), any())
    }

    @Test
    fun `play pause during a quote ends it without restoring the previous position`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        viewModel.onPlayPauseClick()
        advanceUntilIdle()
        playbackState.value = playbackState.value.copy(positionMs = 3_000)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.filterIsInstance<ChatMessage.Quote>().none { it.isPlaying })
        verify(playbackManager).pause(transientLoss = false, sourceView = SourceView.EPISODE_DETAILS)
        verify(playbackManager, never()).seekToTimeMsSuspend(org.mockito.kotlin.eq(10_000), anyOrNull())
    }

    @Test
    fun `playback progress is clamped and safe without a duration`() {
        assertEquals(0.25f, ChatPlayback(positionMs = 50, durationMs = 200).progress)
        assertEquals(1f, ChatPlayback(positionMs = 300, durationMs = 200).progress)
        assertEquals(0f, ChatPlayback(positionMs = 300, durationMs = 0).progress)
    }

    private fun TestScope.connectToInternet() {
        networkConnectionWatcher.networkCapabilities.value = mock<NetworkCapabilities> {
            on { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } doReturn true
        }
        advanceUntilIdle()
    }

    private fun TestScope.sendQuestion(text: String) {
        viewModel.onInputTextChange(text)
        viewModel.onSend()
        advanceUntilIdle()
    }

    private fun TestEventSink.messageIndexes(): List<Long?> {
        return List(size) { pollEvent() }.filterIsInstance<EpisodeChatMessageSentEvent>().map { it.messageIndex }
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
        override val networkCapabilities = MutableStateFlow<NetworkCapabilities?>(null)
    }

    private class TestChatManager : ChatManager {
        val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        var observedMessages: Flow<List<ChatMessage>>? = null
        val createdChats = mutableListOf<CreateChat>()
        val sentMessages = mutableListOf<SendMessage>()
        val sentHistories = mutableListOf<List<String>>()
        val clearedEpisodeUuids = mutableListOf<String>()
        var sendMessageException: Exception? = null
        var sendMessageGate: CompletableDeferred<Unit>? = null

        override fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> = observedMessages ?: messages

        override suspend fun createChat(episodeUuid: String, podcastUuid: String) {
            createdChats += CreateChat(episodeUuid, podcastUuid)
        }

        override suspend fun sendMessage(
            episodeUuid: String,
            message: ChatMessage.User,
            allMessages: List<ChatMessage>,
        ) {
            sendMessageGate?.await()
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
