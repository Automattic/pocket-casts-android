package au.com.shiftyjelly.pocketcasts.referrals

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfo
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsClaimGuestPassViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralOfferInfoProvider
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralsClaimGuestPassViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val referralOfferInfoProvider = mock<ReferralOfferInfoProvider>()
    private val referralOfferInfo = mock<ReferralsOfferInfoPlayStore>()
    private lateinit var viewModel: ReferralsClaimGuestPassViewModel

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
            assertEquals(UiState.Error, awaitItem())
        }
    }

    private suspend fun initViewModel(
        offerInfo: ReferralsOfferInfo? = referralOfferInfo,
    ) {
        whenever(referralOfferInfoProvider.referralOfferInfo()).thenReturn(offerInfo)
        viewModel = ReferralsClaimGuestPassViewModel(
            referralOfferInfoProvider = referralOfferInfoProvider,
        )
    }
}
