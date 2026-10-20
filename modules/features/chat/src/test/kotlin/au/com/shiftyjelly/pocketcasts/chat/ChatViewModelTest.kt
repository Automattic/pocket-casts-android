package au.com.shiftyjelly.pocketcasts.chat

import android.net.NetworkCapabilities
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.ReadSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatAnswerRating
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatInputType
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatQuoteSeeker
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatSendFailure
import au.com.shiftyjelly.pocketcasts.repositories.chat.ClearedChat
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EpisodeChatBetaSheetDismissedEvent
import com.automattic.eventhorizon.EpisodeChatBetaSheetShownEvent
import com.automattic.eventhorizon.EpisodeChatBetaSheetSignInTappedEvent
import com.automattic.eventhorizon.EpisodeChatClearUndoneEvent
import com.automattic.eventhorizon.EpisodeChatClearedEvent
import com.automattic.eventhorizon.EpisodeChatDeleteUndoneEvent
import com.automattic.eventhorizon.EpisodeChatDeletedEvent
import com.automattic.eventhorizon.EpisodeChatDismissedEvent
import com.automattic.eventhorizon.EpisodeChatEntryPointType
import com.automattic.eventhorizon.EpisodeChatErrorType
import com.automattic.eventhorizon.EpisodeChatHistoryRowTappedEvent
import com.automattic.eventhorizon.EpisodeChatHistorySectionType
import com.automattic.eventhorizon.EpisodeChatHistoryShownEvent
import com.automattic.eventhorizon.EpisodeChatInputType
import com.automattic.eventhorizon.EpisodeChatMessageFailedEvent
import com.automattic.eventhorizon.EpisodeChatMessageSentEvent
import com.automattic.eventhorizon.EpisodeChatQuotePlayTappedEvent
import com.automattic.eventhorizon.EpisodeChatQuotePlayUndoneEvent
import com.automattic.eventhorizon.EpisodeChatQuoteSourceType
import com.automattic.eventhorizon.EpisodeChatResponseRatedEvent
import com.automattic.eventhorizon.EpisodeChatResponseRatingClearedEvent
import com.automattic.eventhorizon.EpisodeChatResponseReceivedEvent
import com.automattic.eventhorizon.EpisodeChatSentimentType
import com.automattic.eventhorizon.EpisodeChatShownEvent
import com.automattic.eventhorizon.EventHorizon
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule(order = 0)
    val coroutineRule = MainCoroutineRule()

    @get:Rule(order = 1)
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val chatManager = TestChatManager()
    private val playbackState = MutableStateFlow(PlaybackState())
    private val userSeeks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val upNextQueue = mock<UpNextQueue> {
        on { queueEpisodes } doReturn emptyList()
    }
    private val playbackManager = mock<PlaybackManager> {
        on { playbackStateFlow } doReturn playbackState
        on { userSeeks } doReturn userSeeks
        on { upNextQueue } doReturn upNextQueue
    }
    private val episode = MutableStateFlow<BaseEpisode>(
        PodcastEpisode(uuid = EPISODE_UUID, publishedDate = Date(), duration = 200.0, playedUpTo = 50.0),
    )
    private val episodeManager = mock<EpisodeManager> {
        on { findEpisodeByUuidFlow(EPISODE_UUID) } doReturn episode
        on { findEpisodeByUuidFlow(OTHER_EPISODE_UUID) } doReturn MutableStateFlow(
            PodcastEpisode(uuid = OTHER_EPISODE_UUID, publishedDate = Date(), duration = 300.0),
        )
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
    private val subscription = MutableStateFlow<Subscription?>(null)
    private val cachedSubscriptionSetting = mock<ReadSetting<Subscription?>> {
        on { value } doAnswer { subscription.value }
        on { flow } doReturn subscription
    }
    private val artworkConfigurationSetting = mock<UserSetting<ArtworkConfiguration>> {
        on { value } doReturn ArtworkConfiguration(useEpisodeArtwork = true)
    }
    private val settings = mock<Settings> {
        on { artworkConfiguration } doReturn artworkConfigurationSetting
        on { episodeChatBetaSheetSeen } doReturn betaSheetSeenSetting
        on { episodeChatSurveySeen } doReturn surveySeenSetting
        on { cachedSubscription } doReturn cachedSubscriptionSetting
    }
    private val networkConnectionWatcher = TestNetworkConnectionWatcher()
    private var isLoggedIn = true
    private val syncManager = mock<SyncManager> {
        on { isLoggedIn() } doAnswer { isLoggedIn }
    }
    private val timeSource = TestTimeSource()
    private val clock = MutableClock(Instant.parse("2026-10-14T12:00:00Z"))

    private lateinit var eventSink: TestEventSink
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        eventSink = TestEventSink()
        viewModel = createViewModel()
    }

    private val quoteSeeker = mock<ChatQuoteSeeker> {
        on { quoteTime(any(), any()) } doSuspendableAnswer {
            val quote = it.arguments[1] as ChatMessage.Quote
            ChatQuoteSeeker.QuoteTime(timeMs = quote.startMs, referenceTimeMs = null)
        }
        on { playbackTimeMs(any(), any()) } doSuspendableAnswer { (it.arguments[1] as ChatQuoteSeeker.QuoteTime).timeMs }
    }

    private fun createViewModel() = ChatViewModel(
        networkConnectionWatcher = networkConnectionWatcher,
        chatManager = chatManager,
        playbackManager = playbackManager,
        episodeManager = episodeManager,
        quoteSeeker = quoteSeeker,
        eventHorizon = EventHorizon(eventSink),
        settings = settings,
        applicationScope = kotlinx.coroutines.CoroutineScope(coroutineRule.testDispatcher),
        ioDispatcher = coroutineRule.testDispatcher,
        timeSource = timeSource,
        clock = clock,
        syncManager = syncManager,
    )

    @Test
    fun `set episode info creates chat without messages`() = runTest {
        viewModel.setEpisodeInfo(
            episodeUuid = EPISODE_UUID,
            episodeTitle = "Episode title",
            podcastUuid = PODCAST_UUID,
            podcastTitle = "Podcast title",
            episodeDurationMs = 123_000,
            sourceView = SourceView.PLAYER,
            entryPoint = ChatEntryPoint.PlayerShelf,
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
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                hasHistory = false,
                entryPoint = ChatEntryPoint.PlayerShelf.analyticsValue,
                isBeta = false,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `opening the chat through the free beta tracks it as beta`() = runTest {
        setEpisodeInfo(isBeta = true)
        advanceUntilIdle()

        val shown = generateSequence { if (eventSink.isEmpty()) null else eventSink.pollEvent() }
            .filterIsInstance<EpisodeChatShownEvent>()
            .single()
        assertEquals(true, shown.isBeta)
    }

    @Test
    fun `reopening a chat with messages tracks it as continued`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User("Earlier"), ChatMessage.Assistant("Answer"))

        setEpisodeInfo(entryPoint = ChatEntryPoint.MoreActions)

        assertEquals(
            EpisodeChatShownEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                hasHistory = true,
                entryPoint = ChatEntryPoint.MoreActions.analyticsValue,
                isBeta = false,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `closing the chat before its messages load still tracks it as shown`() = runTest {
        chatManager.observedMessages = MutableSharedFlow()
        setEpisodeInfo()

        viewModel.trackDismissed()

        assertEquals(
            EpisodeChatShownEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                hasHistory = null,
                entryPoint = ChatEntryPoint.PlayerShelf.analyticsValue,
                isBeta = false,
            ),
            eventSink.pollEvent(),
        )
        assertTrue(eventSink.pollEvent() is EpisodeChatDismissedEvent)
    }

    @Test
    fun `chat shown is tracked once per opened chat`() = runTest {
        setEpisodeInfo()
        chatManager.messages.value = listOf(ChatMessage.User("Question"))

        assertEquals(1, List(eventSink.size) { eventSink.pollEvent() }.count { it is EpisodeChatShownEvent })
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
            sourceView = SourceView.PLAYER,
            entryPoint = ChatEntryPoint.PlayerShelf,
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
            assertTrue(state.canSend)
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
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                messageLength = "Summarize this episode".length.toLong(),
                inputType = EpisodeChatInputType.SummaryPrompt,
                messageIndex = 1,
                isRetry = false,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `summarize click sends the summary prompt after an answer`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        viewModel.onInputTextChange("First question")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onSummarizeClick("Summarize this episode")
        advanceUntilIdle()

        assertEquals(
            listOf(
                SendMessage(EPISODE_UUID, "First question"),
                SendMessage(EPISODE_UUID, "Summarize this episode"),
            ),
            chatManager.sentMessages,
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
    fun `send is ignored while awaiting a reply and keeps the typed text`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        val gate = CompletableDeferred<Unit>()
        chatManager.sendMessageGate = gate
        viewModel.onInputTextChange("First question")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onInputTextChange("Second question")
        viewModel.onSend()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(SendMessage(EPISODE_UUID, "First question")), chatManager.sentMessages)
        assertEquals("Second question", viewModel.uiState.value.inputText)
    }

    @Test
    fun `send is ignored while offline`() = runTest {
        setEpisodeInfo()
        networkConnectionWatcher.networkCapabilities.value = null
        advanceUntilIdle()

        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        advanceUntilIdle()

        assertTrue(chatManager.sentMessages.isEmpty())
        assertEquals("Question", viewModel.uiState.value.inputText)
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
    fun `send tracks message sent before the answer arrives`() = runTest {
        setEpisodeInfo()
        eventSink.skipEvent()
        chatManager.sendMessageGate = CompletableDeferred()
        viewModel.onInputTextChange("Question")

        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatMessageSentEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                messageLength = "Question".length.toLong(),
                inputType = EpisodeChatInputType.Typed,
                messageIndex = 1,
                isRetry = false,
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
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                durationMs = 2_500,
                messageIndex = 1,
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

        assertTrue(eventSink.pollEvent() is EpisodeChatMessageSentEvent)
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

        eventSink.skipEvent()
        assertEquals(
            EpisodeChatMessageFailedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                error = EpisodeChatErrorType.Network,
                inputType = EpisodeChatInputType.Typed,
                messageIndex = 1,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `retry resends failed transient user message`() = runTest {
        setEpisodeInfo()
        connectToInternet()
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
    fun `retry does nothing while offline`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        advanceUntilIdle()
        chatManager.sendMessageException = null
        networkConnectionWatcher.networkCapabilities.value = null
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        assertTrue(chatManager.sentMessages.isEmpty())
        assertEquals(ChatError.NetworkError, viewModel.uiState.value.error)
    }

    @Test
    fun `sending a new question drops the earlier unsent one`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("First")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onInputTextChange("Second")
        viewModel.onSend()

        val texts = viewModel.uiState.value.messages.filterIsInstance<ChatMessage.User>().map { it.text }
        assertEquals(listOf("Second"), texts)
    }

    @Test
    fun `retry tracks message sent as a retry`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        advanceUntilIdle()
        chatManager.sendMessageException = null
        eventSink.skipEvent(eventSink.size)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatMessageSentEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                messageLength = "Question".length.toLong(),
                inputType = EpisodeChatInputType.Typed,
                messageIndex = 1,
                isRetry = true,
            ),
            eventSink.pollEvent(),
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
    fun `retrying a reopened summarize question keeps the summarize input type`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User(
                text = "Summarize this episode",
                failure = ChatSendFailure.Network,
                inputType = ChatInputType.SummaryPrompt,
                uuid = "question-uuid",
            ),
        )
        setEpisodeInfo()
        connectToInternet()
        advanceUntilIdle()
        eventSink.skipEvent(eventSink.size)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatInputType.SummaryPrompt,
            (eventSink.pollEvent() as EpisodeChatMessageSentEvent).inputType,
        )
    }

    @Test
    fun `retrying a reopened typed question after a summarize send tracks it as typed`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        chatManager.sendMessageException = IOException()
        viewModel.onSummarizeClick("Summarize this episode")
        advanceUntilIdle()
        chatManager.sendMessageException = null
        chatManager.otherMessages.value = listOf(ChatMessage.User(text = "Typed", failure = ChatSendFailure.Network, uuid = "typed-uuid"))
        viewModel.openHistoryChat(historyItem(OTHER_EPISODE_UUID))
        advanceUntilIdle()
        eventSink.skipEvent(eventSink.size)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatInputType.Typed,
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
    fun `stored ratings are restored without tracking`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User(text = "Q1", uuid = "q1"),
            ChatMessage.Assistant(text = "A1", rating = ChatAnswerRating.Positive, uuid = "a1"),
            ChatMessage.User(text = "Q2", uuid = "q2"),
            ChatMessage.Assistant(text = "A2", uuid = "a2"),
        )
        setEpisodeInfo()
        advanceUntilIdle()

        assertEquals(mapOf("a1" to ChatAnswerRating.Positive), viewModel.uiState.value.answerRatings)
        assertTrue(List(eventSink.size) { eventSink.pollEvent() }.none { it is EpisodeChatResponseRatedEvent })
    }

    @Test
    fun `rating changes are saved in order`() = runTest {
        setEpisodeInfo()

        viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Positive)
        viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Negative)
        viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Negative)
        advanceUntilIdle()

        assertEquals(
            listOf(
                "answer-uuid" to ChatAnswerRating.Positive,
                "answer-uuid" to ChatAnswerRating.Negative,
                "answer-uuid" to null,
            ),
            chatManager.savedRatings,
        )
    }

    @Test
    fun `later message updates keep the ratings changed in the chat`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User(text = "Q1", uuid = "q1"),
            ChatMessage.Assistant(text = "A1", rating = ChatAnswerRating.Positive, uuid = "a1"),
        )
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.rateAnswer("a1", ChatAnswerRating.Negative)
        chatManager.messages.value += ChatMessage.User(text = "Q2", uuid = "q2")
        advanceUntilIdle()

        assertEquals(mapOf("a1" to ChatAnswerRating.Negative), viewModel.uiState.value.answerRatings)
    }

    @Test
    fun `a thumbs up thanks the user`() = runTest {
        setEpisodeInfo()

        viewModel.toasts.filter { it == ChatToast.FeedbackThanks }.test {
            viewModel.rateAnswer("answer-uuid", ChatAnswerRating.Positive)

            awaitItem()
        }
    }

    @Test
    fun `a thumbs down, a switch to thumbs down or a cleared rating does not thank the user`() = runTest {
        setEpisodeInfo()
        viewModel.rateAnswer("first-answer", ChatAnswerRating.Positive)

        viewModel.toasts.filter { it == ChatToast.FeedbackThanks }.test {
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
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                rating = EpisodeChatSentimentType.Negative,
                messageIndex = 2,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `clearing a rating tracks the removed rating`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User("Q1", uuid = "q1"),
            ChatMessage.Assistant("A1", uuid = "a1"),
        )
        setEpisodeInfo()
        eventSink.skipEvent()
        viewModel.rateAnswer("a1", ChatAnswerRating.Positive)
        eventSink.skipEvent()

        viewModel.rateAnswer("a1", ChatAnswerRating.Positive)

        assertEquals(
            EpisodeChatResponseRatingClearedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                previousRating = EpisodeChatSentimentType.Positive,
                messageIndex = 1,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `switching a rating tracks the replaced rating`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User("Q1", uuid = "q1"),
            ChatMessage.Assistant("A1", uuid = "a1"),
        )
        setEpisodeInfo()
        eventSink.skipEvent()
        viewModel.rateAnswer("a1", ChatAnswerRating.Positive)
        eventSink.skipEvent()

        viewModel.rateAnswer("a1", ChatAnswerRating.Negative)

        assertEquals(
            EpisodeChatResponseRatedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                rating = EpisodeChatSentimentType.Negative,
                messageIndex = 1,
                previousRating = EpisodeChatSentimentType.Positive,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `clear chat removes the ratings`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User(text = "Q1", uuid = "q1"))
        setEpisodeInfo()
        advanceUntilIdle()
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

        assertEquals(listOf(1L, 2L, 2L), eventSink.messageIndexes())
        assertEquals(listOf("First", "Response"), chatManager.sentHistories.last())
    }

    @Test
    fun `retry keeps the message index`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        sendQuestion("First")
        chatManager.sendMessageException = IOException()
        sendQuestion("Second")
        chatManager.sendMessageException = null

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 2L), eventSink.messageIndexes())
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
    fun `a question that did not reach us is shown as not sent when the chat reopens`() = runTest {
        setEpisodeInfo()
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Failed question")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel = createViewModel()
        setEpisodeInfo()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Failed question"), state.messages.filterIsInstance<ChatMessage.User>().map { it.text })
        assertEquals(ChatError.NetworkError, state.error)
    }

    @Test
    fun `a question we could not answer shows the server error when the chat reopens`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User(text = "Question", failure = ChatSendFailure.Server, uuid = "question-uuid"))

        setEpisodeInfo()
        advanceUntilIdle()

        assertEquals(ChatError.ServerError, viewModel.uiState.value.error)
    }

    @Test
    fun `a question left waiting when the chat closed is shown as not sent`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User(text = "Question", uuid = "question-uuid"))

        setEpisodeInfo()
        advanceUntilIdle()

        assertEquals(ChatError.NetworkError, viewModel.uiState.value.error)
    }

    @Test
    fun `an answered conversation reopens without an error`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User(text = "Question", uuid = "question-uuid"),
            ChatMessage.Assistant(text = "Answer", uuid = "answer-uuid"),
        )

        setEpisodeInfo()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `retrying a reopened question resends it without it in the history`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User(text = "First", uuid = "first-uuid"),
            ChatMessage.Assistant(text = "Answer", uuid = "answer-uuid"),
            ChatMessage.User(text = "Question", failure = ChatSendFailure.Network, uuid = "question-uuid"),
        )
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(SendMessage(EPISODE_UUID, "Question"), chatManager.sentMessages.single())
        assertEquals(listOf("First", "Answer"), chatManager.sentHistories.single())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `undoing clear chat restores a failed question as not sent`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User(text = "Question", failure = ChatSendFailure.Network, uuid = "question-uuid"))
        setEpisodeInfo()
        advanceUntilIdle()
        viewModel.clearChat()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)

        viewModel.undoClearChat()
        advanceUntilIdle()

        assertEquals(ChatError.NetworkError, viewModel.uiState.value.error)
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(SendMessage(EPISODE_UUID, "Question"), chatManager.sentMessages.single())
    }

    @Test
    fun `asking something new instead of retrying discards the stored failed question`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User(text = "Question", failure = ChatSendFailure.Network, uuid = "question-uuid"))
        setEpisodeInfo()
        advanceUntilIdle()

        sendQuestion("Another question")

        assertEquals(listOf("question-uuid"), chatManager.discardedMessageUuids)
        val messages = viewModel.uiState.value.messages
        assertEquals(listOf("Another question"), messages.filterIsInstance<ChatMessage.User>().map { it.text })
        assertEquals(listOf("Response"), messages.filterIsInstance<ChatMessage.Assistant>().map { it.text })
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
        chatManager.messages.value = listOf(ChatMessage.User(text = "Q1", uuid = "q1"))
        setEpisodeInfo()
        advanceUntilIdle()
        eventSink.skipEvent()

        viewModel.clearChat()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatClearedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `clear chat offers undo`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User(text = "Q1", uuid = "q1"))
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.toasts.test {
            viewModel.clearChat()

            assertEquals(ChatToast.ChatCleared, awaitItem())
        }
    }

    @Test
    fun `undoing clear chat restores the messages and ratings and tracks it`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User(text = "Q1", uuid = "q1"),
            ChatMessage.Assistant(text = "A1", rating = ChatAnswerRating.Positive, uuid = "a1"),
        )
        setEpisodeInfo()
        advanceUntilIdle()
        viewModel.clearChat()
        advanceUntilIdle()
        assertEquals(emptyMap<String, ChatAnswerRating>(), viewModel.uiState.value.answerRatings)
        eventSink.skipEvent(eventSink.size)

        viewModel.undoClearChat()
        advanceUntilIdle()

        assertEquals(listOf("q1", "a1"), viewModel.uiState.value.messages.map { it.uuid })
        assertEquals(setOf("q1", "a1"), viewModel.uiState.value.restoredMessageUuids)
        assertEquals(mapOf("a1" to ChatAnswerRating.Positive), viewModel.uiState.value.answerRatings)
        assertEquals(
            EpisodeChatClearUndoneEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `clearing an empty chat does nothing`() = runTest {
        setEpisodeInfo()
        advanceUntilIdle()
        eventSink.skipEvent(eventSink.size)

        viewModel.toasts.test {
            viewModel.clearChat()
            advanceUntilIdle()

            expectNoEvents()
        }
        assertTrue(chatManager.clearedEpisodeUuids.isEmpty())
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `asking after clearing the chat drops the undo`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User(text = "Q1", uuid = "q1"),
            ChatMessage.Assistant(text = "A1", uuid = "a1"),
        )
        setEpisodeInfo()
        advanceUntilIdle()
        viewModel.clearChat()
        advanceUntilIdle()
        sendQuestion("Q2")

        viewModel.undoClearChat()
        advanceUntilIdle()

        assertEquals(listOf("q1", "a1"), chatManager.clearedMessageUuids)
        assertEquals(1, viewModel.uiState.value.messages.count { it is ChatMessage.User })
    }

    @Test
    fun `a clear that finishes after a new question does not bring back the old chat`() = runTest {
        chatManager.messages.value = listOf(
            ChatMessage.User(text = "Q1", uuid = "q1"),
            ChatMessage.Assistant(text = "A1", uuid = "a1"),
        )
        setEpisodeInfo()
        connectToInternet()
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        chatManager.clearMessagesGate = gate
        viewModel.clearChat()
        sendQuestion("Q2")
        assertTrue(chatManager.sentMessages.isEmpty())

        gate.complete(Unit)
        advanceUntilIdle()
        viewModel.undoClearChat()
        advanceUntilIdle()

        assertEquals(listOf(SendMessage(EPISODE_UUID, "Q2")), chatManager.sentMessages)
        assertEquals(listOf("response-uuid-1"), viewModel.uiState.value.messages.filterIsInstance<ChatMessage.Assistant>().map { it.uuid })
        assertEquals(1, viewModel.uiState.value.messages.count { it is ChatMessage.User })
    }

    @Test
    fun `undoing a clear of only an unsent question brings it back as not sent`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        chatManager.sendMessageException = IOException()
        sendQuestion("Question")
        viewModel.clearChat()
        advanceUntilIdle()
        eventSink.skipEvent(eventSink.size)

        viewModel.undoClearChat()
        advanceUntilIdle()

        assertEquals(listOf("Question"), viewModel.uiState.value.messages.filterIsInstance<ChatMessage.User>().map { it.text })
        assertEquals(ChatError.NetworkError, viewModel.uiState.value.error)
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
    fun `quote starting before episode end can play even when it ends after it`() = runTest {
        val quote = createQuote(startMs = 120_000, endMs = 130_000)

        assertTrue(playableState(quote))
    }

    @Test
    fun `quote starting at episode end cannot play`() = runTest {
        val quote = createQuote(startMs = 123_000, endMs = 125_000)

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
    fun `playing a quote tracks the inline timestamp as the source`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
        playableState(quote)
        eventSink.skipEvent()

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        assertEquals(
            EpisodeChatQuotePlayTappedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                quoteSource = EpisodeChatQuoteSourceType.InlineTimestamp,
                isOtherEpisode = false,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `playing a quote seeks to its start and keeps playing`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        verify(playbackManager).seekToTimeMsSuspend(org.mockito.kotlin.eq(1_000), anyOrNull())
        verify(playbackManager).playQueueSuspend(any(), any())
        verify(playbackManager, never()).playNowSuspend(any<BaseEpisode>(), any(), any(), any())
        verify(playbackManager, never()).pause(any(), any())
        verify(playbackManager, never()).pauseSuspend(any(), any())
        verify(playbackManager, never()).seekToTimeMsSuspend(org.mockito.kotlin.eq(10_000), anyOrNull())
    }

    @Test
    fun `playing a quote seeks to the time the seeker resolves`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(quoteSeeker.playbackTimeMs(any(), any())).thenReturn(31_500)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        verify(playbackManager).seekToTimeMsSuspend(org.mockito.kotlin.eq(31_500), anyOrNull())
    }

    @Test
    fun `playing a quote drops the seek when the user seeks while it resolves`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val resolved = CompletableDeferred<Int>()
        whenever(quoteSeeker.playbackTimeMs(any(), any())).doSuspendableAnswer { resolved.await() }
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()
        userSeeks.emit(Unit)
        resolved.complete(31_500)
        advanceUntilIdle()

        verify(playbackManager, never()).seekToTimeMsSuspend(any(), anyOrNull())
        verify(playbackManager, never()).playQueueSuspend(any(), any())
    }

    @Test
    fun `playing a quote drops the seek when the user seeks while the episode loads`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        whenever(quoteSeeker.playbackTimeMs(any(), any())).thenReturn(31_500)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid")
        whenever(playbackManager.playNowSuspend(any<BaseEpisode>(), any(), any(), any())).doSuspendableAnswer {
            userSeeks.emit(Unit)
            playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
            Unit
        }
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        verify(playbackManager, never()).seekToTimeMsSuspend(any(), anyOrNull())
    }

    @Test
    fun `playing a quote drops the seek when the episode changes while it resolves`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val resolved = CompletableDeferred<Int>()
        whenever(quoteSeeker.playbackTimeMs(any(), any())).doSuspendableAnswer { resolved.await() }
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = OTHER_EPISODE_UUID, positionMs = 0)
        resolved.complete(31_500)
        advanceUntilIdle()

        verify(playbackManager, never()).seekToTimeMsSuspend(any(), anyOrNull())
    }

    @Test
    fun `playing a quote offers undo with the episode title`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        playableState(quote)

        viewModel.toasts.test {
            viewModel.playQuote(quote.uuid)
            advanceUntilIdle()

            assertEquals(episode.value.title, (awaitItem() as ChatToast.QuotePlaying).episodeTitle)
        }
    }

    @Test
    fun `undoing a quote goes back to where playback was and pauses if it was paused`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()
        eventSink.skipEvent(eventSink.size)

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        verify(playbackManager).seekToTimeMsSuspend(org.mockito.kotlin.eq(10_000), anyOrNull())
        verify(playbackManager).pause(any(), any())
        assertEquals(
            EpisodeChatQuotePlayUndoneEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                isOtherEpisode = false,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `undoing a quote keeps playing if it was playing`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        verify(playbackManager).seekToTimeMsSuspend(org.mockito.kotlin.eq(10_000), anyOrNull())
        verify(playbackManager, never()).pause(any(), any())
    }

    @Test
    fun `undoing after two quotes goes back to where playback was before the first`() = runTest {
        val first = createQuote(startMs = 1_000, endMs = 3_000)
        val second = createQuote(startMs = 5_000, endMs = 7_000, uuid = "second-quote-uuid")
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        chatManager.messages.value = listOf(first, second)
        setEpisodeInfo(123_000)
        advanceUntilIdle()
        viewModel.playQuote(first.uuid)
        advanceUntilIdle()
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 1_500)
        viewModel.playQuote(second.uuid)
        advanceUntilIdle()

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        verify(playbackManager).seekToTimeMsSuspend(org.mockito.kotlin.eq(10_000), anyOrNull())
        verify(playbackManager, never()).seekToTimeMsSuspend(org.mockito.kotlin.eq(1_500), anyOrNull())
    }

    @Test
    fun `an expired quote undo starts again from the next quote`() = runTest {
        val first = createQuote(startMs = 1_000, endMs = 3_000)
        val second = createQuote(startMs = 5_000, endMs = 7_000, uuid = "second-quote-uuid")
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        chatManager.messages.value = listOf(first, second)
        setEpisodeInfo(123_000)
        advanceUntilIdle()
        viewModel.playQuote(first.uuid)
        advanceUntilIdle()
        viewModel.onQuoteUndoExpired(quotePlayingToast().undoId)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 1_500)
        viewModel.playQuote(second.uuid)
        advanceUntilIdle()

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        verify(playbackManager).seekToTimeMsSuspend(org.mockito.kotlin.eq(1_500), anyOrNull())
    }

    @Test
    fun `a quote toast replaced by the next quote keeps the first undo`() = runTest {
        val first = createQuote(startMs = 1_000, endMs = 3_000)
        val second = createQuote(startMs = 5_000, endMs = 7_000, uuid = "second-quote-uuid")
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        chatManager.messages.value = listOf(first, second)
        setEpisodeInfo(123_000)
        advanceUntilIdle()
        viewModel.playQuote(first.uuid)
        advanceUntilIdle()
        val firstToast = quotePlayingToast()
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 1_500)
        viewModel.playQuote(second.uuid)
        advanceUntilIdle()
        viewModel.onQuoteUndoExpired(firstToast.undoId)

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        verify(playbackManager).seekToTimeMsSuspend(org.mockito.kotlin.eq(10_000), anyOrNull())
        verify(playbackManager, never()).seekToTimeMsSuspend(org.mockito.kotlin.eq(1_500), anyOrNull())
    }

    @Test
    fun `a quote that could not play drops the undo of the quote before it`() = runTest {
        val first = createQuote(startMs = 1_000, endMs = 3_000)
        val second = createQuote(startMs = 5_000, endMs = 7_000, uuid = "second-quote-uuid")
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        whenever(episodeManager.findEpisodeByUuid("other-uuid")).thenReturn(PodcastEpisode(uuid = "other-uuid", publishedDate = Date()))
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        chatManager.messages.value = listOf(first, second)
        setEpisodeInfo(123_000)
        advanceUntilIdle()
        viewModel.playQuote(first.uuid)
        advanceUntilIdle()
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid", positionMs = 1_500)
        viewModel.playQuote(second.uuid)
        advanceUntilIdle()
        eventSink.skipEvent(eventSink.size)
        assertEquals(ChatToast.QuotePlayFailed, viewModel.toasts.first())

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        verify(playbackManager, never()).seekToTimeMsSuspend(org.mockito.kotlin.eq(10_000), anyOrNull())
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `undoing a quote pauses a paused other episode even when it loads too slowly`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val other = PodcastEpisode(uuid = "other-uuid", publishedDate = Date())
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        whenever(episodeManager.findEpisodeByUuid("other-uuid")).thenReturn(other)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = "other-uuid", positionMs = 10_000)
        whenever(playbackManager.playNowSuspend(org.mockito.kotlin.eq(episode.value), any(), any(), any())).then {
            playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
            Unit
        }
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        verify(playbackManager).playNowSuspend(org.mockito.kotlin.eq(other), any(), any(), any())
        verify(playbackManager).pause(any(), any())
        verify(playbackManager, never()).seekToTimeMsSuspend(org.mockito.kotlin.eq(10_000), anyOrNull())
    }

    @Test
    fun `playing a quote while another episode plays switches to the chat episode first`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid")
        whenever(playbackManager.playNowSuspend(any<BaseEpisode>(), any(), any(), any())).then {
            playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
            Unit
        }
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        val order = org.mockito.kotlin.inOrder(playbackManager)
        order.verify(playbackManager).playNowSuspend(org.mockito.kotlin.eq(episode.value), any(), any(), any())
        order.verify(playbackManager).seekToTimeMsSuspend(org.mockito.kotlin.eq(1_000), anyOrNull())
    }

    @Test
    fun `playing a quote while another episode plays tracks the switch`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid")
        whenever(playbackManager.playNowSuspend(org.mockito.kotlin.eq(episode.value), any(), any(), any())).then {
            playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
            Unit
        }
        playableState(quote)
        eventSink.skipEvent()

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        assertEquals(
            EpisodeChatQuotePlayTappedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                quoteSource = EpisodeChatQuoteSourceType.InlineTimestamp,
                isOtherEpisode = true,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `a quote whose episode never loads shows that it could not play`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid")
        playableState(quote)

        viewModel.toasts.test {
            viewModel.playQuote(quote.uuid)
            advanceUntilIdle()

            assertEquals(ChatToast.QuotePlayFailed, awaitItem())
        }
    }

    @Test
    fun `a quote tap is tracked before its episode loads even when it never does`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid")
        playableState(quote)
        eventSink.skipEvent(eventSink.size)

        viewModel.playQuote(quote.uuid)

        assertEquals(
            EpisodeChatQuotePlayTappedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                quoteSource = EpisodeChatQuoteSourceType.InlineTimestamp,
                isOtherEpisode = true,
            ),
            eventSink.pollEvent(),
        )
        advanceUntilIdle()
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `a quote whose episode never loads goes back to what was playing`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val other = PodcastEpisode(uuid = "other-uuid", publishedDate = Date())
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        whenever(episodeManager.findEpisodeByUuid("other-uuid")).thenReturn(other)
        whenever(upNextQueue.currentEpisode).thenReturn(episode.value)
        whenever(playbackManager.playNowSuspend(org.mockito.kotlin.eq(other), any(), any(), any())).then {
            playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid")
            Unit
        }
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid", positionMs = 10_000)
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        verify(playbackManager).playNowSuspend(org.mockito.kotlin.eq(other), any(), any(), any())
        verify(playbackManager).seekToTimeMsSuspend(org.mockito.kotlin.eq(10_000), anyOrNull())
    }

    @Test
    fun `a quote whose episode never loads keeps an episode started while it was loading`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val other = PodcastEpisode(uuid = "other-uuid", publishedDate = Date())
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        whenever(episodeManager.findEpisodeByUuid("other-uuid")).thenReturn(other)
        whenever(upNextQueue.currentEpisode).thenReturn(PodcastEpisode(uuid = "started-uuid", publishedDate = Date()))
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid", positionMs = 10_000)
        playableState(quote)

        viewModel.toasts.test {
            viewModel.playQuote(quote.uuid)
            advanceUntilIdle()

            assertEquals(ChatToast.QuotePlayFailed, awaitItem())
        }
        verify(playbackManager, never()).playNowSuspend(org.mockito.kotlin.eq(other), any(), any(), any())
        verify(playbackManager, never()).seekToTimeMsSuspend(any(), anyOrNull())
        verify(upNextQueue, never()).changeList(any())
    }

    @Test
    fun `undoing a quote from another episode puts Up Next back in its order`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val other = PodcastEpisode(uuid = "other-uuid", publishedDate = Date())
        val queued = listOf(PodcastEpisode(uuid = "queued-1", publishedDate = Date()), PodcastEpisode(uuid = "queued-2", publishedDate = Date()))
        whenever(upNextQueue.queueEpisodes).thenReturn(queued)
        whenever(upNextQueue.currentEpisode).thenReturn(other)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        whenever(episodeManager.findEpisodeByUuid("other-uuid")).thenReturn(other)
        switchPlaybackOnPlayNow(other)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid", positionMs = 10_000)
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        eventSink.skipEvent(eventSink.size)
        viewModel.undoQuotePlay()
        advanceUntilIdle()

        verify(upNextQueue).changeList(queued)
        assertEquals(true, (eventSink.pollEvent() as EpisodeChatQuotePlayUndoneEvent).isOtherEpisode)
    }

    @Test
    fun `undoing a quote from another episode restores its archived and played state`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val other = PodcastEpisode(uuid = "other-uuid", publishedDate = Date())
        val quoteEpisode = PodcastEpisode(
            uuid = EPISODE_UUID,
            publishedDate = Date(),
            duration = 600.0,
            isArchived = true,
            playingStatus = EpisodePlayingStatus.COMPLETED,
            playedUpTo = 600.0,
        )
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(quoteEpisode)
        whenever(episodeManager.findEpisodeByUuid("other-uuid")).thenReturn(other)
        switchPlaybackOnPlayNow(other, quoteEpisode)
        whenever(playbackManager.playNowSuspend(org.mockito.kotlin.same(quoteEpisode), any(), any(), any())).then {
            quoteEpisode.isArchived = false
            quoteEpisode.playingStatus = EpisodePlayingStatus.NOT_PLAYED
            quoteEpisode.playedUpTo = 0.0
            playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
            Unit
        }
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid", positionMs = 10_000)
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        verify(episodeManager).updatePlayingStatusBlocking(org.mockito.kotlin.same(quoteEpisode), org.mockito.kotlin.eq(EpisodePlayingStatus.COMPLETED))
        verify(episodeManager).updatePlayedUpToBlocking(org.mockito.kotlin.same(quoteEpisode), org.mockito.kotlin.eq(600.0), org.mockito.kotlin.eq(true))
        verify(episodeManager).archiveAllInList(listOf(quoteEpisode), null)
        verify(episodeManager, never()).archiveBlocking(any(), any(), any(), any())
    }

    @Test
    fun `undoing a quote when nothing was loaded takes the episode out of Up Next`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        switchPlaybackOnPlayNow(other = null)
        playbackState.value = PlaybackState()
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        val order = inOrder(playbackManager, upNextQueue, episodeManager)
        order.verify(playbackManager).stop()
        order.verify(upNextQueue).removeAll()
        order.verify(episodeManager).updatePlayedUpToBlocking(any(), any(), org.mockito.kotlin.eq(true))
        verify(playbackManager, never()).removeEpisode(any(), any(), any(), any())
    }

    @Test
    fun `undoing a quote in the same episode leaves Up Next alone`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        verify(upNextQueue, never()).changeList(any())
        verify(episodeManager, never()).updatePlayingStatusBlocking(any(), any())
    }

    @Test
    fun `undoing a quote from another chat after one in the playing episode goes back to that episode`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val otherQuote = createQuote(startMs = 2_000, endMs = 4_000, uuid = "other-quote-uuid")
        val playing = episode.value
        val other = PodcastEpisode(
            uuid = OTHER_EPISODE_UUID,
            publishedDate = Date(),
            duration = 300.0,
            isArchived = true,
            playingStatus = EpisodePlayingStatus.COMPLETED,
            playedUpTo = 300.0,
        )
        val queued = listOf(PodcastEpisode(uuid = "queued-1", publishedDate = Date()))
        whenever(upNextQueue.queueEpisodes).thenReturn(queued)
        whenever(upNextQueue.currentEpisode).thenReturn(playing)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(playing)
        whenever(episodeManager.findEpisodeByUuid(OTHER_EPISODE_UUID)).thenReturn(other)
        whenever(playbackManager.playNowSuspend(org.mockito.kotlin.same(other), any(), any(), any())).then {
            playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = OTHER_EPISODE_UUID)
            Unit
        }
        whenever(playbackManager.playNowSuspend(org.mockito.kotlin.same(playing), any(), any(), any())).then {
            playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID)
            Unit
        }
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        chatManager.otherMessages.value = listOf(otherQuote)
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 1_000)
        viewModel.openHistoryChat(historyItem(OTHER_EPISODE_UUID))
        advanceUntilIdle()
        viewModel.playQuote(otherQuote.uuid)
        advanceUntilIdle()
        eventSink.skipEvent(eventSink.size)

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        assertEquals(true, (eventSink.pollEvent() as EpisodeChatQuotePlayUndoneEvent).isOtherEpisode)
        verify(playbackManager).playNowSuspend(org.mockito.kotlin.same(playing), any(), any(), any())
        verify(upNextQueue).changeList(queued)
        verify(episodeManager).updatePlayingStatusBlocking(org.mockito.kotlin.same(other), org.mockito.kotlin.eq(EpisodePlayingStatus.COMPLETED))
        verify(episodeManager).archiveAllInList(listOf(other), null)
        verify(playbackManager).seekToTimeMsSuspend(org.mockito.kotlin.eq(10_000), anyOrNull())
    }

    private suspend fun quotePlayingToast() = viewModel.toasts.first() as ChatToast.QuotePlaying

    private suspend fun switchPlaybackOnPlayNow(other: BaseEpisode?, quoteEpisode: BaseEpisode = episode.value) {
        whenever(playbackManager.playNowSuspend(org.mockito.kotlin.eq(quoteEpisode), any(), any(), any())).then {
            playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
            Unit
        }
        if (other != null) {
            whenever(playbackManager.playNowSuspend(org.mockito.kotlin.eq(other), any(), any(), any())).then {
                playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = other.uuid)
                Unit
            }
        }
    }

    @Test
    fun `a tapped quote loads until it plays`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val resolved = CompletableDeferred<Int>()
        whenever(quoteSeeker.playbackTimeMs(any(), any())).doSuspendableAnswer { resolved.await() }
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID)
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        assertEquals(quote.uuid, viewModel.uiState.value.loadingQuoteUuid)
        advanceUntilIdle()
        assertEquals(quote.uuid, viewModel.uiState.value.loadingQuoteUuid)

        resolved.complete(1_000)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadingQuoteUuid)
        verify(playbackManager).playQueueSuspend(any(), any())
    }

    @Test
    fun `a tapped quote stops loading when its episode never loads`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid")
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        runCurrent()
        assertEquals(quote.uuid, viewModel.uiState.value.loadingQuoteUuid)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadingQuoteUuid)
    }

    @Test
    fun `a tapped quote stops loading when its episode cannot be found`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadingQuoteUuid)
        verifyNoSeek()
    }

    @Test
    fun `a tapped quote stops loading when the user seeks first`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val resolved = CompletableDeferred<Int>()
        whenever(quoteSeeker.playbackTimeMs(any(), any())).doSuspendableAnswer { resolved.await() }
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID)
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()
        userSeeks.emit(Unit)
        resolved.complete(1_000)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadingQuoteUuid)
    }

    @Test
    fun `tapping a loading quote again is ignored`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        val resolved = CompletableDeferred<Int>()
        whenever(quoteSeeker.playbackTimeMs(any(), any())).doSuspendableAnswer { resolved.await() }
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID)
        playableState(quote)
        eventSink.skipEvent(eventSink.size)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()
        resolved.complete(1_000)
        advanceUntilIdle()

        assertTrue(eventSink.pollEvent() is EpisodeChatQuotePlayTappedEvent)
        assertTrue(eventSink.isEmpty())
        verify(quoteSeeker, times(1)).playbackTimeMs(any(), any())
        verify(playbackManager, times(1)).seekToTimeMsSuspend(any(), anyOrNull())
    }

    @Test
    fun `tapping another quote moves the loading to it`() = runTest {
        val first = createQuote(startMs = 1_000, endMs = 3_000)
        val second = createQuote(startMs = 5_000, endMs = 7_000, uuid = "second-quote-uuid")
        val resolved = CompletableDeferred<Int>()
        whenever(quoteSeeker.playbackTimeMs(any(), any())).doSuspendableAnswer { resolved.await() }
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID)
        chatManager.messages.value = listOf(first, second)
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.playQuote(first.uuid)
        advanceUntilIdle()
        viewModel.playQuote(second.uuid)
        advanceUntilIdle()

        assertEquals(second.uuid, viewModel.uiState.value.loadingQuoteUuid)
        resolved.complete(5_000)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.loadingQuoteUuid)
        verify(playbackManager, times(1)).seekToTimeMsSuspend(org.mockito.kotlin.eq(5_000), anyOrNull())
    }

    @Test
    fun `switching chats stops showing the quote loading`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(quoteSeeker.playbackTimeMs(any(), any())).doSuspendableAnswer { CompletableDeferred<Int>().await() }
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID)
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        viewModel.openHistoryChat(historyItem(OTHER_EPISODE_UUID))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadingQuoteUuid)
    }

    @Test
    fun `play pause while a quote is starting cancels the quote`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid")
        whenever(playbackManager.playNowSuspend(any<String>(), any(), any(), any())).then {
            playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
            Unit
        }
        playableState(quote)

        viewModel.playQuote(quote.uuid)
        runCurrent()
        viewModel.onPlayPauseClick()
        advanceUntilIdle()

        verify(playbackManager, never()).seekToTimeMsSuspend(any(), anyOrNull())
        verify(playbackManager, never()).playQueueSuspend(any(), any())
    }

    @Test
    fun `play quote outside stored episode duration does not seek`() = runTest {
        val quote = createQuote(startMs = 49_020_000, endMs = 49_500_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(
            PodcastEpisode(uuid = EPISODE_UUID, publishedDate = Date(), duration = 123.0),
        )
        playableState(quote, episodeDurationMs = 0)

        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()

        verifyNoSeek()
    }

    @Test
    fun `playback shows the saved position when nothing is playing`() = runTest {
        setEpisodeInfo()
        advanceUntilIdle()

        assertEquals(ChatPlayback(isPlaying = false, positionMs = 50_000, durationMs = 200_000), viewModel.uiState.value.playback)
        assertNull(viewModel.uiState.value.otherPlayingEpisode)
    }

    @Test
    fun `header follows another playing episode while the chat stays on its episode`() = runTest {
        setEpisodeInfo()
        playbackState.value = PlaybackState(
            state = PlaybackState.State.PLAYING,
            episodeUuid = "other-uuid",
            title = "Other episode",
            podcast = Podcast(uuid = "other-podcast-uuid", title = "Other podcast"),
            positionMs = 10_000,
            durationMs = 40_000,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ChatPlayingEpisode("Other episode", "other-podcast-uuid", "Other podcast"), state.otherPlayingEpisode)
        assertEquals(ChatPlayback(isPlaying = true, positionMs = 10_000, durationMs = 40_000), state.playback)
        assertEquals("Episode title", state.episodeTitle)
    }

    @Test
    fun `header shows a playing user file`() = runTest {
        val userEpisode = UserEpisode(uuid = "user-file-uuid", publishedDate = Date(), title = "My recording")
        whenever(episodeManager.findEpisodeByUuid("user-file-uuid")).thenReturn(userEpisode)
        setEpisodeInfo()
        playbackState.value = PlaybackState(
            state = PlaybackState.State.PLAYING,
            episodeUuid = "user-file-uuid",
            title = "My recording",
        )
        advanceUntilIdle()

        assertEquals(
            ChatPlayingEpisode("My recording", podcastUuid = "", podcastTitle = "", userEpisode = userEpisode),
            viewModel.uiState.value.otherPlayingEpisode,
        )
    }

    @Test
    fun `the playing podcast is exposed for the chat colours only while an episode is loaded`() = runTest {
        val podcast = Podcast(uuid = "other-podcast-uuid", title = "Other podcast")
        setEpisodeInfo()
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid", podcast = podcast)
        advanceUntilIdle()

        assertEquals(podcast, viewModel.uiState.value.playingPodcast)

        playbackState.value = PlaybackState(state = PlaybackState.State.STOPPED, episodeUuid = "other-uuid", podcast = podcast)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.playingPodcast)
    }

    @Test
    fun `header returns to the chat episode when it starts playing`() = runTest {
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid", title = "Other episode")
        setEpisodeInfo()
        advanceUntilIdle()

        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 1_000)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.otherPlayingEpisode)
    }

    @Test
    fun `stopped playback shows the chat episode`() = runTest {
        playbackState.value = PlaybackState(state = PlaybackState.State.STOPPED, episodeUuid = "other-uuid", title = "Other episode")
        setEpisodeInfo()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.otherPlayingEpisode)
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
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.onPlayPauseClick()
        advanceUntilIdle()

        verify(playbackManager).pause(transientLoss = false, sourceView = SourceView.PLAYER)
        verify(playbackManager, never()).playNowSuspend(any<String>(), any(), any(), any())
    }

    @Test
    fun `play pause pauses another playing episode`() = runTest {
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid")
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.onPlayPauseClick()
        advanceUntilIdle()

        verify(playbackManager).pause(transientLoss = false, sourceView = SourceView.PLAYER)
        verify(playbackManager, never()).playNowSuspend(any<String>(), any(), any(), any())
    }

    @Test
    fun `play pause resumes another paused episode`() = runTest {
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = "other-uuid")
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.onPlayPauseClick()
        advanceUntilIdle()

        verify(playbackManager).playQueueSuspend(SourceView.PLAYER, false)
        verify(playbackManager, never()).playNowSuspend(any<String>(), any(), any(), any())
    }

    @Test
    fun `play pause plays the chat episode when nothing is playing`() = runTest {
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.onPlayPauseClick()
        advanceUntilIdle()

        verify(playbackManager).playNowSuspend(EPISODE_UUID, false, false, SourceView.PLAYER)
        verify(playbackManager, never()).pause(any(), any())
    }

    @Test
    fun `play pause resumes the paused chat episode without reloading it`() = runTest {
        playbackState.value = PlaybackState(state = PlaybackState.State.PAUSED, episodeUuid = EPISODE_UUID)
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.onPlayPauseClick()
        advanceUntilIdle()

        verify(playbackManager).playQueueSuspend(SourceView.PLAYER, false)
        verify(playbackManager, never()).playNowSuspend(any<String>(), any(), any(), any())
    }

    @Test
    fun `playback progress is clamped and safe without a duration`() {
        assertEquals(0.25f, ChatPlayback(positionMs = 50, durationMs = 200).progress)
        assertEquals(1f, ChatPlayback(positionMs = 300, durationMs = 200).progress)
        assertEquals(0f, ChatPlayback(positionMs = 300, durationMs = 0).progress)
    }

    private fun TestScope.connectToInternet() {
        networkConnectionWatcher.networkCapabilities.value = internetCapabilities()
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
        chatManager.messages.value = listOf(quote)
        setEpisodeInfo(episodeDurationMs)
        advanceUntilIdle()
        return viewModel.uiState.value.messages.filterIsInstance<ChatMessage.Quote>().single().canPlay
    }

    private fun createQuote(startMs: Int, endMs: Int, uuid: String = "quote-uuid") = ChatMessage.Quote(
        text = "Quote",
        start = "start",
        end = "end",
        startMs = startMs,
        endMs = endMs,
        uuid = uuid,
    )

    private fun setEpisodeInfo(
        episodeDurationMs: Int = 123_000,
        sourceView: SourceView = SourceView.PLAYER,
        entryPoint: ChatEntryPoint = ChatEntryPoint.PlayerShelf,
        isBeta: Boolean = false,
        requiresSignIn: Boolean = false,
    ) {
        viewModel.setEpisodeInfo(
            episodeUuid = EPISODE_UUID,
            episodeTitle = "Episode title",
            podcastUuid = PODCAST_UUID,
            podcastTitle = "Podcast title",
            episodeDurationMs = episodeDurationMs,
            sourceView = sourceView,
            entryPoint = entryPoint,
            isBeta = isBeta,
            requiresSignIn = requiresSignIn,
        )
    }

    @Test
    fun `beta user sees the beta sheet and it is tracked`() = runTest {
        setEpisodeInfo(isBeta = true)
        eventSink.skipEvent()

        assertTrue(viewModel.uiState.value.isBetaSheetVisible)
        assertEquals(
            EpisodeChatBetaSheetShownEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `setting episode info again after recreation keeps the chat and does not track again`() = runTest {
        setEpisodeInfo(isBeta = true)
        eventSink.skipEvent(2)

        setEpisodeInfo(isBeta = true)

        assertTrue(viewModel.uiState.value.isBetaSheetVisible)
        assertEquals(1, chatManager.createdChats.size)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `signed out user always sees the beta sheet asking to sign in`() = runTest {
        betaSheetSeen.value = true
        isLoggedIn = false

        setEpisodeInfo(isBeta = true, requiresSignIn = true)

        val state = viewModel.uiState.value
        assertTrue(state.isBetaSheetVisible)
        assertTrue(state.isSignInRequired)
        assertEquals(
            EpisodeChatBetaSheetShownEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `tapping sign in on the beta sheet starts sign in and tracks it`() = runTest {
        isLoggedIn = false
        setEpisodeInfo(isBeta = true, requiresSignIn = true)
        eventSink.skipEvent()

        viewModel.navigation.test {
            viewModel.onSignInClick()

            assertEquals(ChatNavigation.SignIn, awaitItem())
        }
        assertEquals(
            EpisodeChatBetaSheetSignInTappedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
        assertTrue(viewModel.uiState.value.isBetaSheetVisible)
    }

    @Test
    fun `coming back signed in hides the beta sheet and marks it seen`() = runTest {
        isLoggedIn = false
        setEpisodeInfo(isBeta = true, requiresSignIn = true)
        eventSink.skipEvent()

        isLoggedIn = true
        viewModel.refreshSignIn()

        val state = viewModel.uiState.value
        assertFalse(state.isBetaSheetVisible)
        assertFalse(state.isSignInRequired)
        verify(betaSheetSeenSetting).set(true, updateModifiedAt = false)
        assertTrue(eventSink.pollEvent() is EpisodeChatShownEvent)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `a plus account signing in leaves beta mode`() = runTest {
        isLoggedIn = false
        setEpisodeInfo(isBeta = true, requiresSignIn = true)

        isLoggedIn = true
        viewModel.refreshSignIn()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isBeta)

        subscription.value = Subscription(
            tier = SubscriptionTier.Plus,
            billingCycle = BillingCycle.Monthly,
            platform = SubscriptionPlatform.Android,
            expiryDate = Instant.now(),
            isAutoRenewing = true,
            giftDays = 0,
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBeta)
    }

    @Test
    fun `a signed out user is not counted as opening the chat until signing in`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User(text = "Old question", uuid = "old-uuid"))
        isLoggedIn = false
        setEpisodeInfo(isBeta = true, requiresSignIn = true)
        viewModel.trackDismissed()

        assertTrue(eventSink.pollEvent() is EpisodeChatBetaSheetShownEvent)
        assertTrue(eventSink.isEmpty())

        isLoggedIn = true
        viewModel.refreshSignIn()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatShownEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                hasHistory = true,
                entryPoint = EpisodeChatEntryPointType.SignIn,
                isBeta = true,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `a signed out user does not see the saved conversation behind the sign in sheet`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User(text = "Old question", uuid = "old-uuid"))
        isLoggedIn = false
        setEpisodeInfo(isBeta = true, requiresSignIn = true)
        advanceUntilIdle()

        assertEquals(emptyList<ChatMessage>(), viewModel.uiState.value.messages)

        isLoggedIn = true
        viewModel.refreshSignIn()
        advanceUntilIdle()

        assertEquals(listOf("old-uuid"), viewModel.uiState.value.messages.map { it.uuid })
    }

    @Test
    fun `tapping sign in twice starts sign in once`() = runTest {
        isLoggedIn = false
        setEpisodeInfo(isBeta = true, requiresSignIn = true)
        eventSink.skipEvent()

        viewModel.navigation.test {
            viewModel.onSignInClick()
            viewModel.onSignInClick()

            assertEquals(ChatNavigation.SignIn, awaitItem())
            expectNoEvents()
        }
        assertEquals(1, eventSink.size)
    }

    @Test
    fun `an already signed in user is not asked to sign in`() = runTest {
        betaSheetSeen.value = true

        setEpisodeInfo(isBeta = true, requiresSignIn = true)

        assertFalse(viewModel.uiState.value.isSignInRequired)
        assertFalse(viewModel.uiState.value.isBetaSheetVisible)
    }

    @Test
    fun `a chat restored after signing in opens without a second beta sheet`() = runTest {
        setEpisodeInfo(isBeta = true, requiresSignIn = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSignInRequired)
        assertFalse(state.isBetaSheetVisible)
        assertTrue(state.isBeta)
        verify(betaSheetSeenSetting).set(true, updateModifiedAt = false)
        assertEquals(
            EpisodeChatShownEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                hasHistory = false,
                entryPoint = EpisodeChatEntryPointType.SignIn,
                isBeta = true,
            ),
            eventSink.pollEvent(),
        )
        assertTrue(eventSink.isEmpty())

        viewModel.refreshSignIn()
        advanceUntilIdle()

        assertEquals(1, chatManager.createdChats.size)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `a plus account restored after signing in leaves beta mode without the beta sheet`() = runTest {
        subscription.value = Subscription(
            tier = SubscriptionTier.Plus,
            billingCycle = BillingCycle.Monthly,
            platform = SubscriptionPlatform.Android,
            expiryDate = Instant.now(),
            isAutoRenewing = true,
            giftDays = 0,
        )

        setEpisodeInfo(isBeta = true, requiresSignIn = true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBeta)
        assertFalse(viewModel.uiState.value.isBetaSheetVisible)
        assertEquals(false, (eventSink.pollEvent() as EpisodeChatShownEvent).isBeta)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `coming back without signing in keeps the beta sheet`() = runTest {
        isLoggedIn = false
        setEpisodeInfo(isBeta = true, requiresSignIn = true)

        viewModel.refreshSignIn()

        assertTrue(viewModel.uiState.value.isBetaSheetVisible)
        verify(betaSheetSeenSetting, never()).set(any(), any(), any(), any())
    }

    @Test
    fun `dismissing the sign in sheet closes the chat without marking the sheet seen`() = runTest {
        isLoggedIn = false
        setEpisodeInfo(isBeta = true, requiresSignIn = true)

        viewModel.navigation.test {
            viewModel.dismissBetaSheet()

            assertEquals(ChatNavigation.Close, awaitItem())
        }
        verify(betaSheetSeenSetting, never()).set(any(), any(), any(), any())
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
    fun `plus user sees the beta note while the feature is in beta`() = runTest {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT_FREE_BETA, true)

        setEpisodeInfo(isBeta = false)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isBetaNoteVisible)
        assertFalse(viewModel.uiState.value.isBetaSheetVisible)
    }

    @Test
    fun `nobody sees the beta note once the feature leaves beta`() = runTest {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT_FREE_BETA, false)

        setEpisodeInfo(isBeta = true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBetaNoteVisible)
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
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `showing the history groups the chats and tracks how many there are`() = runTest {
        chatManager.history.value = listOf(historyItem(EPISODE_UUID), historyItem(OTHER_EPISODE_UUID, daysAgo = 3))
        setEpisodeInfo()
        advanceUntilIdle()
        eventSink.skipEvent()

        viewModel.showHistory()

        val state = viewModel.uiState.value
        assertTrue(state.isHistoryVisible)
        assertEquals(listOf(ChatHistoryGroup.Today, ChatHistoryGroup.PreviousSevenDays), state.history.map { it.group })
        assertEquals(EpisodeChatHistoryShownEvent(source = SourceView.PLAYER.analyticsValue, chatCount = 2), eventSink.pollEvent())
    }

    @Test
    fun `showing the history before it loads tracks how many chats there are once it loads`() = runTest {
        val history = MutableSharedFlow<List<EpisodeChatHistoryItem>>()
        chatManager.observedHistory = history
        setEpisodeInfo()
        advanceUntilIdle()
        eventSink.skipEvent()

        viewModel.showHistory()
        assertTrue(eventSink.isEmpty())
        history.emit(listOf(historyItem(EPISODE_UUID), historyItem(OTHER_EPISODE_UUID, daysAgo = 3)))
        advanceUntilIdle()

        assertEquals(EpisodeChatHistoryShownEvent(source = SourceView.PLAYER.analyticsValue, chatCount = 2), eventSink.pollEvent())
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `closing the history before it loads does not track it as shown`() = runTest {
        val history = MutableSharedFlow<List<EpisodeChatHistoryItem>>()
        chatManager.observedHistory = history
        setEpisodeInfo()
        advanceUntilIdle()
        eventSink.skipEvent()

        viewModel.showHistory()
        viewModel.closeHistory()
        history.emit(listOf(historyItem(EPISODE_UUID)))
        advanceUntilIdle()

        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `showing the history again regroups the chats for the current day`() = runTest {
        chatManager.history.value = listOf(historyItem(EPISODE_UUID))
        setEpisodeInfo()
        advanceUntilIdle()
        clock += 1.days

        viewModel.showHistory()

        assertEquals(listOf(ChatHistoryGroup.Yesterday), viewModel.uiState.value.history.map { it.group })
    }

    @Test
    fun `closing the history goes back to the chat`() = runTest {
        setEpisodeInfo()
        viewModel.showHistory()

        viewModel.closeHistory()

        assertFalse(viewModel.uiState.value.isHistoryVisible)
    }

    @Test
    fun `opening another chat from the history shows its messages and keeps the header on what is playing`() = runTest {
        val otherMessage = ChatMessage.User(text = "Other question", uuid = "other-message-uuid")
        chatManager.otherMessages.value = listOf(otherMessage)
        chatManager.messages.value = listOf(ChatMessage.User(text = "Question", uuid = "message-uuid"))
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, title = "Episode title")
        setEpisodeInfo()
        advanceUntilIdle()
        viewModel.showHistory()
        eventSink.skipEvent(2)

        viewModel.openHistoryChat(historyItem(OTHER_EPISODE_UUID))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isHistoryVisible)
        assertEquals(OTHER_EPISODE_UUID, state.episodeUuid)
        assertEquals("Title $OTHER_EPISODE_UUID", state.episodeTitle)
        assertEquals(listOf(otherMessage), state.messages)
        assertEquals("Episode title", state.otherPlayingEpisode?.episodeTitle)
        assertEquals(CreateChat(OTHER_EPISODE_UUID, OTHER_PODCAST_UUID), chatManager.createdChats.last())
        assertEquals(
            EpisodeChatHistoryRowTappedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = OTHER_EPISODE_UUID,
                podcastUuid = OTHER_PODCAST_UUID,
                isNowPlaying = false,
            ),
            eventSink.pollEvent(),
        )
        assertEquals(
            EpisodeChatShownEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = OTHER_EPISODE_UUID,
                podcastUuid = OTHER_PODCAST_UUID,
                hasHistory = true,
                entryPoint = ChatEntryPoint.History.analyticsValue,
                isBeta = false,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `opening a chat from the history tracks its section and position`() = runTest {
        chatManager.history.value = listOf(
            historyItem(EPISODE_UUID, podcastUuid = PODCAST_UUID),
            historyItem("yesterday-episode", daysAgo = 1),
            historyItem(OTHER_EPISODE_UUID, daysAgo = 3),
        )
        setEpisodeInfo()
        viewModel.showHistory()
        eventSink.skipEvent(eventSink.size)

        viewModel.openHistoryChat(historyItem(OTHER_EPISODE_UUID, daysAgo = 3))

        assertEquals(
            EpisodeChatHistoryRowTappedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = OTHER_EPISODE_UUID,
                podcastUuid = OTHER_PODCAST_UUID,
                isNowPlaying = false,
                section = EpisodeChatHistorySectionType.Previous7Days,
                position = 3,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `opening the playing episode chat from the history tracks it as now playing`() = runTest {
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID)
        setEpisodeInfo()
        advanceUntilIdle()
        eventSink.skipEvent()

        viewModel.openHistoryChat(historyItem(EPISODE_UUID, podcastUuid = PODCAST_UUID))

        assertEquals(
            EpisodeChatHistoryRowTappedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                isNowPlaying = true,
            ),
            eventSink.pollEvent(),
        )
        assertTrue(eventSink.isEmpty())
        assertEquals(1, chatManager.createdChats.size)
    }

    @Test
    fun `deleting another chat from the history offers undo and restores it`() = runTest {
        val otherMessage = ChatMessage.User(text = "Other question", uuid = "other-message-uuid")
        chatManager.otherMessages.value = listOf(otherMessage)
        val message = ChatMessage.User(text = "Question", uuid = "message-uuid")
        chatManager.messages.value = listOf(message)
        setEpisodeInfo()
        advanceUntilIdle()
        eventSink.skipEvent()

        viewModel.toasts.test {
            viewModel.deleteHistoryChat(historyItem(OTHER_EPISODE_UUID))
            advanceUntilIdle()

            assertEquals(ChatToast.ChatDeleted, awaitItem())
        }
        assertEquals(emptyList<ChatMessage>(), chatManager.otherMessages.value)
        assertEquals(listOf(message), viewModel.uiState.value.messages)

        viewModel.undoDeleteChat()
        advanceUntilIdle()

        assertEquals(listOf(otherMessage), chatManager.otherMessages.value)
        assertEquals(
            EpisodeChatDeletedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = OTHER_EPISODE_UUID,
                podcastUuid = OTHER_PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
        assertEquals(
            EpisodeChatDeleteUndoneEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = OTHER_EPISODE_UUID,
                podcastUuid = OTHER_PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `deleting the open chat from the history empties the conversation`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User(text = "Question", uuid = "message-uuid"))
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.deleteHistoryChat(historyItem(EPISODE_UUID, podcastUuid = PODCAST_UUID))
        advanceUntilIdle()

        assertEquals(emptyList<ChatMessage>(), viewModel.uiState.value.messages)
        assertEquals(listOf(EPISODE_UUID), chatManager.clearedEpisodeUuids)
    }

    @Test
    fun `sending in the open chat keeps the undo for another deleted chat`() = runTest {
        val otherMessage = ChatMessage.User(text = "Other question", uuid = "other-message-uuid")
        chatManager.otherMessages.value = listOf(otherMessage)
        setEpisodeInfo()
        connectToInternet()
        advanceUntilIdle()
        viewModel.deleteHistoryChat(historyItem(OTHER_EPISODE_UUID))
        advanceUntilIdle()

        viewModel.undoExpired.test {
            viewModel.onInputTextChange("New question")
            viewModel.onSend()
            advanceUntilIdle()
            expectNoEvents()
        }
        viewModel.undoDeleteChat()
        advanceUntilIdle()

        assertEquals(listOf(otherMessage), chatManager.otherMessages.value)
    }

    @Test
    fun `sending after deleting the open chat expires its undo`() = runTest {
        chatManager.messages.value = listOf(ChatMessage.User(text = "Question", uuid = "message-uuid"))
        setEpisodeInfo()
        connectToInternet()
        advanceUntilIdle()
        viewModel.deleteHistoryChat(historyItem(EPISODE_UUID, podcastUuid = PODCAST_UUID))
        advanceUntilIdle()

        viewModel.undoExpired.test {
            viewModel.onInputTextChange("New question")
            viewModel.onSend()
            advanceUntilIdle()
            awaitItem()
        }
    }

    @Test
    fun `history opens while an answer is pending and the answer still lands`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        val gate = CompletableDeferred<Unit>()
        chatManager.sendMessageGate = gate
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        runCurrent()

        viewModel.showHistory()
        assertTrue(viewModel.uiState.value.isHistoryVisible)
        assertTrue(viewModel.uiState.value.isAwaitingReply)

        gate.complete(Unit)
        advanceUntilIdle()
        viewModel.closeHistory()

        val state = viewModel.uiState.value
        assertFalse(state.isAwaitingReply)
        assertNull(state.error)
        assertEquals(listOf("Question", "Response"), state.messages.map { it.text() })
        assertEquals(SendMessage(EPISODE_UUID, "Question"), chatManager.sentMessages.single())
    }

    @Test
    fun `an answer that fails while the history is open shows its error back in the chat`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        val gate = CompletableDeferred<Unit>()
        chatManager.sendMessageGate = gate
        chatManager.sendMessageException = IOException()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        runCurrent()
        viewModel.showHistory()

        gate.complete(Unit)
        advanceUntilIdle()
        viewModel.closeHistory()

        val state = viewModel.uiState.value
        assertFalse(state.isAwaitingReply)
        assertEquals(ChatError.NetworkError, state.error)
        assertEquals(listOf("Question"), state.messages.map { it.text() })
    }

    @Test
    fun `deleting the open chat from the history while answering stops the answer and undo brings the question back`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        chatManager.sendMessageGate = CompletableDeferred()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        runCurrent()
        viewModel.showHistory()

        viewModel.deleteHistoryChat(historyItem(EPISODE_UUID, podcastUuid = PODCAST_UUID))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAwaitingReply)
        assertEquals(emptyList<ChatMessage>(), viewModel.uiState.value.messages)
        assertTrue(chatManager.sentMessages.isEmpty())

        viewModel.undoDeleteChat()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Question"), state.messages.map { it.text() })
        assertEquals(ChatError.NetworkError, state.error)
        assertTrue(chatManager.sentMessages.isEmpty())
    }

    @Test
    fun `tapping the open chat in the history while answering goes back to the pending answer`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        val gate = CompletableDeferred<Unit>()
        chatManager.sendMessageGate = gate
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        runCurrent()
        viewModel.showHistory()

        viewModel.openHistoryChat(historyItem(EPISODE_UUID, podcastUuid = PODCAST_UUID))
        runCurrent()

        assertFalse(viewModel.uiState.value.isHistoryVisible)
        assertTrue(viewModel.uiState.value.isAwaitingReply)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("Question", "Response"), viewModel.uiState.value.messages.map { it.text() })
        assertEquals(1, chatManager.createdChats.size)
    }

    @Test
    fun `opening another chat from the history while answering leaves the question not sent`() = runTest {
        setEpisodeInfo()
        connectToInternet()
        chatManager.sendMessageGate = CompletableDeferred()
        viewModel.onInputTextChange("Question")
        viewModel.onSend()
        runCurrent()
        viewModel.showHistory()

        viewModel.openHistoryChat(historyItem(OTHER_EPISODE_UUID))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isAwaitingReply)
        viewModel.openHistoryChat(historyItem(EPISODE_UUID, podcastUuid = PODCAST_UUID))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EPISODE_UUID, state.episodeUuid)
        assertFalse(state.isAwaitingReply)
        assertEquals(ChatError.NetworkError, state.error)
        assertEquals(listOf("Question"), state.messages.map { it.text() })
        assertTrue(chatManager.sentMessages.isEmpty())

        chatManager.sendMessageGate = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(SendMessage(EPISODE_UUID, "Question"), chatManager.sentMessages.single())
        assertEquals(listOf("Question", "Response"), viewModel.uiState.value.messages.map { it.text() })
    }

    @Test
    fun `opening another chat clears the typed question`() = runTest {
        setEpisodeInfo()
        advanceUntilIdle()
        viewModel.onInputTextChange("Half typed")

        viewModel.openHistoryChat(historyItem(OTHER_EPISODE_UUID))
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun `opening another chat restores its stored ratings`() = runTest {
        val answer = ChatMessage.Assistant(text = "Answer", uuid = "answer-uuid", rating = ChatAnswerRating.Positive)
        chatManager.otherMessages.value = listOf(ChatMessage.User(text = "Question", uuid = "question-uuid"), answer)
        chatManager.messages.value = listOf(ChatMessage.User(text = "Question", uuid = "message-uuid"))
        setEpisodeInfo()
        advanceUntilIdle()

        viewModel.openHistoryChat(historyItem(OTHER_EPISODE_UUID))
        chatManager.messages.value = emptyList()
        advanceUntilIdle()

        assertEquals(mapOf("answer-uuid" to ChatAnswerRating.Positive), viewModel.uiState.value.answerRatings)
    }

    @Test
    fun `undoing a quote after switching chats tracks the quote episode`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = EPISODE_UUID, positionMs = 10_000)
        playableState(quote)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()
        viewModel.openHistoryChat(historyItem(OTHER_EPISODE_UUID))
        advanceUntilIdle()
        eventSink.skipEvent(eventSink.size)

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatQuotePlayUndoneEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                isOtherEpisode = false,
            ),
            eventSink.pollEvent(),
        )
        verify(playbackManager, never()).playNowSuspend(any<BaseEpisode>(), any(), any(), any())
    }

    @Test
    fun `undoing a quote when nothing was loaded tracks it as the same episode like the tap`() = runTest {
        val quote = createQuote(startMs = 1_000, endMs = 3_000)
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        switchPlaybackOnPlayNow(other = null)
        playbackState.value = PlaybackState()
        playableState(quote)
        eventSink.skipEvent(eventSink.size)
        viewModel.playQuote(quote.uuid)
        advanceUntilIdle()
        assertEquals(false, (eventSink.pollEvent() as EpisodeChatQuotePlayTappedEvent).isOtherEpisode)

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        assertEquals(false, (eventSink.pollEvent() as EpisodeChatQuotePlayUndoneEvent).isOtherEpisode)
    }

    @Test
    fun `undoing chained quotes tracks another episode when any tap started from one`() = runTest {
        val first = createQuote(startMs = 1_000, endMs = 3_000)
        val second = createQuote(startMs = 5_000, endMs = 7_000, uuid = "second-quote-uuid")
        whenever(episodeManager.findEpisodeByUuid(EPISODE_UUID)).thenReturn(episode.value)
        whenever(episodeManager.findEpisodeByUuid("other-uuid")).thenReturn(PodcastEpisode(uuid = "other-uuid", publishedDate = Date()))
        switchPlaybackOnPlayNow(other = null)
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = "other-uuid", positionMs = 10_000)
        chatManager.messages.value = listOf(first, second)
        setEpisodeInfo(123_000)
        advanceUntilIdle()
        viewModel.playQuote(first.uuid)
        advanceUntilIdle()
        viewModel.playQuote(second.uuid)
        advanceUntilIdle()
        eventSink.skipEvent(eventSink.size)

        viewModel.undoQuotePlay()
        advanceUntilIdle()

        assertEquals(true, (eventSink.pollEvent() as EpisodeChatQuotePlayUndoneEvent).isOtherEpisode)
    }

    private fun historyItem(
        episodeUuid: String,
        podcastUuid: String = OTHER_PODCAST_UUID,
        daysAgo: Long = 0,
    ) = EpisodeChatHistoryItem(
        episodeUuid = episodeUuid,
        episodeTitle = "Title $episodeUuid",
        podcastUuid = podcastUuid,
        podcastTitle = "Podcast",
        episodeDuration = 300.0,
        lastMessageAt = Date.from(clock.instant().minus(daysAgo, ChronoUnit.DAYS)),
    )

    private class TestNetworkConnectionWatcher : NetworkConnectionWatcher {
        override val networkCapabilities = MutableStateFlow<NetworkCapabilities?>(internetCapabilities())
    }

    private class TestChatManager : ChatManager {
        val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        val otherMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
        var observedMessages: Flow<List<ChatMessage>>? = null
        val createdChats = mutableListOf<CreateChat>()
        val sentMessages = mutableListOf<SendMessage>()
        val sentHistories = mutableListOf<List<String>>()
        val clearedEpisodeUuids = mutableListOf<String>()
        val clearedMessageUuids = mutableListOf<String>()
        var sendMessageException: Exception? = null
        var sendMessageGate: CompletableDeferred<Unit>? = null
        var clearMessagesGate: CompletableDeferred<Unit>? = null
        val savedRatings = mutableListOf<Pair<String, ChatAnswerRating?>>()

        val history = MutableStateFlow<List<EpisodeChatHistoryItem>>(emptyList())

        override fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> = observedMessages ?: messagesFor(episodeUuid)

        private fun messagesFor(episodeUuid: String) = if (episodeUuid == OTHER_EPISODE_UUID) otherMessages else messages

        var observedHistory: Flow<List<EpisodeChatHistoryItem>>? = null

        override fun observeHistory(): Flow<List<EpisodeChatHistoryItem>> = observedHistory ?: history

        val discardedMessageUuids = mutableListOf<String>()

        override suspend fun discardMessage(uuid: String) {
            discardedMessageUuids += uuid
            messages.value = messages.value.filterNot { it.uuid == uuid }
        }

        override suspend fun createChat(episodeUuid: String, podcastUuid: String) {
            createdChats += CreateChat(episodeUuid, podcastUuid)
        }

        override suspend fun sendMessage(
            episodeUuid: String,
            message: ChatMessage.User,
            allMessages: List<ChatMessage>,
        ) {
            val episodeMessages = messagesFor(episodeUuid)
            episodeMessages.value = episodeMessages.value.filterNot { it.uuid == message.uuid } + message
            sendMessageGate?.await()
            sendMessageException?.let { exception ->
                val failure = if (exception is IOException) ChatSendFailure.Network else ChatSendFailure.Server
                episodeMessages.value = episodeMessages.value.map { if (it.uuid == message.uuid) message.copy(failure = failure) else it }
                throw exception
            }
            sentMessages += SendMessage(episodeUuid, message.text)
            sentHistories += allMessages.map { it.text() }
            episodeMessages.value += ChatMessage.Assistant(text = "Response", uuid = "response-uuid-${sentMessages.size}")
        }

        override suspend fun clearMessages(episodeUuid: String): ClearedChat {
            clearMessagesGate?.await()
            clearedEpisodeUuids += episodeUuid
            val episodeMessages = messagesFor(episodeUuid)
            val cleared = TestClearedChat(episodeUuid, episodeMessages.value)
            clearedMessageUuids += episodeMessages.value.map { it.uuid }
            episodeMessages.value = emptyList()
            return cleared
        }

        override suspend fun restoreMessages(clearedChat: ClearedChat) {
            val cleared = clearedChat as TestClearedChat
            val episodeMessages = messagesFor(cleared.episodeUuid)
            episodeMessages.value = cleared.messages + episodeMessages.value
        }

        private class TestClearedChat(val episodeUuid: String, override val messages: List<ChatMessage>) : ClearedChat

        override suspend fun rateAnswer(answerUuid: String, rating: ChatAnswerRating?) {
            savedRatings += answerUuid to rating
        }
    }

    private data class CreateChat(val episodeUuid: String, val podcastUuid: String)
    private data class SendMessage(val episodeUuid: String, val message: String)

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val PODCAST_UUID = "podcast-uuid"
        const val OTHER_EPISODE_UUID = "other-episode-uuid"
        const val OTHER_PODCAST_UUID = "other-podcast-uuid"
    }
}

private fun internetCapabilities() = mock<NetworkCapabilities> {
    on { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } doReturn true
}

private fun ChatMessage.text() = when (this) {
    is ChatMessage.User -> text
    is ChatMessage.Assistant -> text
    is ChatMessage.Quote -> text
}
