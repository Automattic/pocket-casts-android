package au.com.shiftyjelly.pocketcasts.reimagine.podcast

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.reimagine.podcast.SharePodcastViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.ShareActionMediaType
import com.automattic.eventhorizon.ShareScreenShownEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.automattic.eventhorizon.SourceView as EventHorizonSourceView

@ExperimentalCoroutinesApi
class SharePodcastViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val eventSink = TestEventSink()
    private val podcastManager = mock<PodcastManager>()

    private val podcast = Podcast(uuid = "podcast-id", title = "Podcast Title")

    private lateinit var viewModel: SharePodcastViewModel

    @Before
    fun setUp() {
        whenever(podcastManager.podcastByUuidFlow("podcast-id")).thenReturn(flowOf(podcast))
        whenever(podcastManager.episodeCountByPodcatUuidFlow("podcast-id")).thenReturn(flowOf(50))

        viewModel = SharePodcastViewModel(
            podcast.uuid,
            SourceView.PLAYER,
            podcastManager,
            EventHorizon(eventSink),
        )
    }

    @Test
    fun `get UI state`() = runTest {
        viewModel.uiState.test {
            assertEquals(
                UiState(
                    podcast = podcast,
                    episodeCount = 50,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `track screen show event`() = runTest {
        viewModel.onScreenShown()

        val event = eventSink.pollEvent()

        assertEquals(
            ShareScreenShownEvent(
                type = ShareActionMediaType.Podcast,
                podcastUuid = "podcast-id",
                source = EventHorizonSourceView.Player,
            ),
            event,
        )
    }
}
