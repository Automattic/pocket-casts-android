package au.com.shiftyjelly.pocketcasts.account.viewmodel

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionType
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingExitInfo
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Flowable
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class OnboardingActivityViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var userManager: UserManager

    private lateinit var viewModel: OnboardingActivityViewModel

    private val paidSubscriptionStatus = SubscriptionStatus.Paid(
        expiry = Date(),
        autoRenew = false,
        index = 0,
        platform = SubscriptionPlatform.GIFT,
        tier = SubscriptionTier.PLUS,
        type = SubscriptionType.PLUS,
    )

    private val freeSubscriptionStatus = SubscriptionStatus.Free()

    @Test
    fun `given showPlusPromotionForFreeUser is false, when exit onboarding, then finish with Done`() = runTest {
        initViewModel(freeSubscriptionStatus)

        viewModel.finishState.test {
            viewModel.onExitOnboarding(OnboardingExitInfo(showPlusPromotionForFreeUser = false))
            assert(awaitItem() == OnboardingFinish.Done)
        }
    }

    @Test
    fun `given showPlusPromotionForFreeUser is true and free user, when exit onboarding, then finish with DoneShowPlusPromotion`() = runTest {
        initViewModel(freeSubscriptionStatus)

        viewModel.finishState.test {
            viewModel.onExitOnboarding(OnboardingExitInfo(showPlusPromotionForFreeUser = true))
            assert(awaitItem() == OnboardingFinish.DoneShowPlusPromotion)
        }
    }

    @Test
    fun `given showPlusPromotionForFreeUser is true and paid user, when exit onboarding, then finish with Done`() = runTest {
        initViewModel(paidSubscriptionStatus)

        viewModel.finishState.test {
            viewModel.onExitOnboarding(OnboardingExitInfo(showPlusPromotionForFreeUser = true))
            assert(awaitItem() == OnboardingFinish.Done)
        }
    }

    private fun initViewModel(subscriptionStatus: SubscriptionStatus) {
        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(
                SignInState.SignedIn(email = "", subscriptionStatus = subscriptionStatus),
            ),
        )
        viewModel = OnboardingActivityViewModel(
            userManager = userManager,
        )
    }
}
