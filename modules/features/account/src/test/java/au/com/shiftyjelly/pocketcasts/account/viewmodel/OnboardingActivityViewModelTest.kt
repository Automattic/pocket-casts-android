package au.com.shiftyjelly.pocketcasts.account.viewmodel

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingExitInfo
import au.com.shiftyjelly.pocketcasts.settings.onboarding.SuggestedFoldersAction
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Flowable
import java.time.Instant
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

    private val subscription = Subscription(
        tier = SubscriptionTier.Plus,
        billingCycle = BillingCycle.Monthly,
        platform = SubscriptionPlatform.Android,
        expiryDate = Instant.now(),
        isAutoRenewing = true,
        giftDays = 0,
    )

    @Test
    fun `given simple exit info, when exit onboarding, then finish with Done`() = runTest {
        initViewModel(subscription = null)

        viewModel.finishState.test {
            viewModel.onExitOnboarding(OnboardingExitInfo.Simple)
            assert(awaitItem() == OnboardingFinish.Done)
        }
    }

    @Test
    fun `given plus promtion exit info for free user, when exit onboarding, then finish with DoneShowPlusPromotion`() = runTest {
        initViewModel(subscription = null)

        viewModel.finishState.test {
            viewModel.onExitOnboarding(OnboardingExitInfo.ShowPlusPromotion)
            assert(awaitItem() == OnboardingFinish.DoneShowPlusPromotion)
        }
    }

    @Test
    fun `given plus promtion exit info for paid user, when exit onboarding, then finish with Done`() = runTest {
        initViewModel(subscription)

        viewModel.finishState.test {
            viewModel.onExitOnboarding(OnboardingExitInfo.ShowPlusPromotion)
            assert(awaitItem() == OnboardingFinish.Done)
        }
    }

    @Test
    fun `given referral exit info, when exit onboarding, then finish with DoneShowWelcomeInReferralFlow`() = runTest {
        initViewModel(subscription = null)

        viewModel.finishState.test {
            viewModel.onExitOnboarding(OnboardingExitInfo.ShowReferralWelcome)
            assert(awaitItem() == OnboardingFinish.DoneShowWelcomeInReferralFlow)
        }
    }

    @Test
    fun `given suggested folders exit info, when exit onboarding, then finish with DoneShowWelcomeInReferralFlow`() = runTest {
        initViewModel(subscription)

        viewModel.finishState.test {
            viewModel.onExitOnboarding(OnboardingExitInfo.ApplySuggestedFolders(SuggestedFoldersAction.UseSuggestion))
            assert(awaitItem() == OnboardingFinish.DoneApplySuggestedFolders(SuggestedFoldersAction.UseSuggestion))
        }
    }

    private fun initViewModel(subscription: Subscription?) {
        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(
                SignInState.SignedIn(email = "", subscription),
            ),
        )
        viewModel = OnboardingActivityViewModel(
            userManager = userManager,
        )
    }
}
