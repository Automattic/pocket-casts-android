package au.com.shiftyjelly.pocketcasts.repositories.payment

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import au.com.shiftyjelly.pocketcasts.models.type.Membership
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.InAppMessageResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PlayInAppMessagesTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val dataSource = PaymentDataSource.fake()
    private val listener = RecordingListener()
    private var paymentClient = PaymentClient.test(dataSource, listener)
    private val subscriptionManager = mock<SubscriptionManager>()
    private val syncManager = mock<SyncManager> {
        on { isLoggedIn() } doReturn true
    }
    private val settings = mock<Settings>()
    private val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED, coroutineRule.testDispatcher)
    private val activity = mock<Activity>()

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.PLAY_IN_APP_MESSAGES, true)
        setSubscriptionPlatform(null)
    }

    @Test
    fun `show messages for a signed in user without a subscription`() = runTest {
        playInAppMessages().show(activity)

        assertEquals(1, listener.calls)
    }

    @Test
    fun `show messages for a Play subscriber`() = runTest {
        setSubscriptionPlatform(SubscriptionPlatform.Android)

        playInAppMessages().show(activity)

        assertEquals(1, listener.calls)
    }

    @Test
    fun `do not show messages when the feature is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAY_IN_APP_MESSAGES, false)

        playInAppMessages().show(activity)

        assertEquals(0, listener.calls)
    }

    @Test
    fun `do not show messages when signed out`() = runTest {
        whenever(syncManager.isLoggedIn()).doReturn(false)

        playInAppMessages().show(activity)

        assertEquals(0, listener.calls)
    }

    @Test
    fun `do not show messages for iOS, web or gift subscribers`() = runTest {
        listOf(SubscriptionPlatform.iOS, SubscriptionPlatform.Web, SubscriptionPlatform.Gift).forEach { platform ->
            setSubscriptionPlatform(platform)

            playInAppMessages().show(activity)
        }

        assertEquals(0, listener.calls)
    }

    @Test
    fun `show messages once per foreground`() = runTest {
        val playInAppMessages = playInAppMessages()

        playInAppMessages.show(activity)
        playInAppMessages.show(activity)
        assertEquals(1, listener.calls)

        lifecycleOwner.currentState = Lifecycle.State.CREATED
        lifecycleOwner.currentState = Lifecycle.State.RESUMED
        playInAppMessages.show(activity)
        assertEquals(2, listener.calls)
    }

    @Test
    fun `do not show messages again while a previous request is pending`() = runTest {
        val pendingDataSource = mock<PaymentDataSource> {
            on { showInAppMessages(any()) } doSuspendableAnswer { awaitCancellation() }
        }
        paymentClient = PaymentClient.test(pendingDataSource, listener)
        val playInAppMessages = playInAppMessages()

        playInAppMessages.show(activity)
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        lifecycleOwner.currentState = Lifecycle.State.RESUMED
        playInAppMessages.show(activity)

        verifyBlocking(pendingDataSource, times(1)) { showInAppMessages(activity) }
    }

    @Test
    fun `refresh the subscription twice when its status is updated`() = runTest {
        dataSource.inAppMessageResult = InAppMessageResult.SubscriptionStatusUpdated

        playInAppMessages().show(activity)
        runCurrent()
        verify(subscriptionManager, times(1)).fetchFreshSubscription()

        advanceTimeBy(5_001)
        verify(subscriptionManager, times(2)).fetchFreshSubscription()
    }

    @Test
    fun `do not refresh the subscription when no action is needed`() = runTest {
        playInAppMessages().show(activity)
        advanceTimeBy(10_000)

        verify(subscriptionManager, never()).fetchFreshSubscription()
    }

    @Test
    fun `do not refresh the subscription when showing messages fails`() = runTest {
        dataSource.inAppMessageResultCode = PaymentResultCode.ServiceUnavailable

        playInAppMessages().show(activity)
        advanceTimeBy(10_000)

        verify(subscriptionManager, never()).fetchFreshSubscription()
    }

    private fun TestScope.playInAppMessages() = PlayInAppMessages(
        paymentClient = paymentClient,
        subscriptionManager = subscriptionManager,
        syncManager = syncManager,
        settings = settings,
        scope = backgroundScope,
        processLifecycleOwner = lifecycleOwner,
    )

    private fun setSubscriptionPlatform(platform: SubscriptionPlatform?) {
        val subscription = platform?.let { Subscription.PlusPreview.copy(platform = it) }
        val membership = Membership.Empty.copy(subscription = subscription)
        whenever(settings.cachedMembership).doReturn(UserSetting.Mock(membership, mock()))
    }

    private class RecordingListener : PaymentClient.Listener {
        var calls = 0
            private set

        override fun onInAppMessagesShown(result: PaymentResult<InAppMessageResult>) {
            calls++
        }
    }
}
