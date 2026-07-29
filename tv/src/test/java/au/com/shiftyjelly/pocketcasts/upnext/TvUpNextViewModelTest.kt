package au.com.shiftyjelly.pocketcasts.upnext

import android.content.Context
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.subjects.BehaviorSubject
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TvUpNextViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val changes = BehaviorSubject.create<UpNextQueue.State>()
    private val upNextQueue = mock<UpNextQueue> {
        on { changesObservable } doReturn changes
    }
    private val syncManager = mock<SyncManager>()
    private val context = mock<Context>()

    private val currentEpisode = episode("current")

    @Test
    fun `an empty queue maps to the empty state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvUpNextUiState.Loading, awaitItem())

            changes.onNext(UpNextQueue.State.Empty)

            assertEquals(TvUpNextUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `a loaded queue exposes the queued podcast episodes`() = runTest {
        val viewModel = createViewModel()
        val first = episode("first")
        val second = episode("second")

        viewModel.uiState.test {
            assertEquals(TvUpNextUiState.Loading, awaitItem())

            changes.onNext(UpNextQueue.State.Loaded(currentEpisode, null, listOf(first, second)))

            assertEquals(TvUpNextUiState.Loaded(listOf(first, second)), awaitItem())
        }
    }

    @Test
    fun `user episodes are excluded from the queue`() = runTest {
        val viewModel = createViewModel()
        val podcastEpisode = episode("podcast")
        val userEpisode = UserEpisode(uuid = "user", publishedDate = Date(0))

        viewModel.uiState.test {
            assertEquals(TvUpNextUiState.Loading, awaitItem())

            changes.onNext(UpNextQueue.State.Loaded(currentEpisode, null, listOf(podcastEpisode, userEpisode)))

            assertEquals(TvUpNextUiState.Loaded(listOf(podcastEpisode)), awaitItem())
        }
    }

    @Test
    fun `a queue of only user episodes maps to the empty state`() = runTest {
        val viewModel = createViewModel()
        val userEpisode = UserEpisode(uuid = "user", publishedDate = Date(0))

        viewModel.uiState.test {
            assertEquals(TvUpNextUiState.Loading, awaitItem())

            changes.onNext(UpNextQueue.State.Loaded(currentEpisode, null, listOf(userEpisode)))

            assertEquals(TvUpNextUiState.Empty, awaitItem())
        }
    }

    private fun createViewModel() = TvUpNextViewModel(
        context = context,
        syncManager = syncManager,
        upNextQueue = upNextQueue,
    )

    private fun episode(uuid: String) = PodcastEpisode(uuid = uuid, publishedDate = Date(0))
}
