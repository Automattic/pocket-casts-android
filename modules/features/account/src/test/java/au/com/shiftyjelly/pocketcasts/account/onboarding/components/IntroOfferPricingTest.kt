package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.Price
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntroOfferPricingTest {
    private val plans = SubscriptionPlans.Preview

    @Test
    fun `intro offer plan has intro and regular prices`() {
        val plan = plans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.IntroOffer).getOrNull()!!

        val pricing = plan.introOfferPricing!!

        assertEquals(plan.pricingPhases.first().price, pricing.introPrice)
        assertEquals(plan.pricingPhases.last().price, pricing.regularPrice)
        assertEquals(50, pricing.discountPercent)
    }

    @Test
    fun `intro offer that does not last exactly one year has no intro pricing`() {
        val plan = plans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.IntroOffer).getOrNull()!!
        val introPhase = plan.pricingPhases.first()
        val otherSchedules = listOf(
            introPhase.schedule.copy(recurrenceMode = PricingSchedule.RecurrenceMode.Recurring(2)),
            introPhase.schedule.copy(period = PricingSchedule.Period.Monthly, recurrenceMode = PricingSchedule.RecurrenceMode.Recurring(6)),
            introPhase.schedule.copy(periodCount = 2),
        )

        otherSchedules.forEach { schedule ->
            val otherPlan = plan.copy(pricingPhases = listOf(introPhase.copy(schedule = schedule), plan.pricingPhases.last()))
            assertNull("$schedule", otherPlan.introOfferPricing)
        }
    }

    @Test
    fun `trial plan has no intro pricing`() {
        val plan = plans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.Trial).getOrNull()!!

        assertNull(plan.introOfferPricing)
    }

    @Test
    fun `base plan has no intro pricing`() {
        assertNull(SubscriptionPlan.PlusYearlyPreview.introOfferPricing)
    }

    @Test
    fun `discount percent is rounded`() {
        val pricing = IntroOfferPricing(
            introPrice = Price(19.99.toBigDecimal(), "USD", "$19.99"),
            regularPrice = Price(39.99.toBigDecimal(), "USD", "$39.99"),
        )

        assertEquals(50, pricing.discountPercent)
    }

    @Test
    fun `free regular price has no discount`() {
        val pricing = IntroOfferPricing(
            introPrice = Price(0.toBigDecimal(), "USD", "Free"),
            regularPrice = Price(0.toBigDecimal(), "USD", "Free"),
        )

        assertEquals(0, pricing.discountPercent)
    }
}
