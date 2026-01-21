package au.com.shiftyjelly.pocketcasts.utils.extensions

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.InstallmentPlanDetails
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.Price
import au.com.shiftyjelly.pocketcasts.payment.PricingPhase
import au.com.shiftyjelly.pocketcasts.payment.PricingPlan
import au.com.shiftyjelly.pocketcasts.payment.PricingPlans
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule
import au.com.shiftyjelly.pocketcasts.payment.Product
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PaymentExtensionsTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val infinitePricingPhase = PricingPhase(
        Price(100.toBigDecimal(), "USD", "$100.00"),
        PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Yearly, periodCount = 0),
    )

    private val installmentPricingPhase = PricingPhase(
        Price(8.33.toBigDecimal(), "USD", "$8.33"),
        PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Monthly, periodCount = 12),
    )

    private val baseProducts = SubscriptionTier.entries.flatMap { tier ->
        BillingCycle.entries.mapNotNull { billingCycle ->
            val productId = SubscriptionPlan.productId(tier, billingCycle) ?: return@mapNotNull null
            val basePlanId = SubscriptionPlan.basePlanId(tier, billingCycle) ?: return@mapNotNull null
            Product(
                id = productId,
                name = "$tier $billingCycle",
                pricingPlans = PricingPlans(
                    basePlan = PricingPlan.Base(
                        planId = basePlanId,
                        pricingPhases = listOf(infinitePricingPhase),
                        tags = emptyList(),
                    ),
                    offerPlans = emptyList(),
                ),
            )
        }
    }

    private val installmentProduct = Product(
        id = SubscriptionPlan.PLUS_YEARLY_INSTALLMENT_PRODUCT_ID,
        name = "Plus Yearly Installment",
        pricingPlans = PricingPlans(
            basePlan = PricingPlan.Base(
                planId = "p1y-installment",
                pricingPhases = listOf(installmentPricingPhase),
                tags = emptyList(),
                installmentPlanDetails = InstallmentPlanDetails(
                    commitmentPaymentsCount = 12,
                    subsequentCommitmentPaymentsCount = 0,
                ),
            ),
            offerPlans = emptyList(),
        ),
    )

    @Test
    fun `returns regular yearly plan when feature flag is disabled for Plus tier`() {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, false)
        val productsWithInstallment = baseProducts + installmentProduct
        val result = SubscriptionPlans.create(productsWithInstallment)
        assertNotNull(result)
        assertTrue(result is PaymentResult.Success)
        val plans = (result as PaymentResult.Success).value

        val plan = plans.getYearlyPlanWithFeatureFlag(SubscriptionTier.Plus)

        assertEquals("Plus Yearly", plan.name)
        assertEquals(SubscriptionTier.Plus, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
        assertFalse(plan.isInstallment)
        assertEquals(infinitePricingPhase.price, plan.recurringPrice)
    }

    @Test
    fun `returns regular yearly plan when feature flag is disabled for Patron tier`() {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, false)
        val plans = SubscriptionPlans.create(baseProducts).getOrNull()!!
        val plan = plans.getYearlyPlanWithFeatureFlag(SubscriptionTier.Patron)

        assertEquals("Patron Yearly", plan.name)
        assertEquals(SubscriptionTier.Patron, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
        assertFalse(plan.isInstallment)
        assertEquals(infinitePricingPhase.price, plan.recurringPrice)
    }

    @Test
    fun `returns installment plan when feature flag is enabled and installment available for Plus tier`() {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, true)
        val productsWithInstallment = baseProducts + installmentProduct
        val plans = SubscriptionPlans.create(productsWithInstallment).getOrNull()!!
        val plan = plans.getYearlyPlanWithFeatureFlag(SubscriptionTier.Plus)

        assertEquals("Plus Yearly Installment", plan.name)
        assertEquals(SubscriptionTier.Plus, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
        assertTrue(plan.isInstallment)
        assertEquals(installmentPricingPhase.price, plan.recurringPrice)
        assertEquals(12, plan.installmentPlanDetails?.commitmentPaymentsCount)
    }

    @Test
    fun `returns regular yearly plan when feature flag is enabled but installment not available for Plus tier`() {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, true)
        val plans = SubscriptionPlans.create(baseProducts).getOrNull()!!

        val plan = plans.getYearlyPlanWithFeatureFlag(SubscriptionTier.Plus)

        assertEquals("Plus Yearly", plan.name)
        assertEquals(SubscriptionTier.Plus, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
        assertFalse(plan.isInstallment)
        assertEquals(infinitePricingPhase.price, plan.recurringPrice)
    }

    @Test
    fun `returns regular yearly plan when feature flag is enabled for Patron tier`() {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, true)
        val productsWithInstallment = baseProducts + installmentProduct
        val plans = SubscriptionPlans.create(productsWithInstallment).getOrNull()!!

        val plan = plans.getYearlyPlanWithFeatureFlag(SubscriptionTier.Patron)

        assertEquals("Patron Yearly", plan.name)
        assertEquals(SubscriptionTier.Patron, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
        assertFalse(plan.isInstallment)
        assertEquals(infinitePricingPhase.price, plan.recurringPrice)
    }

    @Test
    fun `installment plan is only available for Plus yearly subscription`() {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, true)
        val productsWithInstallment = baseProducts + installmentProduct
        val plans = SubscriptionPlans.create(productsWithInstallment).getOrNull()!!

        // Plus Yearly should get installment
        val plusYearlyPlan = plans.getYearlyPlanWithFeatureFlag(SubscriptionTier.Plus)
        assertTrue(plusYearlyPlan.isInstallment)

        // Patron Yearly should NOT get installment
        val patronYearlyPlan = plans.getYearlyPlanWithFeatureFlag(SubscriptionTier.Patron)
        assertFalse(patronYearlyPlan.isInstallment)
    }
}
