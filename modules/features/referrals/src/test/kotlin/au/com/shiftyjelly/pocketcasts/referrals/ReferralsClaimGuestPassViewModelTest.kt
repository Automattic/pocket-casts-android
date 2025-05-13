package au.com.shiftyjelly.pocketcasts.referrals

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.FakePaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.flatMap
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsClaimGuestPassViewModel.NavigationEvent
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsClaimGuestPassViewModel.ReferralsClaimGuestPassError
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsClaimGuestPassViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.EmptyResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.ErrorResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.SuccessResult
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import com.pocketcasts.service.api.ReferralValidationResponse
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralsClaimGuestPassViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val paymentDataSource = FakePaymentDataSource()
    private val paymentClient = PaymentClient.test(paymentDataSource)
    private val referralManager = mock<ReferralManager>()
    private val userManager = mock<UserManager>()
    private val analyticsTracker = mock<AnalyticsTracker>()
    private val settings = mock<Settings>()
    private lateinit var viewModel: ReferralsClaimGuestPassViewModel
    private val referralCode = "referral_code"
    private val referralPlan = SubscriptionPlans.Preview
        .findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.Referral)
        .flatMap(ReferralSubscriptionPlan::create)
        .getOrNull()!!

    @Test
    fun `given referral subscription offer found, when vm init, then state is loaded`() = runTest {
        initViewModel()

        viewModel.state.test {
            assertEquals(UiState.Loaded(referralPlan), awaitItem())
        }
    }

    @Test
    fun `given referral subscription offer not found, when vm init, then error state is shown`() = runTest {
        paymentDataSource.loadedProducts = emptyList()

        initViewModel()

        viewModel.state.test {
            assertEquals(UiState.Error(ReferralsClaimGuestPassError.FailedToLoadOffer), awaitItem())
        }
    }

    @Test
    fun `given user not signed-in, when activate pass button is clicked, then navigate to login or signup`() = runTest {
        initViewModel(
            signInState = SignInState.SignedOut,
        )

        viewModel.navigationEvent.test {
            viewModel.onActivatePassClick()
            assertEquals(NavigationEvent.LoginOrSignup, awaitItem())
        }
    }

    @Test
    fun `given user signed-in, when activate pass button is clicked, then referral code is validated`() = runTest {
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))
        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
        )

        viewModel.onActivatePassClick()

        verify(referralManager).validateReferralCode(referralCode)
    }

    @Test
    fun `given validation error, when activate pass button is clicked, then invalid offer is shown`() = runTest {
        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
            referralValidationResult = ErrorResult(""),
        )

        viewModel.navigationEvent.test {
            viewModel.onActivatePassClick()
            assertEquals(NavigationEvent.InValidOffer, awaitItem())
        }
    }

    @Test
    fun `given validation empty result, when activate pass button is clicked, then invalid offer is shown`() = runTest {
        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
            referralValidationResult = EmptyResult(),
        )

        viewModel.navigationEvent.test {
            viewModel.onActivatePassClick()
            assertEquals(NavigationEvent.InValidOffer, awaitItem())
        }
    }

    @Test
    fun `given no network, when activate pass button is clicked, then no network error is shown`() = runTest {
        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
            referralValidationResult = ErrorResult(errorMessage = "", error = NoNetworkException()),
        )

        viewModel.snackBarEvent.test {
            viewModel.onActivatePassClick()
            assertEquals(ReferralsClaimGuestPassViewModel.SnackbarEvent.NoNetwork, awaitItem())
        }
    }

    @Test
    fun `given validation success, when activate pass button is clicked, then billing flow is started`() = runTest {
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))

        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
            referralValidationResult = SuccessResult(mock()),
        )

        viewModel.navigationEvent.test {
            viewModel.onActivatePassClick()
            assertTrue(awaitItem() is NavigationEvent.LaunchBillingFlow)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when purchase is successful, then code is redeemed`() = runTest {
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))

        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
            referralValidationResult = SuccessResult(mock()),
        )
        viewModel.launchBillingFlow(referralPlan, mock())

        verify(referralManager).redeemReferralCode(referralCode)
    }

    @Test
    fun `when purchase fails, then purchase failed error message is shown`() = runTest {
        paymentDataSource.purchasedProductsResultCode = PaymentResultCode.Error

        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
            referralValidationResult = SuccessResult(mock()),
        )

        viewModel.snackBarEvent.test {
            viewModel.launchBillingFlow(referralPlan, mock())
            assertEquals(ReferralsClaimGuestPassViewModel.SnackbarEvent.PurchaseFailed, awaitItem())
        }
    }

    @Test
    fun `when redeem fails, then redeem failed error message is shown`() = runTest {
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(ErrorResult(""))

        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
            referralValidationResult = SuccessResult(mock()),
        )

        viewModel.snackBarEvent.test {
            viewModel.launchBillingFlow(referralPlan, mock())
            assertEquals(ReferralsClaimGuestPassViewModel.SnackbarEvent.RedeemFailed, awaitItem())
        }
    }

    @Test
    fun `when redeem is successful, then screen closes`() = runTest {
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))

        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
            referralValidationResult = SuccessResult(mock()),
        )

        viewModel.navigationEvent.test {
            viewModel.launchBillingFlow(referralPlan, mock())
            assertEquals(NavigationEvent.Close, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given show welcome screen setting true, when redeem is successful, then welcome screen shown`() = runTest {
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))

        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
            referralValidationResult = SuccessResult(mock()),
            showWelcomeSetting = UserSetting.Mock(true, mock()),
        )

        viewModel.navigationEvent.test {
            viewModel.launchBillingFlow(referralPlan, mock())
            skipItems(1) // skip close screen
            assertEquals(NavigationEvent.Welcome, awaitItem())
        }
    }

    @Test
    fun `given show welcome screen setting false, when redeem is successful, then welcome screen not shown`() = runTest {
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))

        initViewModel(
            signInState = SignInState.SignedIn("email", subscription = null),
            referralValidationResult = SuccessResult(mock()),
            showWelcomeSetting = UserSetting.Mock(false, mock()),
        )

        viewModel.navigationEvent.test {
            viewModel.launchBillingFlow(referralPlan, mock())
            assertEquals(NavigationEvent.Close, awaitItem())
            ensureAllEventsConsumed()
        }
    }

    private suspend fun initViewModel(
        signInState: SignInState = SignInState.SignedOut,
        referralValidationResult: ReferralManager.ReferralResult<ReferralValidationResponse> = SuccessResult(mock()),
        showWelcomeSetting: UserSetting<Boolean> = UserSetting.Mock(false, mock()),
    ) {
        whenever(settings.referralClaimCode).thenReturn(UserSetting.Mock(referralCode, mock()))
        whenever(settings.showReferralWelcome).thenReturn(showWelcomeSetting)
        whenever(referralManager.validateReferralCode(referralCode)).thenReturn(referralValidationResult)
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(signInState))
        viewModel = ReferralsClaimGuestPassViewModel(
            paymentClient = paymentClient,
            referralManager = referralManager,
            userManager = userManager,
            settings = settings,
            analyticsTracker = analyticsTracker,
        )
    }
}
