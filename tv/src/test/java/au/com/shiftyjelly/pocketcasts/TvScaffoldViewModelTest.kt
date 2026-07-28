package au.com.shiftyjelly.pocketcasts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.auth.TvSignOutManager
import au.com.shiftyjelly.pocketcasts.home.TvProfileState
import au.com.shiftyjelly.pocketcasts.home.TvScaffoldViewModel
import au.com.shiftyjelly.pocketcasts.home.TvTab
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.jakewharton.rxrelay2.BehaviorRelay
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

    private val userManager = mock<UserManager>()
    private val syncManager = mock<SyncManager> {
        on { isLoggedIn() } doReturn false
        on { isLoggedInObservable } doReturn isLoggedIn
        on { emailFlow() } doReturn email
    }
    private val signOutManager = mock<TvSignOutManager>()

    private val viewModel by lazy {
        TvScaffoldViewModel(userManager, syncManager, signOutManager)
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
    fun `signOut delegates to the sign out manager`() = runTest {
        viewModel.signOut()

        verify(signOutManager).signOutAndWipeData()
    }
}
