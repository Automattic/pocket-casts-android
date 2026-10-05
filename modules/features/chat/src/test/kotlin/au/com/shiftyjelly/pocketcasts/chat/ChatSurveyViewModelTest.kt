package au.com.shiftyjelly.pocketcasts.chat

import androidx.lifecycle.SavedStateHandle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import com.automattic.eventhorizon.EpisodeChatSentimentType
import com.automattic.eventhorizon.EpisodeChatSurveyDismissedEvent
import com.automattic.eventhorizon.EpisodeChatSurveyResponseSubmittedEvent
import com.automattic.eventhorizon.EpisodeChatSurveyShownEvent
import com.automattic.eventhorizon.EventHorizon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSurveyViewModelTest {
    private val eventSink = TestEventSink()
    private val savedStateHandle = SavedStateHandle()

    private fun createViewModel() = ChatSurveyViewModel(
        savedStateHandle = savedStateHandle,
        eventHorizon = EventHorizon(eventSink),
    ).apply {
        onShown(EPISODE_UUID, PODCAST_UUID, SourceView.PLAYER)
    }

    @Test
    fun `tracks shown once across recreation`() {
        createViewModel()
        createViewModel()

        assertEquals(
            EpisodeChatSurveyShownEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `tracks positive answer and no dismissal`() {
        val viewModel = createViewModel()
        eventSink.skipEvent()

        viewModel.onAnswer(isPositive = true)
        viewModel.onDismissed()

        assertEquals(
            EpisodeChatSurveyResponseSubmittedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
                response = EpisodeChatSentimentType.Positive,
            ),
            eventSink.pollEvent(),
        )
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `tracks negative answer only once`() {
        val viewModel = createViewModel()
        eventSink.skipEvent()

        viewModel.onAnswer(isPositive = false)
        viewModel.onAnswer(isPositive = true)

        assertEquals(
            EpisodeChatSentimentType.Negative,
            (eventSink.pollEvent() as EpisodeChatSurveyResponseSubmittedEvent).response,
        )
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `tracks dismissal without an answer`() {
        val viewModel = createViewModel()
        eventSink.skipEvent()

        viewModel.onDismissed()

        assertEquals(
            EpisodeChatSurveyDismissedEvent(
                source = SourceView.PLAYER.analyticsValue,
                episodeUuid = EPISODE_UUID,
                podcastUuid = PODCAST_UUID,
            ),
            eventSink.pollEvent(),
        )
    }

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val PODCAST_UUID = "podcast-uuid"
    }
}
