package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import io.reactivex.Flowable
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class ManualCleanupViewModelTest {
    private lateinit var episodeManager: EpisodeManager
    private lateinit var playbackManager: PlaybackManager
    private lateinit var viewModel: ManualCleanupViewModel

    private val episode: Episode = Episode(uuid = "1", publishedDate = Date())
    private val episodes = listOf(episode)
    private val diskSpaceView =
        ManualCleanupViewModel.State.DiskSpaceView(title = LR.string.unplayed, episodes = episodes)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        episodeManager = mock()
        playbackManager = mock()
        whenever(episodeManager.observeDownloadedEpisodes())
            .thenReturn(Flowable.generate { listOf(episodes) })
        viewModel = ManualCleanupViewModel(episodeManager, playbackManager)
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
    fun `given episodes selected, when delete button clicked, then episodes are deleted`() {
        whenever(episodeManager.observeDownloadedEpisodes())
            .thenReturn(Flowable.generate { listOf(episode) })
        viewModel.onDiskSpaceCheckedChanged(isChecked = true, diskSpaceView = diskSpaceView)

        viewModel.onDeleteButtonClicked()

        verifyBlocking(episodeManager, times(1)) {
            deleteEpisodeFiles(episodes, playbackManager)
        }
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
