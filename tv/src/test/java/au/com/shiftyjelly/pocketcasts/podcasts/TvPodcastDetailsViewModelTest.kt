package au.com.shiftyjelly.pocketcasts.podcasts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Single
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TvPodcastDetailsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val podcast = Podcast(uuid = "podcast-uuid", title = "Podcast")
    private val availableEpisode = episode(uuid = "episode-1", isArchived = false)
    private val archivedEpisode = episode(uuid = "episode-2", isArchived = true)

    private val episodes = MutableSharedFlow<List<PodcastEpisode>>(replay = 1)

    @Test
    fun `archived episodes are hidden`() = runTest {
        val viewModel = createViewModel(
            podcastManager = mock { on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.just(podcast) },
            episodeManager = mock { on { findEpisodesByPodcastOrderedFlow(any()) } doReturn episodes },
        )

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())

            episodes.emit(listOf(availableEpisode, archivedEpisode))

            val state = awaitItem() as TvPodcastDetailsUiState.Loaded
            assertEquals(podcast, state.podcast)
            assertEquals(listOf(availableEpisode), state.episodes)
        }
    }

    @Test
    fun `a podcast that cannot be resolved maps to the not found state`() = runTest {
        val viewModel = createViewModel(
            podcastManager = mock {
                on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.error(RuntimeException("boom"))
            },
            episodeManager = mock(),
        )

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.NotFound, expectMostRecentItem())
        }
    }

    private fun createViewModel(
        podcastManager: PodcastManager,
        episodeManager: EpisodeManager,
    ) = TvPodcastDetailsViewModel(
        podcastUuid = "podcast-uuid",
        podcastManager = podcastManager,
        episodeManager = episodeManager,
        defaultDispatcher = coroutineRule.testDispatcher,
    )

    private fun episode(uuid: String, isArchived: Boolean) = PodcastEpisode(
        uuid = uuid,
        publishedDate = Date(0),
        isArchived = isArchived,
    )
}
