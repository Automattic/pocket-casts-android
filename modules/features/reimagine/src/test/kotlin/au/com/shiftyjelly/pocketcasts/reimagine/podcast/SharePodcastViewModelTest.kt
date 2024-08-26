package au.com.shiftyjelly.pocketcasts.reimagine.podcast

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.reimagine.FakeTracker
import au.com.shiftyjelly.pocketcasts.reimagine.TrackEvent
import au.com.shiftyjelly.pocketcasts.reimagine.podcast.SharePodcastViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SharePodcastViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val tracker = FakeTracker()
    private val podcastManager = mock<PodcastManager>()

    private val podcast = Podcast(uuid = "podcast-id", title = "Podcast Title")

    private lateinit var viewModel: SharePodcastViewModel

    @Before
    fun setUp() {
        whenever(podcastManager.observePodcastByUuidFlow("podcast-id")).thenReturn(flowOf(podcast))
        whenever(podcastManager.observeEpisodeCountByPodcatUuid("podcast-id")).thenReturn(flowOf(50))

        viewModel = SharePodcastViewModel(
            podcast.uuid,
            SourceView.PLAYER,
            podcastManager,
            AnalyticsTracker.test(tracker, isEnabled = true),
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

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.SHARE_SCREEN_SHOWN,
                mapOf(
                    "type" to "podcast",
                    "podcast_uuid" to "podcast-id",
                    "source" to "player",
                ),
            ),
            event,
        )
    }
}
