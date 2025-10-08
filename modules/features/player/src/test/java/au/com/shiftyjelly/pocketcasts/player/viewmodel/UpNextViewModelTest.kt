package au.com.shiftyjelly.pocketcasts.player.viewmodel

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Flowable
import java.time.Instant
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UpNextViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun `initial state isSignedInAsPaidUser should be true for paid user`() = runTest {
        val viewModel = initViewModel(isPaidUser = true)

        assertEquals(true, viewModel.isSignedInAsPaidUser.value)
    }

    @Test
    fun `initial state isSignedInAsPaidUser should be false for free user`() = runTest {
        val viewModel = initViewModel(isPaidUser = false)

        assertEquals(false, viewModel.isSignedInAsPaidUser.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initViewModel(
        isPaidUser: Boolean = false,
    ): UpNextViewModel {
        val userManager = mock<UserManager>()

        whenever(userManager.getSignInState())
            .thenReturn(
                Flowable.just(
                    SignInState.SignedIn(
                        email = "",
                        subscription = if (isPaidUser) {
                            Subscription(
                                tier = SubscriptionTier.Plus,
                                billingCycle = BillingCycle.Monthly,
                                platform = SubscriptionPlatform.Android,
                                expiryDate = Instant.now(),
                                isAutoRenewing = true,
                                giftDays = 0,
                            )
                        } else {
                            null
                        },
                    ),
                ),
            )
        return UpNextViewModel(userManager, mock(), AnalyticsTracker.test())
    }
}
