package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Flowable
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalCoroutinesApi::class)
class ManualCleanupViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private lateinit var episodeManager: EpisodeManager
    private lateinit var playbackManager: PlaybackManager
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var viewModel: ManualCleanupViewModel

    private val episode: PodcastEpisode = PodcastEpisode(uuid = "1", publishedDate = Date())
    private val episodes = listOf(episode)
    private val diskSpaceView =
        ManualCleanupViewModel.State.DiskSpaceView(title = LR.string.unplayed, episodes = episodes)

    @Before
    fun setUp() {
        episodeManager = mock()
        playbackManager = mock()
        analyticsTracker = mock()
        whenever(episodeManager.observeDownloadedEpisodes())
            .thenReturn(Flowable.generate { listOf(episodes) })
        viewModel = ManualCleanupViewModel(episodeManager, playbackManager, analyticsTracker)
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
            diskSpaceView = diskSpaceView.copy(episodes = emptyList())
        )

        assertFalse(viewModel.state.value.deleteButton.isEnabled)
    }

    @Test
    fun `given episodes selected, when delete button clicked, then delete action invoked`() {
        whenever(episodeManager.observeDownloadedEpisodes())
            .thenReturn(Flowable.generate { listOf(episode) })
        val deleteButtonClickAction = mock<() -> Unit>()
        viewModel.setup(deleteButtonClickAction)
        viewModel.onDiskSpaceCheckedChanged(isChecked = true, diskSpaceView = diskSpaceView)

        viewModel.onDeleteButtonClicked()

        verify(deleteButtonClickAction).invoke()
    }

    @Test
    fun `given episodes not selected, when delete button clicked, then episodes are not deleted`() {
        whenever(episodeManager.observeDownloadedEpisodes())
            .thenReturn(Flowable.generate { listOf(episode) })
        viewModel.onDiskSpaceCheckedChanged(isChecked = false, diskSpaceView = diskSpaceView)

        viewModel.onDeleteButtonClicked()

        verifyBlocking(episodeManager, never()) {
            deleteEpisodeFiles(episodes, playbackManager)
        }
    }
}
