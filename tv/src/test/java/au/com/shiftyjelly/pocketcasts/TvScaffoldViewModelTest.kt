package au.com.shiftyjelly.pocketcasts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.auth.TvSignOutManager
import au.com.shiftyjelly.pocketcasts.home.TvProfileState
import au.com.shiftyjelly.pocketcasts.home.TvScaffoldViewModel
import au.com.shiftyjelly.pocketcasts.home.TvTab
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.subjects.BehaviorSubject
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvScaffoldViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val isLoggedIn = BehaviorRelay.createDefault(false)
    private val email = MutableStateFlow<String?>(null)

    private val syncManager = mock<SyncManager> {
        on { isLoggedIn() } doReturn false
        on { isLoggedInObservable } doReturn isLoggedIn
        on { emailFlow() } doReturn email
    }
    private val signOutManager = mock<TvSignOutManager>()

    private val queueChanges = BehaviorSubject.create<UpNextQueue.State>()
    private val upNextQueue = mock<UpNextQueue> {
        on { changesObservable } doReturn queueChanges
    }

    private val loadedQueue = UpNextQueue.State.Loaded(
        episode = PodcastEpisode(uuid = "episode", publishedDate = Date(0)),
        podcast = null,
        queue = emptyList(),
    )

    private val launchRequests = TvLaunchRequests()

    private val viewModel by lazy {
        TvScaffoldViewModel(syncManager, signOutManager, launchRequests, upNextQueue)
    }

    @Test
    fun `initial state has the default tabs with home selected`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(TvTab.entries, state.tabs)
            assertEquals(TvTab.Home, state.selectedTab)
            assertEquals(0, state.selectedTabIndex)
        }
    }

    @Test
    fun `selectTab updates the selected tab`() = runTest {
        viewModel.uiState.test {
            assertEquals(TvTab.Home, awaitItem().selectedTab)

            viewModel.selectTab(TvTab.Playlists)
            val state = awaitItem()
            assertEquals(TvTab.Playlists, state.selectedTab)
            assertEquals(2, state.selectedTabIndex)
        }
    }

    @Test
    fun `selectTab with the same tab does not emit new state`() = runTest {
        viewModel.uiState.test {
            assertEquals(TvTab.Home, awaitItem().selectedTab)

            viewModel.selectTab(TvTab.Home)
            expectNoEvents()
        }
    }

    @Test
    fun `now playing tab appears between up next and search when the queue loads`() = runTest {
        viewModel.uiState.test {
            assertEquals(TvTab.entries, awaitItem().tabs)

            queueChanges.onNext(loadedQueue)

            assertEquals(
                listOf(TvTab.Home, TvTab.YourPodcasts, TvTab.Playlists, TvTab.UpNext, TvTab.NowPlaying, TvTab.Search),
                awaitItem().tabs,
            )
        }
    }

    @Test
    fun `now playing tab hides when the queue empties`() = runTest {
        queueChanges.onNext(loadedQueue)

        viewModel.uiState.test {
            assertEquals(true, awaitItem().tabs.contains(TvTab.NowPlaying))

            queueChanges.onNext(UpNextQueue.State.Empty)

            assertEquals(TvTab.entries, awaitItem().tabs)
        }
    }

    @Test
    fun `openNowPlaying selects the now playing tab immediately`() = runTest {
        viewModel.uiState.test {
            assertEquals(TvTab.Home, awaitItem().selectedTab)

            viewModel.openNowPlaying()

            val state = expectMostRecentItem()
            assertEquals(TvTab.NowPlaying, state.selectedTab)
            assertEquals(true, state.tabs.contains(TvTab.NowPlaying))
        }
    }

    @Test
    fun `openNowPlaying keeps the tab selected while the queue loads`() = runTest {
        viewModel.uiState.test {
            assertEquals(TvTab.Home, awaitItem().selectedTab)

            viewModel.openNowPlaying()
            queueChanges.onNext(loadedQueue)

            val state = expectMostRecentItem()
            assertEquals(TvTab.NowPlaying, state.selectedTab)
            assertEquals(4, state.selectedTabIndex)
        }
    }

    @Test
    fun `redirects to home when the queue empties while on now playing`() = runTest {
        queueChanges.onNext(loadedQueue)

        viewModel.uiState.test {
            awaitItem()

            viewModel.selectTab(TvTab.NowPlaying)
            assertEquals(TvTab.NowPlaying, awaitItem().selectedTab)

            queueChanges.onNext(UpNextQueue.State.Empty)
            coroutineRule.testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(TvTab.Home, state.selectedTab)
            assertEquals(TvTab.entries, state.tabs)
        }
    }

    @Test
    fun `stays on now playing when the queue empties only transiently while switching episodes`() = runTest {
        queueChanges.onNext(loadedQueue)

        viewModel.uiState.test {
            assertEquals(TvTab.Home, awaitItem().selectedTab)

            viewModel.selectTab(TvTab.NowPlaying)
            queueChanges.onNext(UpNextQueue.State.Empty)
            queueChanges.onNext(loadedQueue)
            coroutineRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(TvTab.NowPlaying, expectMostRecentItem().selectedTab)
        }
    }

    @Test
    fun `keeps now playing selected when the queue reports later than the debounce window`() = runTest {
        viewModel.uiState.test {
            assertEquals(TvTab.Home, awaitItem().selectedTab)

            viewModel.openNowPlaying()
            coroutineRule.testDispatcher.scheduler.advanceUntilIdle()
            queueChanges.onNext(loadedQueue)
            coroutineRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(TvTab.NowPlaying, expectMostRecentItem().selectedTab)
        }
    }

    @Test
    fun `profile is signed out without an account`() = runTest {
        viewModel.uiState.test {
            assertEquals(TvProfileState.SignedOut, awaitItem().profile)
        }
    }

    @Test
    fun `profile has the account email when signed in`() = runTest {
        whenever(syncManager.isLoggedIn()).doReturn(true)
        whenever(syncManager.getEmail()).doReturn("user@example.com")
        isLoggedIn.accept(true)
        email.value = "user@example.com"

        viewModel.uiState.test {
            assertEquals(TvProfileState.SignedIn(email = "user@example.com"), awaitItem().profile)
        }
    }

    @Test
    fun `profile email is null when signed in with a blank email`() = runTest {
        whenever(syncManager.isLoggedIn()).doReturn(true)
        whenever(syncManager.getEmail()).doReturn("")
        isLoggedIn.accept(true)
        email.value = ""

        viewModel.uiState.test {
            assertEquals(TvProfileState.SignedIn(email = null), awaitItem().profile)
        }
    }

    @Test
    fun `profile is seeded from the sync manager before the streams emit`() = runTest {
        whenever(syncManager.isLoggedInObservable).doReturn(BehaviorRelay.create())
        whenever(syncManager.emailFlow()).doReturn(MutableSharedFlow())
        whenever(syncManager.isLoggedIn()).doReturn(true)
        whenever(syncManager.getEmail()).doReturn("user@example.com")

        viewModel.uiState.test {
            assertEquals(TvProfileState.SignedIn(email = "user@example.com"), awaitItem().profile)
        }
    }

    @Test
    fun `profile updates when the sign in state changes`() = runTest {
        viewModel.uiState.test {
            assertEquals(TvProfileState.SignedOut, awaitItem().profile)

            email.value = "user@example.com"
            isLoggedIn.accept(true)
            assertEquals(TvProfileState.SignedIn(email = "user@example.com"), awaitItem().profile)

            isLoggedIn.accept(false)
            assertEquals(TvProfileState.SignedOut, awaitItem().profile)
        }
    }

    @Test
    fun `openNowPlayingRequests emits when a launch open request is made`() = runTest {
        viewModel.openNowPlayingRequests.test {
            launchRequests.requestOpenNowPlaying()
            awaitItem()
        }
    }

    @Test
    fun `signOut delegates to the sign out manager`() = runTest {
        viewModel.signOut()

        verify(signOutManager).signOutAndWipeData()
    }
}
