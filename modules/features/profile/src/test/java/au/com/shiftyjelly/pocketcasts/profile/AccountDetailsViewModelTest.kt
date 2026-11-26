package au.com.shiftyjelly.pocketcasts.profile

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.eventhorizon.EventHorizon
import io.reactivex.Flowable
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailsViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun `plus subscriber on Android can upgrade to patron`() = runTest {
        assertEquals(true, canUpgrade(SubscriptionTier.Plus, SubscriptionPlatform.Android))
    }

    @Test
    fun `plus subscriber on iOS cannot upgrade to patron`() = runTest {
        assertEquals(false, canUpgrade(SubscriptionTier.Plus, SubscriptionPlatform.iOS))
    }

    @Test
    fun `plus subscriber on web cannot upgrade to patron`() = runTest {
        assertEquals(false, canUpgrade(SubscriptionTier.Plus, SubscriptionPlatform.Web))
    }

    @Test
    fun `plus subscriber with gift can upgrade to patron`() = runTest {
        assertEquals(true, canUpgrade(SubscriptionTier.Plus, SubscriptionPlatform.Gift))
    }

    @Test
    fun `plus subscriber with unknown platform can upgrade to patron`() = runTest {
        assertEquals(true, canUpgrade(SubscriptionTier.Plus, SubscriptionPlatform.Unknown))
    }

    @Test
    fun `patron subscriber cannot upgrade to patron`() = runTest {
        assertEquals(false, canUpgrade(SubscriptionTier.Patron, SubscriptionPlatform.Android))
    }

    private suspend fun canUpgrade(tier: SubscriptionTier, platform: SubscriptionPlatform): Boolean {
        val signInState = SignInState.SignedIn(
            email = "user@example.com",
            subscription = Subscription(
                tier = tier,
                billingCycle = BillingCycle.Yearly,
                platform = platform,
                expiryDate = Instant.now().plusSeconds(60 * 60 * 24),
                isAutoRenewing = true,
                giftDays = 0,
            ),
        )
        val userManager = mock<UserManager> {
            on { getSignInState() } doReturn Flowable.just<SignInState>(signInState)
        }
        val settings = mock<Settings>(defaultAnswer = RETURNS_DEEP_STUBS)
        whenever(settings.marketingOptIn.flow).thenReturn(MutableStateFlow(false))
        val paymentClient = mock<PaymentClient>()
        whenever(paymentClient.loadAcknowledgedSubscriptions()).thenReturn(PaymentResult.Success(emptyList()))
        val syncManager = mock<SyncManager>()
        whenever(syncManager.isGoogleLogin()).thenReturn(false)

        val viewModel = AccountDetailsViewModel(
            userManager = userManager,
            paymentClient = paymentClient,
            syncManager = syncManager,
            settings = settings,
            eventHorizon = EventHorizon(TestEventSink()),
            crashLogging = mock<CrashLogging>(),
        )

        val state = viewModel.sectionsState.first { it.email != null }
        return AccountSection.UpgradeToPatron in state.availableSections
    }
}
