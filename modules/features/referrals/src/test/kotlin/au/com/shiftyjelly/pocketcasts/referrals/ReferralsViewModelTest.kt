package au.com.shiftyjelly.pocketcasts.referrals

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionType
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import io.reactivex.Flowable
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var viewModel: ReferralsViewModel
    private val email = "support@pocketcasts.com"
    private val statusAndroidPaidSubscription = SubscriptionStatus.Paid(
        expiry = Date(),
        autoRenew = true,
        giftDays = 0,
        frequency = SubscriptionFrequency.MONTHLY,
        platform = SubscriptionPlatform.ANDROID,
        subscriptionList = emptyList(),
        type = SubscriptionType.PLUS,
        tier = SubscriptionTier.PLUS,
        index = 0,
    )

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.REFERRALS, true)
    }

    @Test
    fun `referrals gift icon is not shown if feature flag is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.REFERRALS, false)

        initViewModel()

        viewModel.state.test {
            assertEquals(false, awaitItem().showIcon)
        }
    }

    @Test
    fun `referrals gift icon is not shown if signed out`() = runTest {
        initViewModel(SignInState.SignedOut)

        viewModel.state.test {
            assertEquals(false, awaitItem().showIcon)
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
            assertEquals(false, awaitItem().showIcon)
        }
    }

    @Test
    fun `referrals gift icon is shown for plus account`() = runTest {
        initViewModel(
            SignInState.SignedIn(
                email,
                statusAndroidPaidSubscription.copy(tier = SubscriptionTier.PLUS),
            ),
        )

        viewModel.state.test {
            assertEquals(true, awaitItem().showIcon)
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
            assertEquals(true, awaitItem().showIcon)
        }
    }

    @Test
    fun `updateBadgeCount decreases badge count when greater than zero`() = runTest {
        initViewModel() // badge count is 3 by default

        viewModel.updateBadgeCount()

        assertEquals(2, viewModel.state.value.badgeCount)
    }

    @Test
    fun `showBadge is true when showIcon is true and badgeCount is greater than zero`() = runTest {
        initViewModel()

        viewModel.state.test {
            assertEquals(true, awaitItem().showBadge)
        }
    }

    @Test
    fun `showBadge is false when showIcon is false`() = runTest {
        initViewModel(SignInState.SignedOut)

        viewModel.state.test {
            assertEquals(false, awaitItem().showBadge)
        }
    }

    @Test
    fun `showBadge is false when badgeCount is zero`() = runTest {
        initViewModel()
        viewModel.updateBadgeCount()
        viewModel.updateBadgeCount()
        viewModel.updateBadgeCount()

        viewModel.state.test {
            assertEquals(false, awaitItem().showBadge)
        }
    }

    private fun initViewModel(
        signInState: SignInState = SignInState.SignedIn(email, statusAndroidPaidSubscription),
    ) {
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(signInState))
        viewModel = ReferralsViewModel(userManager)
    }
}
