package au.com.shiftyjelly.pocketcasts.referrals

import app.cash.turbine.test
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
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import com.pocketcasts.service.api.ReferralValidationResponse
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
            referralResult = ErrorResult(""),
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
            referralResult = EmptyResult(),
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
            referralResult = ErrorResult(errorMessage = "", error = NoNetworkException()),
        )

        viewModel.onActivatePassClick()

        viewModel.state.test {
            assertEquals(UiState.Error(ReferralsClaimGuestPassError.NoNetwork), awaitItem())
        }
    }

    private suspend fun initViewModel(
        offerInfo: ReferralsOfferInfo? = referralOfferInfo,
        signInState: SignInState = SignInState.SignedOut,
        referralResult: ReferralManager.ReferralResult<ReferralValidationResponse> = SuccessResult(mock()),
    ) {
        whenever(referralOfferInfoProvider.referralOfferInfo()).thenReturn(offerInfo)
        whenever(settings.referralClaimCode).thenReturn(UserSetting.Mock(referralCode, mock()))
        whenever(referralManager.validateReferralCode(referralCode)).thenReturn(referralResult)
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(signInState))
        viewModel = ReferralsClaimGuestPassViewModel(
            referralOfferInfoProvider = referralOfferInfoProvider,
            referralManager = referralManager,
            userManager = userManager,
            settings = settings,
        )
    }
}
