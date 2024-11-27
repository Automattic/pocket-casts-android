package au.com.shiftyjelly.pocketcasts.player.viewmodel

import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import io.reactivex.Flowable
import java.util.Date
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UpNextViewModelTest {

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
                        subscriptionStatus = if (isPaidUser) {
                            SubscriptionStatus.Paid(
                                expiryDate = Date(),
                                autoRenew = true,
                                giftDays = 0,
                                frequency = SubscriptionFrequency.MONTHLY,
                                platform = SubscriptionPlatform.ANDROID,
                                subscriptions = emptyList(),
                                tier = SubscriptionTier.PLUS,
                                index = 0,
                            )
                        } else {
                            SubscriptionStatus.Free()
                        },
                    ),
                ),
            )
        return UpNextViewModel(userManager, UnconfinedTestDispatcher())
    }
}
