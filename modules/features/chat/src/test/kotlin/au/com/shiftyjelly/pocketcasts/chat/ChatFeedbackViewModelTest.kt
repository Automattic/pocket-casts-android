package au.com.shiftyjelly.pocketcasts.chat

import androidx.lifecycle.SavedStateHandle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatFeedback
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatFeedbackManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EpisodeChatFeedbackFormDismissedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackFormShownEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackReasonType
import com.automattic.eventhorizon.EpisodeChatFeedbackSubmitTappedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackTriggerType
import com.automattic.eventhorizon.EventHorizon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val feedbackManager = TestChatFeedbackManager()

    private fun createViewModel() = ChatFeedbackViewModel(
        savedStateHandle = savedStateHandle,
        chatManager = TestChatManager(conversation),
        feedbackManager = feedbackManager,
        eventHorizon = EventHorizon(eventSink),
        applicationScope = CoroutineScope(coroutineRule.testDispatcher),
    ).apply {
        onShown(EPISODE_UUID, PODCAST_UUID, SourceView.PLAYER, EpisodeChatFeedbackTriggerType.SessionSurvey)
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
        assertFalse(viewModel.submit())
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

        assertTrue(viewModel.submit())
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

        assertTrue(viewModel.submit())
        assertFalse(viewModel.submit())
        viewModel.onDismissed()
        advanceUntilIdle()

        assertEquals(1, feedbackManager.submitted.size)
        assertTrue(eventSink.pollEvent() is EpisodeChatFeedbackSubmitTappedEvent)
        assertTrue(eventSink.isEmpty())
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

        override suspend fun submit(feedback: ChatFeedback) {
            submitted += feedback
        }
    }

    private class TestChatManager(private val messages: List<ChatMessage>) : ChatManager {
        override fun observeMessages(episodeUuid: String): Flow<List<ChatMessage>> = flowOf(messages)

        override suspend fun createChat(episodeUuid: String, podcastUuid: String) = Unit

        override suspend fun sendMessage(
            episodeUuid: String,
            message: ChatMessage.User,
            allMessages: List<ChatMessage>,
        ) = Unit

        override suspend fun clearMessages(episodeUuid: String) = Unit
    }

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val PODCAST_UUID = "podcast-uuid"
    }
}
