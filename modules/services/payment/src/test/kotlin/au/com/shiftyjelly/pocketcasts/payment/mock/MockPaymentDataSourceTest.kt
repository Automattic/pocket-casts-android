package au.com.shiftyjelly.pocketcasts.payment.mock

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.PurchaseResult
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock

class MockPaymentDataSourceTest {
    private var config = MockBillingConfig(isEnabled = true)
    private val dataSource = MockPaymentDataSource { config }
    private val client = PaymentClient.test(dataSource)

    private val introOfferKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.IntroOffer)

    @Test
    fun `all offers are available by default`() = runTest {
        val plans = client.loadSubscriptionPlans().getOrNull()!!

        SubscriptionOffer.entries.forEach { offer ->
            assertNotNull(plans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, offer).getOrNull())
        }
    }

    @Test
    fun `intro offer has a half price first year`() = runTest {
        val plans = client.loadSubscriptionPlans().getOrNull()!!

        val introPlan = plans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.IntroOffer).getOrNull()!!

        val discountedPrice = introPlan.pricingPhases.first().price.amount
        val fullPrice = introPlan.pricingPhases.last().price.amount
        assertEquals(0, discountedPrice.multiply(2.toBigDecimal()).compareTo(fullPrice.setScale(discountedPrice.scale())))
    }

    @Test
    fun `ineligible offers are left out`() = runTest {
        config = config.copy(eligibleOffers = setOf(SubscriptionOffer.Trial))

        val plans = client.loadSubscriptionPlans().getOrNull()!!

        assertNull(plans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.IntroOffer).getOrNull())
        assertNotNull(plans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.Trial).getOrNull())
        assertNotNull(plans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Yearly))
    }

    @Test
    fun `products fail to load`() = runTest {
        config = config.copy(failProductsLoading = true)

        val result = dataSource.loadProducts()

        assertEquals(PaymentResultCode.ServiceUnavailable, (result as PaymentResult.Failure).code)
    }

    @Test
    fun `successful purchase`() = runTest {
        val result = client.purchaseSubscriptionPlan(introOfferKey, purchaseSource = "test", activity = mock())

        assertEquals(PurchaseResult.Purchased, result)
    }

    @Test
    fun `cancelled purchase`() = runTest {
        config = config.copy(purchaseOutcome = MockPurchaseOutcome.Cancelled)

        val result = client.purchaseSubscriptionPlan(introOfferKey, purchaseSource = "test", activity = mock())

        assertEquals(PurchaseResult.Cancelled, result)
    }

    @Test
    fun `failed purchase`() = runTest {
        config = config.copy(purchaseOutcome = MockPurchaseOutcome.Error)

        val result = client.purchaseSubscriptionPlan(introOfferKey, purchaseSource = "test", activity = mock())

        assertEquals(PurchaseResult.Failure(PaymentResultCode.Error), result)
    }

    @Test
    fun `purchasing an ineligible offer fails`() = runTest {
        config = config.copy(eligibleOffers = emptySet())

        val result = client.purchaseSubscriptionPlan(introOfferKey, purchaseSource = "test", activity = mock())

        assertEquals(PurchaseResult.Failure(PaymentResultCode.DeveloperError), result)
    }
}
