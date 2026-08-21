package au.com.shiftyjelly.pocketcasts.upnext

import android.content.Context
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.UpNextDiscoverButtonTappedEvent
import com.automattic.eventhorizon.UpNextShownEvent
import com.automattic.eventhorizon.UpNextSourceType
import io.reactivex.subjects.BehaviorSubject
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking

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
    private val playbackManager = mock<PlaybackManager>()
    private val eventHorizon = mock<EventHorizon>()

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

    @Test
    fun `the current episode is excluded from the exposed queue`() = runTest {
        val viewModel = createViewModel()
        val queued = episode("queued")

        viewModel.uiState.test {
            assertEquals(TvUpNextUiState.Loading, awaitItem())

            changes.onNext(UpNextQueue.State.Loaded(currentEpisode, null, listOf(queued)))

            val loaded = awaitItem() as TvUpNextUiState.Loaded
            assertFalse(loaded.episodes.contains(currentEpisode))
            assertEquals(listOf(queued), loaded.episodes)
        }
    }

    @Test
    fun `a queue that empties maps back to the empty state`() = runTest {
        val viewModel = createViewModel()
        val queued = episode("queued")

        viewModel.uiState.test {
            assertEquals(TvUpNextUiState.Loading, awaitItem())

            changes.onNext(UpNextQueue.State.Loaded(currentEpisode, null, listOf(queued)))
            assertEquals(TvUpNextUiState.Loaded(listOf(queued)), awaitItem())

            changes.onNext(UpNextQueue.State.Empty)
            assertEquals(TvUpNextUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `play starts playback of the episode`() = runTest {
        val episode = episode("episode")

        createViewModel().play(episode)

        verifyBlocking(playbackManager) { playNowSuspend(episode = episode, sourceView = SourceView.UP_NEXT) }
    }

    @Test
    fun `showing the screen tracks the up next shown event`() = runTest {
        createViewModel().trackUpNextShown()

        verify(eventHorizon).track(UpNextShownEvent(source = UpNextSourceType.TabBar))
    }

    @Test
    fun `tapping the empty state discover button tracks the discover button event`() = runTest {
        createViewModel().trackDiscoverButtonTapped()

        verify(eventHorizon).track(UpNextDiscoverButtonTappedEvent(source = UpNextSourceType.TabBar))
    }

    private fun createViewModel() = TvUpNextViewModel(
        context = context,
        syncManager = syncManager,
        upNextQueue = upNextQueue,
        playbackManager = playbackManager,
        eventHorizon = eventHorizon,
    )

    private fun episode(uuid: String) = PodcastEpisode(uuid = uuid, publishedDate = Date(0))
}
