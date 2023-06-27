package au.com.shiftyjelly.pocketcasts.search.searchhistory

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionType
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import io.reactivex.Flowable
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
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class SearchHistoryViewModelTest {
    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var searchHistoryManager: SearchHistoryManager

    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val subscriptionStatusPaid = SubscriptionStatus.Paid(
        expiry = Date(),
        autoRenew = true,
        giftDays = 0,
        frequency = SubscriptionFrequency.MONTHLY,
        platform = SubscriptionPlatform.ANDROID,
        subscriptionList = emptyList(),
        type = SubscriptionType.PLUS,
        tier = SubscriptionTier.PLUS,
        index = 0
    )

    private val subscriptionStatusFree = SubscriptionStatus.Free()

    @Test
    fun `given paid subscription status and local + remote search, when search history shown, then folders included`() =
        runTest {
            val viewModel = initViewModel(isPlusUser = true, isOnlySearchRemote = false)

            viewModel.start()

            verify(searchHistoryManager).findAll(showFolders = eq(true))
        }

    @Test
    fun `given free subscription status and local + remote only search, when search history shown, then folders not included`() =
        runTest {
            val viewModel = initViewModel(isPlusUser = false, isOnlySearchRemote = true)

            viewModel.start()

            verify(searchHistoryManager).findAll(showFolders = eq(false))
        }

    @Test
    fun `given paid subscription status and remote only search, when search history shown, then folders not included`() =
        runTest {
            val viewModel = initViewModel(isPlusUser = true, isOnlySearchRemote = true)

            viewModel.start()

            verify(searchHistoryManager).findAll(showFolders = eq(false))
        }

    @Test
    fun `given free subscription status and remote only search, when search history shown, then folders not included`() =
        runTest {
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
                        subscriptionStatus = if (isPlusUser) subscriptionStatusPaid else subscriptionStatusFree
                    )
                )
            )
        whenever(searchHistoryManager.findAll(showFolders = anyBoolean()))
            .thenReturn(mock())
        val viewModel =
            SearchHistoryViewModel(searchHistoryManager, userManager, UnconfinedTestDispatcher(), analyticsTracker)
        viewModel.setOnlySearchRemote(isOnlySearchRemote)
        return viewModel
    }
}
