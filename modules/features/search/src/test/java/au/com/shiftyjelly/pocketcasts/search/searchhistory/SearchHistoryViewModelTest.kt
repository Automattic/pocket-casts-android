package au.com.shiftyjelly.pocketcasts.search.searchhistory

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import io.reactivex.Flowable
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class SearchHistoryViewModelTest {
    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var searchHistoryManager: SearchHistoryManager

    private val subscription = Subscription(
        tier = SubscriptionTier.Plus,
        billingCycle = BillingCycle.Monthly,
        platform = SubscriptionPlatform.Android,
        expiryDate = Instant.now(),
        isAutoRenewing = true,
        giftDays = 0,
    )

    @Test
    fun `given paid subscription status and local + remote search, when search history shown, then folders included`() = runTest {
        val viewModel = initViewModel(isPlusUser = true, isOnlySearchRemote = false)

        viewModel.start()

        verify(searchHistoryManager).findAll(showFolders = eq(true))
    }

    @Test
    fun `given free subscription status and local + remote only search, when search history shown, then folders not included`() = runTest {
        val viewModel = initViewModel(isPlusUser = false, isOnlySearchRemote = true)

        viewModel.start()

        verify(searchHistoryManager).findAll(showFolders = eq(false))
    }

    @Test
    fun `given paid subscription status and remote only search, when search history shown, then folders not included`() = runTest {
        val viewModel = initViewModel(isPlusUser = true, isOnlySearchRemote = true)

        viewModel.start()

        verify(searchHistoryManager).findAll(showFolders = eq(false))
    }

    @Test
    fun `given free subscription status and remote only search, when search history shown, then folders not included`() = runTest {
        val viewModel = initViewModel(isPlusUser = false, isOnlySearchRemote = true)

        viewModel.start()

        verify(searchHistoryManager).findAll(showFolders = eq(false))
    }

    private suspend fun initViewModel(
        isPlusUser: Boolean = false,
        isOnlySearchRemote: Boolean = false,
    ): SearchHistoryViewModel {
        whenever(userManager.getSignInState())
            .thenReturn(
                Flowable.just(
                    SignInState.SignedIn(
                        email = "",
                        subscription = if (isPlusUser) subscription else null,
                    ),
                ),
            )
        whenever(searchHistoryManager.findAll(showFolders = anyBoolean()))
            .thenReturn(mock())
        val viewModel =
            SearchHistoryViewModel(searchHistoryManager, userManager, UnconfinedTestDispatcher(), AnalyticsTracker.test())
        viewModel.setOnlySearchRemote(isOnlySearchRemote)
        return viewModel
    }
}
