package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalCoroutinesApi::class)
class ManualCleanupViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private lateinit var episodeManager: EpisodeManager
    private lateinit var viewModel: ManualCleanupViewModel

    private val episode: PodcastEpisode = PodcastEpisode(uuid = "1", publishedDate = Date())
    private val episodes = listOf(episode)
    private val diskSpaceView =
        ManualCleanupViewModel.State.DiskSpaceView(title = LR.string.unplayed, episodes = episodes)

    @Before
    fun setUp() {
        episodeManager = mock()
        whenever(episodeManager.findDownloadedEpisodesFlow()).thenReturn(emptyFlow())
        viewModel = ManualCleanupViewModel(episodeManager, mock(), EventHorizon(TestEventSink()))
    }

    @Test
    fun `given episodes present, when disk space size checked, then delete button is enabled`() {
        viewModel.onDiskSpaceCheckedChanged(isChecked = true, diskSpaceView = diskSpaceView)

        assertTrue(viewModel.state.value.deleteButton.isEnabled)
    }

    @Test
    fun `given episodes present, when disk space size unchecked, then delete button is disabled`() {
        viewModel.onDiskSpaceCheckedChanged(isChecked = false, diskSpaceView = diskSpaceView)

        assertFalse(viewModel.state.value.deleteButton.isEnabled)
    }

    @Test
    fun `given episodes not present, when disk space size checked, then delete button is disabled`() {
        viewModel.onDiskSpaceCheckedChanged(
            isChecked = true,
            diskSpaceView = diskSpaceView.copy(episodes = emptyList()),
        )

        assertFalse(viewModel.state.value.deleteButton.isEnabled)
    }

    @Test
    fun `given episodes selected, when delete button clicked, then delete action invoked`() {
        val deleteButtonClickAction = mock<() -> Unit>()
        viewModel.setup(deleteButtonClickAction)
        viewModel.onDiskSpaceCheckedChanged(isChecked = true, diskSpaceView = diskSpaceView)

        viewModel.onDeleteButtonClicked()

        verify(deleteButtonClickAction).invoke()
    }

    @Test
    fun `given downloaded episodes, when starred switch toggled, then starred episodes are included`() {
        val starredEpisode = PodcastEpisode(uuid = "2", publishedDate = Date(), isStarred = true)
        whenever(episodeManager.findDownloadedEpisodesFlow()).thenReturn(flowOf(listOf(episode, starredEpisode)))
        viewModel = ManualCleanupViewModel(episodeManager, mock(), EventHorizon(TestEventSink()))

        assertEquals(listOf(episode), viewModel.state.value.unplayed?.episodes)

        viewModel.onStarredSwitchClicked(true)

        assertEquals(listOf(episode, starredEpisode), viewModel.state.value.unplayed?.episodes)
    }
}
