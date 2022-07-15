package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verifyBlocking
import com.nhaarman.mockitokotlin2.whenever
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Flowable
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import java.util.Date

class ManualCleanupViewModelTest {
    private val episodeManager: EpisodeManager = mock()
    private val playbackManager: PlaybackManager = mock()
    private val episode: Episode = Episode(uuid = "1", publishedDate = Date())

    @ApplicationContext
    private val context: Context = mock()

    lateinit var viewModel: ManualCleanupViewModel
    private val episodes = listOf(episode)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(context.getString(anyInt())).thenReturn("")
        whenever(context.getString(anyInt(), anyOrNull())).thenReturn("")
        whenever(episodeManager.observeDownloadedEpisodes()).thenReturn(Flowable.generate { emptyList<Episode>() })
        viewModel = ManualCleanupViewModel(episodeManager, playbackManager, context)
    }

    @Test
    fun `given episodes not present, when ui is built, then delete button is disabled`() {
        whenever(episodeManager.observeDownloadedEpisodes()).thenReturn(Flowable.generate { emptyList<Episode>() })

        assertFalse(viewModel.state.value.deleteButton.isEnabled)
    }

    @Test
    fun `given episodes present, when ui is built, then delete button is enabled`() {
        whenever(episodeManager.observeDownloadedEpisodes())
            .thenReturn(Flowable.generate { listOf(episode) })

        assertFalse(viewModel.state.value.deleteButton.isEnabled)
    }

    @Test
    fun `given episodes selected, when delete button clicked, then episodes are deleted`() {
        whenever(episodeManager.observeDownloadedEpisodes())
            .thenReturn(Flowable.generate { listOf(episode) })
        viewModel.onDiskSpaceCheckedChanged(isChecked = true, episodes = episodes)

        viewModel.onDeleteButtonClicked()

        verifyBlocking(episodeManager, times(1)) {
            deleteEpisodeFiles(episodes, playbackManager)
        }
    }

    @Test
    fun `given episodes not selected, when delete button clicked, then episodes are not deleted`() {
        whenever(episodeManager.observeDownloadedEpisodes())
            .thenReturn(Flowable.generate { listOf(episode) })
        viewModel.onDiskSpaceCheckedChanged(isChecked = false, episodes = episodes)

        viewModel.onDeleteButtonClicked()

        verifyBlocking(episodeManager, never()) {
            deleteEpisodeFiles(episodes, playbackManager)
        }
    }
}
