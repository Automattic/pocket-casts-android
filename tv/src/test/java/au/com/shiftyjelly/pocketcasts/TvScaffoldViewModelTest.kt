package au.com.shiftyjelly.pocketcasts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.home.TvProfileState
import au.com.shiftyjelly.pocketcasts.home.TvScaffoldViewModel
import au.com.shiftyjelly.pocketcasts.home.TvTab
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import dagger.Lazy
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val userManager = mock<UserManager> {
        on { getSignInState() } doReturn Flowable.just(SignInState.SignedOut)
    }
    private val syncManager = mock<SyncManager> {
        on { isLoggedIn() } doReturn false
    }
    private val playbackManager = mock<PlaybackManager>()

    private val viewModel by lazy {
        TvScaffoldViewModel(userManager, syncManager, Lazy { playbackManager })
    }

    @Test
    fun `initial state has all tabs with first tab selected`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(TvTab.entries, state.tabs)
            assertEquals(0, state.selectedTabIndex)
        }
    }

    @Test
    fun `selectTab updates selected index`() = runTest {
        viewModel.uiState.test {
            assertEquals(0, awaitItem().selectedTabIndex)

            viewModel.selectTab(2)
            assertEquals(2, awaitItem().selectedTabIndex)
        }
    }

    @Test
    fun `selectTab to same index does not emit new state`() = runTest {
        viewModel.uiState.test {
            assertEquals(0, awaitItem().selectedTabIndex)

            viewModel.selectTab(0)
            expectNoEvents()
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
        whenever(userManager.getSignInState())
            .doReturn(Flowable.just(SignInState.SignedIn(email = "user@example.com", subscription = null)))

        viewModel.uiState.test {
            assertEquals(TvProfileState.SignedIn(email = "user@example.com"), awaitItem().profile)
        }
    }

    @Test
    fun `profile email is null when signed in with a blank email`() = runTest {
        whenever(userManager.getSignInState())
            .doReturn(Flowable.just(SignInState.SignedIn(email = "", subscription = null)))

        viewModel.uiState.test {
            assertEquals(TvProfileState.SignedIn(email = null), awaitItem().profile)
        }
    }

    @Test
    fun `profile is seeded from the sync manager before the sign in state emits`() = runTest {
        whenever(userManager.getSignInState()).doReturn(BehaviorProcessor.create())
        whenever(syncManager.isLoggedIn()).doReturn(true)
        whenever(syncManager.getEmail()).doReturn("user@example.com")

        viewModel.uiState.test {
            assertEquals(TvProfileState.SignedIn(email = "user@example.com"), awaitItem().profile)
        }
    }

    @Test
    fun `tabs can be selected before the sign in state emits`() = runTest {
        whenever(userManager.getSignInState()).doReturn(BehaviorProcessor.create())

        viewModel.uiState.test {
            assertEquals(0, awaitItem().selectedTabIndex)

            viewModel.selectTab(3)
            assertEquals(3, awaitItem().selectedTabIndex)
        }
    }

    @Test
    fun `profile updates when the sign in state changes`() = runTest {
        val signInState = BehaviorProcessor.createDefault<SignInState>(SignInState.SignedOut)
        whenever(userManager.getSignInState()).doReturn(signInState)

        viewModel.uiState.test {
            assertEquals(TvProfileState.SignedOut, awaitItem().profile)

            signInState.offer(SignInState.SignedIn(email = "user@example.com", subscription = null))
            assertEquals(TvProfileState.SignedIn(email = "user@example.com"), awaitItem().profile)

            signInState.offer(SignInState.SignedOut)
            assertEquals(TvProfileState.SignedOut, awaitItem().profile)
        }
    }

    @Test
    fun `signOut delegates to the user manager`() = runTest {
        viewModel.signOut()

        verify(userManager).signOut(playbackManager, wasInitiatedByUser = true)
    }
}
