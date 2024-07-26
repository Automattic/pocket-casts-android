package au.com.shiftyjelly.pocketcasts.sharing.podcast

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.sharing.podcast.SharePodcastViewModel.UiState
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

    private val podcastManager = mock<PodcastManager>()

    private val podcast = Podcast(uuid = "podcast-id", title = "Podcast Title")

    private lateinit var viewModel: SharePodcastViewModel

    @Before
    fun setUp() {
        whenever(podcastManager.observePodcastByUuidFlow("podcast-id")).thenReturn(flowOf(podcast))
        whenever(podcastManager.observeEpisodeCountByPodcatUuid("podcast-id")).thenReturn(flowOf(50))

        viewModel = SharePodcastViewModel(podcast.uuid, podcastManager)
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
}
