package au.com.shiftyjelly.pocketcasts.referrals

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfo
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsClaimGuestPassViewModel.NavigationEvent
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsClaimGuestPassViewModel.ReferralsClaimGuestPassError
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsClaimGuestPassViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.EmptyResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.ErrorResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.SuccessResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralOfferInfoProvider
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import com.pocketcasts.service.api.ReferralValidationResponse
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralsClaimGuestPassViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val referralOfferInfoProvider = mock<ReferralOfferInfoProvider>()
    private val referralOfferInfo = mock<ReferralsOfferInfoPlayStore>()
    private val referralManager = mock<ReferralManager>()
    private val userManager = mock<UserManager>()
    private val subscriptionManager = mock<SubscriptionManager>()
    private val analyticsTracker = mock<AnalyticsTracker>()
    private val settings = mock<Settings>()
    private lateinit var viewModel: ReferralsClaimGuestPassViewModel
    private val referralCode = "referral_code"

    @Before
    fun setUp() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
    }

    @Test
    fun `given referral subscription offer found, when vm init, then state is loaded`() = runTest {
        initViewModel(offerInfo = referralOfferInfo)

        viewModel.state.test {
            assertEquals(UiState.Loaded(referralOfferInfo), awaitItem())
        }
    }

    @Test
    fun `given referral subscription offer not found, when vm init, then error state is shown`() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(null)
        initViewModel(offerInfo = referralOfferInfo)

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
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
        )

        viewModel.onActivatePassClick()

        verify(referralManager).validateReferralCode(referralCode)
    }

    @Test
    fun `given validation error, when activate pass button is clicked, then invalid offer is shown`() = runTest {
        initViewModel(
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
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
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
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
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
            referralValidationResult = ErrorResult(errorMessage = "", error = NoNetworkException()),
        )

        viewModel.snackBarEvent.test {
            viewModel.onActivatePassClick()
            assertEquals(ReferralsClaimGuestPassViewModel.SnackbarEvent.NoNetwork, awaitItem())
        }
    }

    @Test
    fun `given validation success, when activate pass button is clicked, then billing flow is started`() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))
        initViewModel(
            offerInfo = referralOfferInfo,
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
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
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))
        initViewModel(
            offerInfo = referralOfferInfo,
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
            referralValidationResult = SuccessResult(mock()),
            purchaseEvent = PurchaseEvent.Success,
        )
        viewModel.onActivatePassClick()

        verify(referralManager).redeemReferralCode(referralCode)
    }

    @Test
    fun `when purchase fails, then purchase failed error message is shown`() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
        initViewModel(
            offerInfo = referralOfferInfo,
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
            referralValidationResult = SuccessResult(mock()),
            purchaseEvent = PurchaseEvent.Failure(errorMessage = "", responseCode = 0),
        )

        viewModel.snackBarEvent.test {
            viewModel.onActivatePassClick()
            assertEquals(ReferralsClaimGuestPassViewModel.SnackbarEvent.PurchaseFailed, awaitItem())
        }
    }

    @Test
    fun `when redeem fails, then redeem failed error message is shown`() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(ErrorResult(""))
        initViewModel(
            offerInfo = referralOfferInfo,
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
            referralValidationResult = SuccessResult(mock()),
        )

        viewModel.snackBarEvent.test {
            viewModel.onActivatePassClick()
            assertEquals(ReferralsClaimGuestPassViewModel.SnackbarEvent.RedeemFailed, awaitItem())
        }
    }

    @Test
    fun `when redeem is successful, then screen closes`() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))
        initViewModel(
            offerInfo = referralOfferInfo,
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
            referralValidationResult = SuccessResult(mock()),
        )

        viewModel.navigationEvent.test {
            viewModel.onActivatePassClick()
            skipItems(1) // skip billing launch
            assertEquals(NavigationEvent.Close, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given show welcome screen setting true, when redeem is successful, then welcome screen shown`() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))
        initViewModel(
            offerInfo = referralOfferInfo,
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
            referralValidationResult = SuccessResult(mock()),
            showWelcomeSetting = UserSetting.Mock(true, mock()),
        )

        viewModel.navigationEvent.test {
            viewModel.onActivatePassClick()
            skipItems(2) // skip billing launch, close screen
            assertEquals(NavigationEvent.Welcome, awaitItem())
        }
    }

    @Test
    fun `given show welcome screen setting false, when redeem is successful, then welcome screen not shown`() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
        whenever(referralManager.redeemReferralCode(referralCode)).thenReturn(SuccessResult(mock()))
        initViewModel(
            offerInfo = referralOfferInfo,
            signInState = SignInState.SignedIn("email", SubscriptionStatus.Free()),
            referralValidationResult = SuccessResult(mock()),
            showWelcomeSetting = UserSetting.Mock(false, mock()),
        )

        viewModel.navigationEvent.test {
            viewModel.onActivatePassClick()
            assertTrue(awaitItem() is NavigationEvent.LaunchBillingFlow)
            assertEquals(NavigationEvent.Close, awaitItem())
            ensureAllEventsConsumed()
        }
    }

    private suspend fun initViewModel(
        offerInfo: ReferralsOfferInfo? = referralOfferInfo,
        signInState: SignInState = SignInState.SignedOut,
        referralValidationResult: ReferralManager.ReferralResult<ReferralValidationResponse> = SuccessResult(mock()),
        purchaseEvent: PurchaseEvent = PurchaseEvent.Success,
        showWelcomeSetting: UserSetting<Boolean> = UserSetting.Mock(false, mock()),
    ) {
        whenever(subscriptionManager.observePurchaseEvents()).thenReturn(Flowable.just(purchaseEvent))
        whenever(referralOfferInfoProvider.referralOfferInfo()).thenReturn(offerInfo)
        whenever(settings.referralClaimCode).thenReturn(UserSetting.Mock(referralCode, mock()))
        whenever(settings.showReferralWelcome).thenReturn(showWelcomeSetting)
        whenever(referralManager.validateReferralCode(referralCode)).thenReturn(referralValidationResult)
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(signInState))
        viewModel = ReferralsClaimGuestPassViewModel(
            referralOfferInfoProvider = referralOfferInfoProvider,
            referralManager = referralManager,
            userManager = userManager,
            subscriptionManager = subscriptionManager,
            settings = settings,
            analyticsTracker = analyticsTracker,
        )
    }
}
