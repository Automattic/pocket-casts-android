package au.com.shiftyjelly.pocketcasts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.home.TvScaffoldViewModel
import au.com.shiftyjelly.pocketcasts.home.TvTab
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TvScaffoldViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val userManager = mock<UserManager> {
        on { getSignInState() } doReturn Flowable.just(SignInState.SignedOut)
    }

    private val viewModel by lazy { TvScaffoldViewModel(userManager) }

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
}
