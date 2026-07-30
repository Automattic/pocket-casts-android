package au.com.shiftyjelly.pocketcasts.podcasts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Single
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class TvPodcastDetailsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val availableEpisode = episode(uuid = "episode-1", isArchived = false)
    private val archivedEpisode = episode(uuid = "episode-2", isArchived = true)

    private val episodes = MutableSharedFlow<List<PodcastEpisode>>(replay = 1)

    @Test
    fun `archived episodes are hidden`() = runTest {
        val podcast = podcast(showArchived = false)
        val viewModel = createViewModel(
            podcastManager = mock {
                on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.just(podcast)
                on { podcastByUuidFlow(any()) } doReturn MutableStateFlow(podcast)
            },
            episodeManager = mock { on { findEpisodesByPodcastOrderedFlow(any()) } doReturn episodes },
        )

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())

            episodes.emit(listOf(availableEpisode, archivedEpisode))

            val state = awaitItem() as TvPodcastDetailsUiState.Loaded
            assertEquals(podcast, state.podcast)
            assertEquals(listOf(availableEpisode), state.episodes)
            assertEquals(1, state.archivedEpisodeCount)
        }
    }

    @Test
    fun `a podcast with only archived episodes reports the archived count`() = runTest {
        val podcast = podcast(showArchived = false)
        val viewModel = createViewModel(
            podcastManager = mock {
                on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.just(podcast)
                on { podcastByUuidFlow(any()) } doReturn MutableStateFlow(podcast)
            },
            episodeManager = mock { on { findEpisodesByPodcastOrderedFlow(any()) } doReturn episodes },
        )

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())

            episodes.emit(listOf(archivedEpisode))

            val state = awaitItem() as TvPodcastDetailsUiState.Loaded
            assertEquals(emptyList<PodcastEpisode>(), state.episodes)
            assertEquals(1, state.archivedEpisodeCount)
        }
    }

    @Test
    fun `archived episodes are shown once the filter is toggled`() = runTest {
        val podcastFlow = MutableStateFlow(podcast(showArchived = false))
        val viewModel = createViewModel(
            podcastManager = mock {
                on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.just(podcastFlow.value)
                on { podcastByUuidFlow(any()) } doReturn podcastFlow
                on { updateShowArchived(any(), any()) } doAnswer { invocation ->
                    podcastFlow.value = podcast(showArchived = invocation.getArgument(1))
                }
            },
            episodeManager = mock { on { findEpisodesByPodcastOrderedFlow(any()) } doReturn episodes },
        )

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())

            episodes.emit(listOf(availableEpisode, archivedEpisode))
            assertEquals(listOf(availableEpisode), (awaitItem() as TvPodcastDetailsUiState.Loaded).episodes)

            viewModel.toggleArchiveFilter()

            assertEquals(
                listOf(availableEpisode, archivedEpisode),
                (awaitItem() as TvPodcastDetailsUiState.Loaded).episodes,
            )
        }
    }

    @Test
    fun `changing the sort type persists it to the podcast`() = runTest {
        val podcast = podcast(showArchived = false)
        val podcastManager = mock<PodcastManager> {
            on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.just(podcast)
            on { podcastByUuidFlow(any()) } doReturn MutableStateFlow(podcast)
        }
        val viewModel = createViewModel(
            podcastManager = podcastManager,
            episodeManager = mock { on { findEpisodesByPodcastOrderedFlow(any()) } doReturn episodes },
        )

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())
            episodes.emit(listOf(availableEpisode))
            awaitItem() as TvPodcastDetailsUiState.Loaded
        }

        viewModel.changeSortType(EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC)

        verify(podcastManager).updateEpisodesSortTypeBlocking(podcast, EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC)
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

    private fun podcast(showArchived: Boolean) = Podcast(
        uuid = "podcast-uuid",
        title = "Podcast",
        showArchived = showArchived,
    )

    private fun episode(uuid: String, isArchived: Boolean) = PodcastEpisode(
        uuid = uuid,
        publishedDate = Date(0),
        isArchived = isArchived,
    )
}
