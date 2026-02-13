package au.com.shiftyjelly.pocketcasts.wear.ui.podcast

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner.Silent::class)
class PodcastViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule(StandardTestDispatcher())

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var theme: Theme

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var artworkConfigurationSetting: UserSetting<ArtworkConfiguration>

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: PodcastViewModel

    private val testPodcastUuid = "test-podcast-uuid"

    @Before
    fun setup() {
        savedStateHandle = SavedStateHandle(mapOf(PodcastScreen.ARGUMENT to testPodcastUuid))

        val artworkFlow = MutableStateFlow(ArtworkConfiguration(useEpisodeArtwork = false))
        whenever(artworkConfigurationSetting.flow).thenReturn(artworkFlow)
        whenever(settings.artworkConfiguration).thenReturn(artworkConfigurationSetting)
    }

    @Test
    fun `when podcast not found, then emits Empty state`() = runTest(coroutineRule.testDispatcher) {
        whenever(podcastManager.findPodcastByUuid(testPodcastUuid)).thenReturn(null)

        createViewModel().uiState.test {
            val state = awaitItem()
            assertTrue(state is PodcastViewModel.UiState.Empty)
        }
    }

    @Test
    fun `when podcast found with episodes, then emits Loaded state with episodes`() = runTest(coroutineRule.testDispatcher) {
        val podcast = createTestPodcast(uuid = testPodcastUuid)
        val episodes = listOf(
            createTestEpisode(uuid = "ep1", title = "Episode 1"),
            createTestEpisode(uuid = "ep2", title = "Episode 2"),
        )

        whenever(podcastManager.findPodcastByUuid(testPodcastUuid)).thenReturn(podcast)
        whenever(episodeManager.findEpisodesByPodcastOrderedFlow(podcast))
            .thenReturn(flowOf(episodes))

        createViewModel().uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertTrue(state is PodcastViewModel.UiState.Loaded)
            val loadedState = state as PodcastViewModel.UiState.Loaded
            assertEquals(podcast, loadedState.podcast)
            assertEquals(episodes, loadedState.episodes)
            assertEquals(theme, loadedState.theme)
        }
    }

    @Test
    fun `when episodes are archived, then filter them out`() = runTest(coroutineRule.testDispatcher) {
        val podcast = createTestPodcast(uuid = testPodcastUuid)
        val episodes = listOf(
            createTestEpisode(uuid = "ep1", title = "Episode 1", isArchived = false),
            createTestEpisode(uuid = "ep2", title = "Episode 2", isArchived = true),
            createTestEpisode(uuid = "ep3", title = "Episode 3", isArchived = false),
        )

        whenever(podcastManager.findPodcastByUuid(testPodcastUuid)).thenReturn(podcast)
        whenever(episodeManager.findEpisodesByPodcastOrderedFlow(podcast))
            .thenReturn(flowOf(episodes))

        createViewModel().uiState.test {
            skipItems(1)
            val state = awaitItem() as PodcastViewModel.UiState.Loaded
            assertEquals(2, state.episodes.size)
            assertEquals("ep1", state.episodes[0].uuid)
            assertEquals("ep3", state.episodes[1].uuid)
        }
    }

    @Test
    fun `when episodes are finished, then filter them out`() = runTest(coroutineRule.testDispatcher) {
        val podcast = createTestPodcast(uuid = testPodcastUuid)
        val episodes = listOf(
            createTestEpisode(uuid = "ep1", title = "Episode 1", playingStatus = EpisodePlayingStatus.NOT_PLAYED),
            createTestEpisode(uuid = "ep2", title = "Episode 2", playingStatus = EpisodePlayingStatus.COMPLETED),
            createTestEpisode(uuid = "ep3", title = "Episode 3", playingStatus = EpisodePlayingStatus.IN_PROGRESS),
        )

        whenever(podcastManager.findPodcastByUuid(testPodcastUuid)).thenReturn(podcast)
        whenever(episodeManager.findEpisodesByPodcastOrderedFlow(podcast))
            .thenReturn(flowOf(episodes))

        createViewModel().uiState.test {
            skipItems(1)
            val state = awaitItem() as PodcastViewModel.UiState.Loaded
            assertEquals(2, state.episodes.size)
            assertEquals("ep1", state.episodes[0].uuid)
            assertEquals("ep3", state.episodes[1].uuid)
        }
    }

    private fun createViewModel(): PodcastViewModel {
        return PodcastViewModel(
            savedStateHandle = savedStateHandle,
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            theme = theme,
            settings = settings,
        )
    }

    private fun createTestPodcast(
        uuid: String,
        title: String = "Test Podcast",
    ): Podcast {
        return Podcast(
            uuid = uuid,
            title = title,
            podcastUrl = "https://example.com/podcast",
            podcastDescription = "Test Description",
            podcastCategory = "Technology",
            podcastLanguage = "en",
            mediaType = "audio",
            isSubscribed = true,
            grouping = PodcastGrouping.None,
        )
    }

    private fun createTestEpisode(
        uuid: String,
        title: String,
        isArchived: Boolean = false,
        playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED,
        duration: Double = 1800.0,
    ): PodcastEpisode {
        return PodcastEpisode(
            uuid = uuid,
            podcastUuid = testPodcastUuid,
            publishedDate = Date(),
            title = title,
            episodeDescription = "Test Episode Description",
            duration = duration,
            isArchived = isArchived,
            playingStatus = playingStatus,
        )
    }
}
