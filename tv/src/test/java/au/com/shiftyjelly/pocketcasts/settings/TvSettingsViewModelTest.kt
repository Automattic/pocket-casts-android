package au.com.shiftyjelly.pocketcasts.settings

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.ReadSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.AccountDetailsShowPrivacyPolicyEvent
import com.automattic.eventhorizon.AccountDetailsShowTosEvent
import com.automattic.eventhorizon.AccountDetailsSubscriptionEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsAppearanceUseEpisodeArtworkToggledEvent
import com.automattic.eventhorizon.SettingsGeneralShownEvent
import com.jakewharton.rxrelay2.BehaviorRelay
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class TvSettingsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val isLoggedIn = BehaviorRelay.createDefault(false)
    private val subscriptionFlow = MutableStateFlow<Subscription?>(null)
    private val artworkConfigurationFlow = MutableStateFlow(ArtworkConfiguration(useEpisodeArtwork = false))

    private val subscriptionSetting = mock<ReadSetting<Subscription?>> {
        on { flow } doReturn subscriptionFlow
        on { value } doReturn null
    }
    private val artworkConfigurationSetting = mock<UserSetting<ArtworkConfiguration>> {
        on { flow } doReturn artworkConfigurationFlow
        on { value } doAnswer { artworkConfigurationFlow.value }
        on { set(any(), any(), any(), any()) } doAnswer {
            artworkConfigurationFlow.value = it.getArgument(0)
            Unit
        }
    }
    private val settings = mock<Settings> {
        on { cachedSubscription } doReturn subscriptionSetting
        on { artworkConfiguration } doReturn artworkConfigurationSetting
    }
    private val syncManager = mock<SyncManager> {
        on { isLoggedIn() } doReturn false
        on { isLoggedInObservable } doReturn isLoggedIn
    }
    private val eventHorizon = mock<EventHorizon>()

    @Test
    fun `initial state reflects the current settings`() = runTest {
        createViewModel().uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isSignedIn)
            assertEquals(false, state.useEpisodeArtwork)
            assertEquals(null, state.subscription)
        }
    }

    @Test
    fun `state reflects sign in artwork and subscription changes`() = runTest {
        val subscription = subscription(platform = SubscriptionPlatform.Android)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)

            isLoggedIn.accept(true)
            assertEquals(true, awaitItem().isSignedIn)

            viewModel.setUseEpisodeArtwork(true)
            assertEquals(true, awaitItem().useEpisodeArtwork)

            subscriptionFlow.value = subscription
            assertEquals(subscription, awaitItem().subscription)
        }
    }

    @Test
    fun `setUseEpisodeArtwork updates the shared artwork setting and tracks the toggle event`() = runTest {
        createViewModel().setUseEpisodeArtwork(true)

        verify(artworkConfigurationSetting).set(eq(ArtworkConfiguration(useEpisodeArtwork = true)), eq(true), any(), any())
        verify(eventHorizon).track(SettingsAppearanceUseEpisodeArtworkToggledEvent(enabled = true))
    }

    @Test
    fun `showing the settings menu tracks the general shown event`() = runTest {
        createViewModel().trackSettingsShown()

        verify(eventHorizon).track(SettingsGeneralShownEvent)
    }

    @Test
    fun `showing the subscription info tracks the subscription event`() = runTest {
        createViewModel().trackSubscriptionShown()

        verify(eventHorizon).track(AccountDetailsSubscriptionEvent)
    }

    @Test
    fun `showing the privacy policy tracks the privacy policy event`() = runTest {
        createViewModel().trackPrivacyPolicyShown()

        verify(eventHorizon).track(AccountDetailsShowPrivacyPolicyEvent)
    }

    @Test
    fun `showing the terms of use tracks the terms of use event`() = runTest {
        createViewModel().trackTermsOfUseShown()

        verify(eventHorizon).track(AccountDetailsShowTosEvent)
    }

    private fun createViewModel() = TvSettingsViewModel(
        settings = settings,
        syncManager = syncManager,
        eventHorizon = eventHorizon,
    )

    private fun subscription(platform: SubscriptionPlatform) = Subscription(
        tier = SubscriptionTier.Plus,
        billingCycle = BillingCycle.Yearly,
        platform = platform,
        expiryDate = Instant.EPOCH,
        isAutoRenewing = true,
        giftDays = 0,
    )
}
