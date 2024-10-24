package au.com.shiftyjelly.pocketcasts.referrals

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfo
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsSendGuestPassViewModel.ReferralSendGuestPassError
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsSendGuestPassViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.EmptyResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.ErrorResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.SuccessResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralOfferInfoProvider
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import com.pocketcasts.service.api.ReferralCodeResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralsSendGuestPassViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val referralManager = mock<ReferralManager>()
    private val sharingClient = mock<SharingClient>()
    private val referralOfferInfoProvider = mock<ReferralOfferInfoProvider>()
    private val referralOfferInfo = mock<ReferralsOfferInfoPlayStore>()
    private val analyticsTracker = mock<AnalyticsTracker>()
    private lateinit var viewModel: ReferralsSendGuestPassViewModel

    private val referralCodeResponse = mock<ReferralCodeResponse>()
    private val referralCodeSuccessResult = SuccessResult(referralCodeResponse)
    private val referralCode = "referral_code"

    @Before
    fun setUp() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
        whenever(referralCodeResponse.code).thenReturn(referralCode)
        whenever(referralManager.getReferralCode()).thenReturn(referralCodeSuccessResult)
    }

    @Test
    fun `given referral subscription offer not found, when vm init, then error state shown`() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(null)
        initViewModel(offerInfo = referralOfferInfo)

        viewModel.state.test {
            assertTrue(awaitItem() == UiState.Error(ReferralSendGuestPassError.FailedToLoad))
        }
    }

    @Test
    fun `given referral subscription offer found, when vm init, then loaded state shown`() = runTest {
        whenever(referralManager.getReferralCode()).thenReturn(referralCodeSuccessResult)
        initViewModel(offerInfo = referralOfferInfo)

        viewModel.state.test {
            assertEquals(referralOfferInfo, (awaitItem() as UiState.Loaded).referralsOfferInfo)
        }
    }

    @Test
    fun `given referral code success, when getting referral code, then state is loaded`() = runTest {
        whenever(referralManager.getReferralCode()).thenReturn(referralCodeSuccessResult)

        initViewModel(offerInfo = referralOfferInfo)

        viewModel.state.test {
            assertEquals(UiState.Loaded(referralCode, referralOfferInfo), awaitItem())
        }
    }

    @Test
    fun `given empty result, when getting referral code, then state is empty error`() = runTest {
        whenever(referralManager.getReferralCode()).thenReturn(EmptyResult())

        initViewModel()

        viewModel.state.test {
            assertTrue(awaitItem() == UiState.Error(ReferralSendGuestPassError.Empty))
        }
    }

    @Test
    fun `given network error, when getting referral code, then state is network error`() = runTest {
        whenever(referralManager.getReferralCode()).thenReturn(ErrorResult(errorMessage = "", error = NoNetworkException()))

        initViewModel()

        viewModel.state.test {
            assertTrue(awaitItem() == UiState.Error(ReferralSendGuestPassError.NoNetwork))
        }
    }

    @Test
    fun `given unknown error, when getting referral code, then state is failed to load`() = runTest {
        whenever(referralManager.getReferralCode()).thenReturn(ErrorResult(""))

        initViewModel()

        viewModel.state.test {
            assertTrue(awaitItem() == UiState.Error(ReferralSendGuestPassError.FailedToLoad))
        }
    }

    @Test
    fun `when retry clicked, then referral code is fetched again`() = runTest {
        initViewModel()

        viewModel.onRetry()

        verify(referralManager, times(2)).getReferralCode()
    }

    @Test
    fun `given referral code, when share clicked, then referral code is shared`() = runTest {
        val requestCaptor = argumentCaptor<SharingRequest>()
        whenever(referralManager.getReferralCode()).thenReturn(referralCodeSuccessResult)

        initViewModel()
        viewModel.onShareClick(referralCode)

        verify(referralManager).getReferralCode()
        verify(sharingClient).share(requestCaptor.capture())
        val capturedRequest = requestCaptor.firstValue
        with(capturedRequest) {
            assertEquals(referralCode, (data as SharingRequest.Data.ReferralLink).referralCode)
            assertEquals(SocialPlatform.More, platform)
            assertEquals(AnalyticsEvent.REFERRAL_PASS_SHARED, analyticsEvent)
            assertEquals(
                mapOf(
                    "code" to referralCode,
                    "source" to SourceView.REFERRALS.analyticsValue,
                    "type" to "referral_link",
                    "action" to "system_sheet",
                ),
                analyticsProperties,
            )
        }
    }

    private suspend fun initViewModel(
        offerInfo: ReferralsOfferInfo? = referralOfferInfo,
    ) {
        whenever(referralOfferInfoProvider.referralOfferInfo()).thenReturn(offerInfo)
        viewModel = ReferralsSendGuestPassViewModel(
            referralsManager = referralManager,
            sharingClient = sharingClient,
            referralOfferInfoProvider = referralOfferInfoProvider,
            analyticsTracker = analyticsTracker,
        )
    }
}
