package au.com.shiftyjelly.pocketcasts.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatAnswerRating
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatFeedback
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatFeedbackManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.repositories.chat.ClearedChat
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EpisodeChatFeedbackFormDismissedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackFormShownEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackReasonType
import com.automattic.eventhorizon.EpisodeChatFeedbackSubmitFailedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackSubmitTappedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackSubmittedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackTriggerType
import com.automattic.eventhorizon.EventHorizon
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatFeedbackViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val eventSink = TestEventSink()
    private val savedStateHandle = SavedStateHandle()
    private val conversation = listOf(
        ChatMessage.User(text = "Question", uuid = "user-uuid"),
        ChatMessage.Assistant(text = "Answer", uuid = "assistant-uuid"),
    )
    private var storedMessages: List<ChatMessage> = conversation
    private val feedbackManager = TestChatFeedbackManager()

    private fun createViewModel(
        trigger: EpisodeChatFeedbackTriggerType = EpisodeChatFeedbackTriggerType.SessionSurvey,
        messageIndex: Long? = null,
    ) = ChatFeedbackViewModel(
        savedStateHandle = savedStateHandle,
        chatManager = TestChatManager(storedMessages),
        feedbackManager = feedbackManager,
        eventHorizon = EventHorizon(eventSink),
        applicationScope = CoroutineScope(SupervisorJob() + coroutineRule.testDispatcher),
    ).apply {
        onShown(EPISODE_UUID, PODCAST_UUID, SourceView.PLAYER, trigger, messageIndex)
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

        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.submit()
        assertTrue(feedbackManager.submitted.isEmpty())
    }

    @Test
    fun `details show only for other and are sent trimmed`() = runTest {
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedback.Reason.WrongFacts)
        assertFalse(viewModel.uiState.value.showDetails)

        viewModel.onReasonSelected(ChatFeedback.Reason.Other)
        viewModel.onDetailsChange("  It missed the guest  ")
        assertTrue(viewModel.uiState.value.showDetails)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            ChatFeedback(
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                reason = ChatFeedback.Reason.Other,
                details = "It missed the guest",
                conversation = conversation,
            ),
            feedbackManager.submitted.single(),
        )
    }

    @Test
    fun `an unanswered question at the end is not sent with the feedback`() = runTest {
        storedMessages = conversation + ChatMessage.User(text = "Not sent", uuid = "unsent-uuid")
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedback.Reason.WrongFacts)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(conversation, feedbackManager.submitted.single().conversation)
    }

    @Test
    fun `details typed before switching away from other are not sent`() = runTest {
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedback.Reason.Other)
        viewModel.onDetailsChange("Draft")
        viewModel.onReasonSelected(ChatFeedback.Reason.Offensive)
        eventSink.skipEvent()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("", feedbackManager.submitted.single().details)
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
    }

    @Test
    fun `submits only once and does not track dismissal after submitting`() = runTest {
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedback.Reason.NotInteresting)
        eventSink.skipEvent()

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()
        viewModel.submit()
        viewModel.onDismissed()
        advanceUntilIdle()

        assertEquals(1, feedbackManager.submitted.size)
        assertTrue(eventSink.pollEvent() is EpisodeChatFeedbackSubmitTappedEvent)
        assertTrue(eventSink.pollEvent() is EpisodeChatFeedbackSubmittedEvent)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `submitting locks the form and emits submitted on success`() = runTest {
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedback.Reason.WrongFacts)

        viewModel.submitted.test {
            viewModel.submit()
            assertTrue(viewModel.uiState.value.isSubmitting)
            assertFalse(viewModel.uiState.value.canSubmit)

            advanceUntilIdle()
            awaitItem()
        }
        assertFalse(viewModel.uiState.value.hasSubmitError)
    }

    @Test
    fun `failed submit keeps the selection and allows trying again`() = runTest {
        feedbackManager.failure = IOException("offline")
        val viewModel = createViewModel()
        viewModel.onReasonSelected(ChatFeedback.Reason.Other)
        viewModel.onDetailsChange("Missed the guest")
        eventSink.skipEvent()

        viewModel.submitted.test {
            viewModel.submit()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSubmitting)
            assertTrue(state.hasSubmitError)
            assertEquals(ChatFeedback.Reason.Other, state.reason)
            assertEquals("Missed the guest", state.details)
            expectNoEvents()

            feedbackManager.failure = null
            viewModel.submit()
            assertFalse(viewModel.uiState.value.hasSubmitError)
            advanceUntilIdle()
            awaitItem()
        }
        assertTrue(eventSink.pollEvent() is EpisodeChatFeedbackSubmitTappedEvent)
        assertEquals(
            EpisodeChatFeedbackSubmitFailedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                trigger = EpisodeChatFeedbackTriggerType.SessionSurvey,
            ),
            eventSink.pollEvent(),
        )
        assertTrue(eventSink.pollEvent() is EpisodeChatFeedbackSubmitTappedEvent)
        assertTrue(eventSink.pollEvent() is EpisodeChatFeedbackSubmittedEvent)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `successful submit tracks the feedback for the rated answer`() = runTest {
        val viewModel = createViewModel(trigger = EpisodeChatFeedbackTriggerType.ResponseRating, messageIndex = 2)
        viewModel.onReasonSelected(ChatFeedback.Reason.WrongFacts)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            listOf(
                EpisodeChatFeedbackFormShownEvent(
                    source = SourceView.PLAYER.analyticsValue,
                    episodeUuid = EPISODE_UUID,
                    podcastUuid = PODCAST_UUID,
                    trigger = EpisodeChatFeedbackTriggerType.ResponseRating,
                    messageIndex = 2,
                ),
                EpisodeChatFeedbackSubmitTappedEvent(
                    source = SourceView.PLAYER.analyticsValue,
                    episodeUuid = EPISODE_UUID,
                    podcastUuid = PODCAST_UUID,
                    trigger = EpisodeChatFeedbackTriggerType.ResponseRating,
                    reason = EpisodeChatFeedbackReasonType.WrongFacts,
                    hasDetails = false,
                    messageIndex = 2,
                ),
                EpisodeChatFeedbackSubmittedEvent(
                    source = SourceView.PLAYER.analyticsValue,
                    episodeUuid = EPISODE_UUID,
                    podcastUuid = PODCAST_UUID,
                    trigger = EpisodeChatFeedbackTriggerType.ResponseRating,
                    reason = EpisodeChatFeedbackReasonType.WrongFacts,
                    hasDetails = false,
                    messageIndex = 2,
                ),
            ),
            List(eventSink.size) { eventSink.pollEvent() },
        )
    }

    @Test
    fun `feedback sent after the form closes is still tracked as submitted`() = runTest {
        val gate = CompletableDeferred<Unit>()
        feedbackManager.gate = gate
        val viewModelStore = ViewModelStore()
        val viewModel = createViewModel()
        viewModelStore.put("feedback", viewModel)
        viewModel.onReasonSelected(ChatFeedback.Reason.WrongFacts)
        viewModel.submit()
        advanceUntilIdle()
        eventSink.skipEvent(eventSink.size)

        viewModelStore.clear()
        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(eventSink.pollEvent() is EpisodeChatFeedbackSubmittedEvent)
    }

    @Test
    fun `restored after a successful submit emits submitted again`() = runTest {
        createViewModel().apply {
            onReasonSelected(ChatFeedback.Reason.WrongFacts)
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
        viewModel.onReasonSelected(ChatFeedback.Reason.Other)

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

    private class TestChatFeedbackManager : ChatFeedbackManager {
        val submitted = mutableListOf<ChatFeedback>()
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun submit(feedback: ChatFeedback) {
            gate?.await()
            failure?.let { throw it }
            submitted += feedback
        }
    }

    private class TestChatManager(private val messages: List<ChatMessage>) : ChatManager {
        override fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> = flowOf(messages)

        override fun observeHistory(): Flow<List<EpisodeChatHistoryItem>> = flowOf(emptyList())

        override suspend fun hasChatWithMessages(episodeUuid: String) = false

        override suspend fun discardMessage(uuid: String) = Unit

        override suspend fun createChat(episodeUuid: String, podcastUuid: String) = Unit

        override suspend fun sendMessage(
            episodeUuid: String,
            podcastUuid: String,
            message: ChatMessage.User,
            allMessages: List<ChatMessage>,
        ) = Unit

        override suspend fun getStarterQuestions(episodeUuid: String, podcastUuid: String) = emptyList<String>()

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
