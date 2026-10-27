package au.com.shiftyjelly.pocketcasts.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatAnswerRating
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.repositories.chat.ClearedChat
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EpisodeChatFeedbackFormDismissedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackFormShownEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackReasonType
import com.automattic.eventhorizon.EpisodeChatFeedbackSubmitTappedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackSubmittedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackTriggerType
import com.automattic.eventhorizon.EventHorizon
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatFeedbackViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val eventSink = TestEventSink()
    private val savedStateHandle = SavedStateHandle()
    private var messagesGate: CompletableDeferred<Unit>? = null
    private var storedMessages: List<ChatMessage> = listOf(
        ChatMessage.User(text = "Question", uuid = "user-uuid"),
        ChatMessage.Assistant(text = "Answer", uuid = "assistant-uuid"),
    )

    private fun createViewModel(
        trigger: EpisodeChatFeedbackTriggerType = EpisodeChatFeedbackTriggerType.SessionSurvey,
        messageIndex: Long? = null,
        answerUuid: String? = null,
    ) = ChatFeedbackViewModel(
        savedStateHandle = savedStateHandle,
        chatManager = TestChatManager(storedMessages) { messagesGate?.await() },
        eventHorizon = EventHorizon(eventSink),
        applicationScope = CoroutineScope(SupervisorJob() + coroutineRule.testDispatcher),
    ).apply {
        onShown(EPISODE_UUID, PODCAST_UUID, SourceView.PLAYER, trigger, messageIndex, answerUuid)
    }

    @Test
    fun `tracks shown once across recreation`() {
        createViewModel()
        createViewModel()

        assertEquals(
            EpisodeChatFeedbackFormShownEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                trigger = EpisodeChatFeedbackTriggerType.SessionSurvey,
            ),
            eventSink.pollEvent(),
        )
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `cannot submit without a reason`() {
        val viewModel = createViewModel()
        eventSink.skipEvent()

        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.submit()
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `details show only for other and are sent trimmed`() = runTest {
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedbackReason.WrongFacts)
        assertFalse(viewModel.uiState.value.showDetails)

        viewModel.onReasonSelected(ChatFeedbackReason.Other)
        viewModel.onDetailsChange("  It missed the guest  ")
        assertTrue(viewModel.uiState.value.showDetails)
        eventSink.skipEvent()

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(eventSink.pollEvent() is EpisodeChatFeedbackSubmitTappedEvent)
        assertEquals(
            EpisodeChatFeedbackSubmittedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                trigger = EpisodeChatFeedbackTriggerType.SessionSurvey,
                reason = EpisodeChatFeedbackReasonType.Other,
                hasDetails = true,
                details = "It missed the guest",
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `details typed before switching away from other are not sent`() = runTest {
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedbackReason.Other)
        viewModel.onDetailsChange("Draft")
        viewModel.onReasonSelected(ChatFeedbackReason.Offensive)
        eventSink.skipEvent()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            EpisodeChatFeedbackSubmitTappedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                trigger = EpisodeChatFeedbackTriggerType.SessionSurvey,
                reason = EpisodeChatFeedbackReasonType.Offensive,
                hasDetails = false,
            ),
            eventSink.pollEvent(),
        )
        assertNull((eventSink.pollEvent() as EpisodeChatFeedbackSubmittedEvent).details)
    }

    @Test
    fun `submits only once and does not track dismissal after submitting`() = runTest {
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedbackReason.NotInteresting)
        eventSink.skipEvent()

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()
        viewModel.submit()
        viewModel.onDismissed()
        advanceUntilIdle()

        assertTrue(eventSink.pollEvent() is EpisodeChatFeedbackSubmitTappedEvent)
        assertTrue(eventSink.pollEvent() is EpisodeChatFeedbackSubmittedEvent)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `submitting emits submitted right away`() = runTest {
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedbackReason.WrongFacts)

        viewModel.submitted.test {
            viewModel.submit()

            awaitItem()
        }
    }

    @Test
    fun `submit from a thumbs down tracks the rated question and answer`() = runTest {
        storedMessages = listOf(
            ChatMessage.User(text = "First question", uuid = "q1"),
            ChatMessage.Assistant(text = "First answer", uuid = "a1"),
            ChatMessage.Quote(text = "A quote", start = "1:00", end = "1:10", uuid = "a1-quote"),
            ChatMessage.User(text = "Second question", uuid = "q2"),
            ChatMessage.Assistant(text = "Second answer", uuid = "a2"),
        )
        val viewModel = createViewModel(
            trigger = EpisodeChatFeedbackTriggerType.ResponseRating,
            messageIndex = 1,
            answerUuid = "a1",
        )
        viewModel.onReasonSelected(ChatFeedbackReason.WrongFacts)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            listOf(
                EpisodeChatFeedbackFormShownEvent(
                    source = SourceView.PLAYER.analyticsValue,
                    episodeUuid = EPISODE_UUID,
                    podcastUuid = PODCAST_UUID,
                    trigger = EpisodeChatFeedbackTriggerType.ResponseRating,
                    messageIndex = 1,
                ),
                EpisodeChatFeedbackSubmitTappedEvent(
                    source = SourceView.PLAYER.analyticsValue,
                    episodeUuid = EPISODE_UUID,
                    podcastUuid = PODCAST_UUID,
                    trigger = EpisodeChatFeedbackTriggerType.ResponseRating,
                    reason = EpisodeChatFeedbackReasonType.WrongFacts,
                    hasDetails = false,
                    messageIndex = 1,
                ),
                EpisodeChatFeedbackSubmittedEvent(
                    source = SourceView.PLAYER.analyticsValue,
                    episodeUuid = EPISODE_UUID,
                    podcastUuid = PODCAST_UUID,
                    trigger = EpisodeChatFeedbackTriggerType.ResponseRating,
                    reason = EpisodeChatFeedbackReasonType.WrongFacts,
                    hasDetails = false,
                    messageIndex = 1,
                    question = "First question",
                    answer = "First answer\n\nA quote",
                ),
            ),
            List(eventSink.size) { eventSink.pollEvent() },
        )
    }

    @Test
    fun `submit from the session survey does not track a question or answer`() = runTest {
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedbackReason.Other)
        viewModel.onDetailsChange("Not useful")
        eventSink.skipEvent()

        viewModel.submit()
        advanceUntilIdle()
        eventSink.skipEvent()

        val event = eventSink.pollEvent() as EpisodeChatFeedbackSubmittedEvent
        assertEquals("Not useful", event.details)
        assertNull(event.question)
        assertNull(event.answer)
    }

    @Test
    fun `submit still tracks when the rated answer is gone`() = runTest {
        storedMessages = emptyList()
        val viewModel = createViewModel(
            trigger = EpisodeChatFeedbackTriggerType.ResponseRating,
            messageIndex = 1,
            answerUuid = "assistant-uuid",
        )
        viewModel.onReasonSelected(ChatFeedbackReason.WrongFacts)
        eventSink.skipEvent()

        viewModel.submit()
        advanceUntilIdle()
        eventSink.skipEvent()

        val event = eventSink.pollEvent() as EpisodeChatFeedbackSubmittedEvent
        assertNull(event.question)
        assertNull(event.answer)
    }

    @Test
    fun `feedback submitted as the form closes is still tracked`() = runTest {
        val viewModelStore = ViewModelStore()
        val viewModel = createViewModel(
            trigger = EpisodeChatFeedbackTriggerType.ResponseRating,
            messageIndex = 1,
            answerUuid = "assistant-uuid",
        )
        viewModelStore.put("feedback", viewModel)
        viewModel.onReasonSelected(ChatFeedbackReason.WrongFacts)
        eventSink.skipEvent()

        messagesGate = CompletableDeferred()

        viewModel.submit()
        advanceUntilIdle()
        viewModelStore.clear()
        messagesGate?.complete(Unit)
        advanceUntilIdle()
        eventSink.skipEvent()

        val event = eventSink.pollEvent() as EpisodeChatFeedbackSubmittedEvent
        assertEquals("Question", event.question)
        assertEquals("Answer", event.answer)
    }

    @Test
    fun `restored after a successful submit emits submitted again`() = runTest {
        createViewModel().apply {
            onReasonSelected(ChatFeedbackReason.WrongFacts)
            submit()
        }
        advanceUntilIdle()

        createViewModel().submitted.test {
            awaitItem()
        }
    }

    @Test
    fun `details are capped`() {
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedbackReason.Other)

        viewModel.onDetailsChange("a".repeat(1_500))

        assertEquals(1_000, viewModel.uiState.value.details.length)
    }

    @Test
    fun `tracks dismissal without submitting`() {
        val viewModel = createViewModel()
        eventSink.skipEvent()

        viewModel.onDismissed()

        assertEquals(
            EpisodeChatFeedbackFormDismissedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                trigger = EpisodeChatFeedbackTriggerType.SessionSurvey,
            ),
            eventSink.pollEvent(),
        )
    }

    private class TestChatManager(
        private val messages: List<ChatMessage>,
        private val beforeMessages: suspend () -> Unit,
    ) : ChatManager {
        override fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> = flow {
            beforeMessages()
            emit(messages)
        }

        override fun observeHistory(): Flow<List<EpisodeChatHistoryItem>> = flowOf(emptyList())

        override suspend fun hasChatWithMessages(episodeUuid: String) = false

        override suspend fun discardMessage(uuid: String) = Unit

        override suspend fun createChat(episodeUuid: String, podcastUuid: String) = Unit

        override suspend fun sendMessage(
            episodeUuid: String,
            message: ChatMessage.User,
            allMessages: List<ChatMessage>,
        ) = Unit

        override suspend fun clearMessages(episodeUuid: String) = object : ClearedChat {
            override val messages = emptyList<ChatMessage>()
        }

        override suspend fun restoreMessages(clearedChat: ClearedChat) = Unit

        override suspend fun rateAnswer(answerUuid: String, rating: ChatAnswerRating?) = Unit

        override suspend fun deleteAllChats() = Unit
    }

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val PODCAST_UUID = "podcast-uuid"
    }
}
