package au.com.shiftyjelly.pocketcasts.referrals

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule.RecurrenceMode
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import java.math.BigDecimal
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Test

class ReferralSubscriptionPlanTest {
    private val rawPlan = SubscriptionPlans.Preview.findOfferPlan(
        SubscriptionTier.Plus,
        BillingCycle.Yearly,
        SubscriptionOffer.Referral,
    ).getOrNull()!!

    @Test
    fun `create referral plan`() {
        val plan = ReferralSubscriptionPlan.create(rawPlan).getOrNull()

        assertNotNull(plan)
        assertEquals(BigDecimal.ZERO, plan?.freePricingPhase?.price?.amount)
        assertEquals(39.99.toBigDecimal(), plan?.paidPricingPhase?.price?.amount)
    }

    @Test
    fun `do not create plan with a wrong offer`() {
        val invalidRawPlan = rawPlan.copy(offer = SubscriptionOffer.Trial)

        val plan = ReferralSubscriptionPlan.create(invalidRawPlan).getOrNull()

        assertNull(plan)
    }

    @Test
    fun `do not create plan with too many pricing phases`() {
        val invalidRawPlan = rawPlan.copy(pricingPhases = rawPlan.pricingPhases + rawPlan.pricingPhases[0])

        val plan = ReferralSubscriptionPlan.create(invalidRawPlan).getOrNull()

        assertNull(plan)
    }

    @Test
    fun `do not create plan with too few pricing phases`() {
        val invalidRawPlan = rawPlan.copy(pricingPhases = rawPlan.pricingPhases.take(1))

        val plan = ReferralSubscriptionPlan.create(invalidRawPlan).getOrNull()

        assertNull(plan)
    }

    @Test
    fun `do not create plan with non recurring initial phase`() {
        val phases = rawPlan.pricingPhases.mapIndexed { index, phase ->
            if (index == 0) {
                phase.copy(schedule = phase.schedule.copy(recurrenceMode = RecurrenceMode.NonRecurring))
            } else {
                phase
            }
        }
        val invalidRawPlan = rawPlan.copy(pricingPhases = phases)

        val plan = ReferralSubscriptionPlan.create(invalidRawPlan).getOrNull()

        assertNull(plan)
    }

    @Test
    fun `do not create plan with infinite initial phase`() {
        val phases = rawPlan.pricingPhases.mapIndexed { index, phase ->
            if (index == 0) {
                phase.copy(schedule = phase.schedule.copy(recurrenceMode = RecurrenceMode.Infinite))
            } else {
                phase
            }
        }
        val invalidRawPlan = rawPlan.copy(pricingPhases = phases)

        val plan = ReferralSubscriptionPlan.create(invalidRawPlan).getOrNull()

        assertNull(plan)
    }

    @Test
    fun `do not create plan with recurring final phase`() {
        val phases = rawPlan.pricingPhases.mapIndexed { index, phase ->
            if (index == 1) {
                phase.copy(schedule = phase.schedule.copy(recurrenceMode = RecurrenceMode.Recurring(1)))
            } else {
                phase
            }
        }
        val invalidRawPlan = rawPlan.copy(pricingPhases = phases)

        val plan = ReferralSubscriptionPlan.create(invalidRawPlan).getOrNull()

        assertNull(plan)
    }

    @Test
    fun `do not create plan with non recurring final phase`() {
        val phases = rawPlan.pricingPhases.mapIndexed { index, phase ->
            if (index == 1) {
                phase.copy(schedule = phase.schedule.copy(recurrenceMode = RecurrenceMode.NonRecurring))
            } else {
                phase
            }
        }
        val invalidRawPlan = rawPlan.copy(pricingPhases = phases)

        val plan = ReferralSubscriptionPlan.create(invalidRawPlan).getOrNull()

        assertNull(plan)
    }
}
