package au.com.shiftyjelly.pocketcasts.referrals

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfo
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralOfferInfoProvider
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.reactivex.Flowable
import java.util.Date
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

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val userManager: UserManager = mock()
    private val settings: Settings = mock()
    private val referralOfferInfoProvider: ReferralOfferInfoProvider = mock()
    private val referralOfferInfo: ReferralsOfferInfoPlayStore = mock()
    private val analyticsTracker: AnalyticsTracker = mock()
    private lateinit var viewModel: ReferralsViewModel
    private val email = "support@pocketcasts.com"
    private val referralClaimCode = "referral_code"
    private val statusAndroidPaidSubscription = SubscriptionStatus.Paid(
        expiryDate = Date(),
        autoRenew = true,
        giftDays = 0,
        frequency = SubscriptionFrequency.MONTHLY,
        platform = SubscriptionPlatform.ANDROID,
        subscriptions = emptyList(),
        tier = SubscriptionTier.PLUS,
        index = 0,
    )

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.REFERRALS_CLAIM, true)
        FeatureFlag.setEnabled(Feature.REFERRALS_SEND, true)
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
        whenever(settings.playerOrUpNextBottomSheetState).thenReturn(flowOf(BottomSheetBehavior.STATE_COLLAPSED))
    }

    @Test
    fun `gift icon ,tooltip, profile banner are not shown if referral subscription offer not found`() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(null)
        initViewModel(offerInfo = referralOfferInfo)

        viewModel.state.test {
            assertEquals(
                UiState.Loaded(
                    showIcon = false,
                    showTooltip = false,
                    showProfileBanner = false,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `referrals gift icon hidden if referrals send feature flag is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.REFERRALS_SEND, false)

        initViewModel()

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showIcon)
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
            SignInState.SignedIn(
                email,
                statusAndroidPaidSubscription.copy(tier = SubscriptionTier.NONE),
            ),
        )

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showIcon)
        }
    }

    @Test
    fun `referrals gift icon is shown for plus account`() = runTest {
        whenever(referralOfferInfo.subscriptionWithOffer).thenReturn(mock<Subscription.Trial>())
        initViewModel(
            SignInState.SignedIn(
                email,
                statusAndroidPaidSubscription.copy(tier = SubscriptionTier.PLUS),
            ),
        )

        viewModel.state.test {
            assertEquals(true, (awaitItem() as UiState.Loaded).showIcon)
        }
    }

    @Test
    fun `referrals gift icon is shown for patron account`() = runTest {
        initViewModel(
            SignInState.SignedIn(
                email,
                statusAndroidPaidSubscription.copy(tier = SubscriptionTier.PATRON),
            ),
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
    fun `tooltip hidden if referrals send feature flag is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.REFERRALS_SEND, false)

        initViewModel()

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
            signInState = SignInState.SignedIn(email, statusAndroidPaidSubscription),
        )

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showProfileBanner)
        }
    }

    @Test
    fun `profile banner is shown if signed in as free and referral code not empty`() = runTest {
        initViewModel(
            signInState = SignInState.SignedIn(email, SubscriptionStatus.Free()),
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

    @Test
    fun `profile banner is hidden if referrals claim feature is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.REFERRALS_CLAIM, false)
        initViewModel(
            signInState = SignInState.SignedOut,
            referralCode = referralClaimCode,
        )

        viewModel.state.test {
            assertEquals(false, (awaitItem() as UiState.Loaded).showProfileBanner)
        }
    }

    private suspend fun initViewModel(
        signInState: SignInState = SignInState.SignedIn(email, statusAndroidPaidSubscription),
        offerInfo: ReferralsOfferInfo = referralOfferInfo,
        referralCode: String = referralClaimCode,
        showReferralsTooltipUserSetting: UserSetting<Boolean> = UserSetting.Mock(true, mock()),
    ) {
        whenever(settings.showReferralsTooltip).thenReturn(showReferralsTooltipUserSetting)
        whenever(referralOfferInfoProvider.referralOfferInfo()).thenReturn(offerInfo)
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(signInState))
        whenever(settings.referralClaimCode).thenReturn(UserSetting.Mock(referralCode, mock()))
        viewModel = ReferralsViewModel(
            userManager = userManager,
            settings = settings,
            referralOfferInfoProvider = referralOfferInfoProvider,
            analyticsTracker = analyticsTracker,
        )
    }
}
