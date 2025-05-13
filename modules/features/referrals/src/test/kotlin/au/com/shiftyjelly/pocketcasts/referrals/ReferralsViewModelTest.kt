package au.com.shiftyjelly.pocketcasts.referrals

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.FakePaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.reactivex.Flowable
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralsViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val paymentDataSource = FakePaymentDataSource()
    private val paymentClient = PaymentClient.test(paymentDataSource)

    private val userManager: UserManager = mock()
    private val settings: Settings = mock()
    private val analyticsTracker: AnalyticsTracker = mock()
    private lateinit var viewModel: ReferralsViewModel
    private val email = "support@pocketcasts.com"
    private val referralClaimCode = "referral_code"
    private val subscription = Subscription(
        tier = SubscriptionTier.Plus,
        billingCycle = BillingCycle.Monthly,
        platform = SubscriptionPlatform.Android,
        expiryDate = Instant.now(),
        isAutoRenewing = true,
        giftDays = 0,
    )

    @Before
    fun setUp() {
        whenever(settings.playerOrUpNextBottomSheetState).thenReturn(flowOf(BottomSheetBehavior.STATE_COLLAPSED))
    }

    @Test
    fun `gift icon ,tooltip, profile banner are not shown if referral subscription offer not found`() = runTest {
        paymentDataSource.loadedProducts = emptyList()
        initViewModel()

        viewModel.state.test {
            assertEquals(UiState.NoOffer, awaitItem())
        }
    }

    @Test
    fun `referrals gift icon is not shown if signed out`() = runTest {
        initViewModel(SignInState.SignedOut)

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showIcon)
        }
    }

    @Test
    fun `referrals gift icon is not shown for free account`() = runTest {
        initViewModel(
            SignInState.SignedIn(email, subscription = null),
        )

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showIcon)
        }
    }

    @Test
    fun `referrals gift icon is shown for plus account`() = runTest {
        initViewModel(
            SignInState.SignedIn(email, subscription.copy(tier = SubscriptionTier.Plus)),
        )

        viewModel.state.test {
            assertEquals(true, (awaitItem() as UiState.Loaded).showIcon)
        }
    }

    @Test
    fun `referrals gift icon is shown for patron account`() = runTest {
        initViewModel(
            SignInState.SignedIn(email, subscription.copy(tier = SubscriptionTier.Patron)),
        )

        viewModel.state.test {
            assertEquals(true, (awaitItem() as UiState.Loaded).showIcon)
        }
    }

    @Test
    fun `tooltip is shown for paid account on launch`() = runTest {
        initViewModel()

        viewModel.state.test {
            assertEquals(true, (awaitItem() as UiState.Loaded).showTooltip)
        }
    }

    @Test
    fun `tooltip is not shown for free account on launch`() = runTest {
        initViewModel(SignInState.SignedOut)

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showTooltip)
        }
    }

    @Test
    fun `tooltip is hidden on icon click`() = runTest {
        initViewModel()

        viewModel.onIconClick()

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showTooltip)
        }
    }

    @Test
    fun `tooltip is hidden on tooltip click`() = runTest {
        initViewModel()

        viewModel.onTooltipClick()

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showTooltip)
        }
    }

    @Test
    fun `profile banner is hidden if referral code is empty`() = runTest {
        initViewModel(
            referralCode = "",
        )

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showProfileBanner)
        }
    }

    @Test
    fun `profile banner is hidden if signed in as paid`() = runTest {
        initViewModel(
            signInState = SignInState.SignedIn(email, subscription),
        )

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showProfileBanner)
        }
    }

    @Test
    fun `profile banner is shown if signed in as free and referral code not empty`() = runTest {
        initViewModel(
            signInState = SignInState.SignedIn(email, subscription = null),
            referralCode = referralClaimCode,
        )

        viewModel.state.test {
            assertEquals(true, (awaitItem() as UiState.Loaded).showProfileBanner)
        }
    }

    @Test
    fun `profile banner is shown if signed out and referral code not empty`() = runTest {
        initViewModel(
            signInState = SignInState.SignedOut,
            referralCode = referralClaimCode,
        )

        viewModel.state.test {
            assertEquals(true, (awaitItem() as UiState.Loaded).showProfileBanner)
        }
    }

    @Test
    fun `profile banner is hidden on hide banner click`() = runTest {
        initViewModel(
            signInState = SignInState.SignedOut,
            referralCode = referralClaimCode,
        )

        viewModel.onHideBannerClick()

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showProfileBanner)
        }
    }

    private suspend fun initViewModel(
        signInState: SignInState = SignInState.SignedIn(email, subscription),
        referralCode: String = referralClaimCode,
        showReferralsTooltipUserSetting: UserSetting<Boolean> = UserSetting.Mock(true, mock()),
    ) {
        whenever(settings.showReferralsTooltip).thenReturn(showReferralsTooltipUserSetting)
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(signInState))
        whenever(settings.referralClaimCode).thenReturn(UserSetting.Mock(referralCode, mock()))
        viewModel = ReferralsViewModel(
            userManager = userManager,
            paymentClient = paymentClient,
            settings = settings,
            analyticsTracker = analyticsTracker,
        )
    }
}
