package au.com.shiftyjelly.pocketcasts.onboarding

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.onboarding.signin.SyncCompletionWaiter
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSyncingViewModel
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvSyncingViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val podcastsFlow = MutableSharedFlow<List<Podcast>>()
    private val podcastManager = mock<PodcastManager> {
        whenever(it.findSubscribedFlow()).thenReturn(podcastsFlow)
    }
    private val syncCompletion = CompletableDeferred<Unit>()
    private val syncCompletionWaiter = SyncCompletionWaiter { syncCompletion.await() }

    @Test
    fun `initial state has no podcasts and sync not complete`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(emptyList<String>(), state.podcastUuids)
            assertFalse(state.syncComplete)
        }
    }

    @Test
    fun `triggers refreshPodcastsAfterSignIn on creation`() = runTest {
        createViewModel()

        verify(podcastManager).refreshPodcastsAfterSignIn()
    }

    @Test
    fun `podcast uuids update when podcasts sync`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            podcastsFlow.emit(listOf(podcast("uuid-1"), podcast("uuid-2")))
            assertEquals(listOf("uuid-1", "uuid-2"), awaitItem().podcastUuids)

            podcastsFlow.emit(listOf(podcast("uuid-1"), podcast("uuid-2"), podcast("uuid-3")))
            assertEquals(listOf("uuid-1", "uuid-2", "uuid-3"), awaitItem().podcastUuids)
        }
    }

    @Test
    fun `waits for both sync and minimum display time`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertFalse(awaitItem().syncComplete)

            syncCompletion.complete(Unit)
            advanceTimeBy(TvSyncingViewModel.MIN_DISPLAY_TIME_MS - 1)
            expectNoEvents()

            advanceTimeBy(1)
            assertTrue(awaitItem().syncComplete)
        }
    }

    @Test
    fun `waits for sync even after minimum display time elapsed`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertFalse(awaitItem().syncComplete)

            advanceTimeBy(TvSyncingViewModel.MIN_DISPLAY_TIME_MS)
            expectNoEvents()

            syncCompletion.complete(Unit)
            assertTrue(awaitItem().syncComplete)
        }
    }

    @Test
    fun `waits for slow sync that exceeds minimum display time`() = runTest {
        val slowSyncDuration = TvSyncingViewModel.MIN_DISPLAY_TIME_MS * 3
        val slowWaiter = SyncCompletionWaiter { delay(slowSyncDuration) }
        val viewModel = createViewModel(syncWaiter = slowWaiter)

        viewModel.uiState.test {
            assertFalse(awaitItem().syncComplete)

            advanceTimeBy(TvSyncingViewModel.MIN_DISPLAY_TIME_MS)
            expectNoEvents()

            advanceTimeBy(slowSyncDuration - TvSyncingViewModel.MIN_DISPLAY_TIME_MS)
            assertTrue(awaitItem().syncComplete)
        }
    }

    @Test
    fun `completes on sync failure after minimum display time`() = runTest {
        whenever(podcastManager.refreshPodcastsAfterSignIn()).thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            advanceTimeBy(TvSyncingViewModel.MIN_DISPLAY_TIME_MS)
            assertTrue(awaitItem().syncComplete)
        }
    }

    @Test
    fun `completes on waiter failure after minimum display time`() = runTest {
        val failingWaiter = SyncCompletionWaiter { throw RuntimeException("Sync crashed") }
        val viewModel = createViewModel(syncWaiter = failingWaiter)

        viewModel.uiState.test {
            awaitItem()
            advanceTimeBy(TvSyncingViewModel.MIN_DISPLAY_TIME_MS)
            assertTrue(awaitItem().syncComplete)
        }
    }

    @Test
    fun `not complete before minimum display time even if sync finished`() = runTest {
        val instantWaiter = SyncCompletionWaiter { }
        val viewModel = createViewModel(syncWaiter = instantWaiter)

        viewModel.uiState.test {
            assertFalse(awaitItem().syncComplete)
            advanceTimeBy(TvSyncingViewModel.MIN_DISPLAY_TIME_MS - 1)
            expectNoEvents()
        }
    }

    private fun createViewModel(
        syncWaiter: SyncCompletionWaiter = syncCompletionWaiter,
    ) = TvSyncingViewModel(podcastManager, syncWaiter)

    private fun podcast(uuid: String) = Podcast(uuid = uuid)
}
