package au.com.shiftyjelly.pocketcasts.views.viewmodels

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class BaseFragmentViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var endOfYearManager: EndOfYearManager

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var viewModel: BaseFragmentViewModel

    private val freeSubscriptionStatus = SubscriptionStatus.Free()

    @ExperimentalCoroutinesApi
    @Test
    fun `given signed in state and show badge, when init, then ui state updated correctly`() = runTest {
        val signInState = SignInState.SignedIn(email = "", subscriptionStatus = freeSubscriptionStatus)

        initViewModel(
            signInState = signInState,
            showBadge = true,
        )

        val result = viewModel.uiState.value
        assertEquals(signInState, result.signInState)
        assertEquals(true, result.showBadgeOnProfileMenu)
    }

    @Test
    fun `given not signed in state and no show badge, when init, then ui state updated correctly`() = runTest {
        val signInState = SignInState.SignedOut

        initViewModel(
            signInState = signInState,
            showBadge = false,
        )

        val result = viewModel.uiState.value
        assertEquals(signInState, result.signInState)
        assertEquals(false, result.showBadgeOnProfileMenu)
    }

    @Test
    fun `when profile menu tapped and eligible for stories, then set show badge to false`() = runTest {
        initViewModel(
            eligibleForStories = true,
        )
        whenever(settings.endOfYearShowBadge2023).thenReturn(mock())

        viewModel.onMenuItemTapped(UR.id.menu_profile)

        verify(settings.endOfYearShowBadge2023).set(false, updateModifiedAt = false)
    }

    @Test
    fun `when profile menu tapped and not eligible for stories, then set show badge to false`() = runTest {
        initViewModel(
            eligibleForStories = false,
        )
        whenever(settings.endOfYearShowBadge2023).thenReturn(mock())

        viewModel.onMenuItemTapped(UR.id.menu_profile)

        verify(settings.endOfYearShowBadge2023, never()).set(eq(anyBoolean()), eq(anyBoolean()))
    }

    private fun initViewModel(
        signInState: SignInState = SignInState.SignedOut,
        showBadge: Boolean = true,
        eligibleForStories: Boolean = true,
    ) = runTest {
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(signInState))
        whenever(settings.endOfYearShowBadge2023).thenReturn(UserSetting.Mock(showBadge, mock()))
        whenever(endOfYearManager.isEligibleForStories()).thenReturn(eligibleForStories)

        viewModel = BaseFragmentViewModel(
            userManager = userManager,
            endOfYearManager = endOfYearManager,
            settings = settings,
            analyticsTracker = analyticsTracker,
        )
    }
}
