package au.com.shiftyjelly.pocketcasts.payment.mock

import android.content.Context
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MockBillingSettingsTest {
    private val preferences = RuntimeEnvironment.getApplication().getSharedPreferences("mock-billing-test", Context.MODE_PRIVATE)

    private fun createSettings(isDeveloperBuild: Boolean, canUseRealBilling: Boolean) = MockBillingSettings(
        preferences = preferences,
        isDeveloperBuild = isDeveloperBuild,
        canUseRealBilling = canUseRealBilling,
    )

    @Test
    fun `release build never uses mocks`() {
        val settings = createSettings(isDeveloperBuild = false, canUseRealBilling = true)

        settings.setMockEnabled(true)

        assertFalse(settings.canToggleMock)
        assertFalse(settings.isMockEnabled)
    }

    @Test
    fun `prototype build defaults to real billing and can switch to mocks`() {
        val settings = createSettings(isDeveloperBuild = true, canUseRealBilling = true)

        assertTrue(settings.canToggleMock)
        assertFalse(settings.isMockEnabled)

        settings.setMockEnabled(true)

        assertTrue(settings.isMockEnabled)
    }

    @Test
    fun `non Play Store package always uses mocks`() {
        val settings = createSettings(isDeveloperBuild = true, canUseRealBilling = false)

        settings.setMockEnabled(false)

        assertFalse(settings.canToggleMock)
        assertTrue(settings.isMockEnabled)
    }

    @Test
    fun `mock options are saved and restored`() {
        val settings = createSettings(isDeveloperBuild = true, canUseRealBilling = false)

        settings.setOfferEligible(SubscriptionOffer.IntroOffer, isEligible = false)
        settings.setFailProductsLoading(true)
        settings.setPurchaseOutcome(MockPurchaseOutcome.Cancelled)

        val restoredConfig = createSettings(isDeveloperBuild = true, canUseRealBilling = false).config.value
        assertEquals(SubscriptionOffer.entries.toSet() - SubscriptionOffer.IntroOffer, restoredConfig.eligibleOffers)
        assertTrue(restoredConfig.failProductsLoading)
        assertEquals(MockPurchaseOutcome.Cancelled, restoredConfig.purchaseOutcome)
    }
}
