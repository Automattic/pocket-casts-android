package au.com.shiftyjelly.pocketcasts.referrals

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.Price
import au.com.shiftyjelly.pocketcasts.payment.PricingPhase
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule.Period
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule.RecurrenceMode
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import java.math.BigDecimal
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
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

    @Test
    fun `installment plan calculates properties correctly`() {
        // Create an installment plan with monthly billing
        val monthlyPrice = Price(
            amount = 4.99.toBigDecimal(),
            currencyCode = "USD",
            formattedPrice = "$4.99",
        )
        val paidPhase = PricingPhase(
            price = monthlyPrice,
            schedule = PricingSchedule(
                recurrenceMode = RecurrenceMode.Infinite,
                period = Period.Monthly,
                periodCount = 12,
            ),
        )
        val installmentPlan = rawPlan.copy(
            billingCycle = BillingCycle.Monthly,
            pricingPhases = listOf(rawPlan.pricingPhases[0], paidPhase),
            isInstallment = true,
        )

        val plan = ReferralSubscriptionPlan.create(installmentPlan).getOrNull()

        assertNotNull(plan)
        assertTrue(plan!!.isInstallment)
        assertEquals(12, plan.commitmentPaymentsCount)
        assertEquals(0, plan.totalCommitmentAmount.compareTo(59.88.toBigDecimal()))
        assertTrue(plan.formattedTotalCommitmentAmount.isNotEmpty())
        // Verify the formatted amount contains the expected value (currency formatting may vary by locale)
        assertTrue(plan.formattedTotalCommitmentAmount.contains("59.88"))
    }

    @Test
    fun `non-installment plan returns empty strings and zero for installment properties`() {
        val plan = ReferralSubscriptionPlan.create(rawPlan).getOrNull()

        assertNotNull(plan)
        assertEquals(false, plan!!.isInstallment)
        assertEquals(0, plan.commitmentPaymentsCount)
        assertEquals(0, plan.totalCommitmentAmount.compareTo(BigDecimal.ZERO))
        assertEquals("", plan.formattedTotalCommitmentAmount)
    }

    @Test
    fun `installment plan with non-monthly period returns zero commitment count`() {
        // Create an installment plan with yearly billing (not monthly)
        val yearlyPrice = Price(
            amount = 39.99.toBigDecimal(),
            currencyCode = "USD",
            formattedPrice = "$39.99",
        )
        val paidPhase = PricingPhase(
            price = yearlyPrice,
            schedule = PricingSchedule(
                recurrenceMode = RecurrenceMode.Infinite,
                period = Period.Yearly,
                periodCount = 2,
            ),
        )
        val installmentPlan = rawPlan.copy(
            billingCycle = BillingCycle.Yearly,
            pricingPhases = listOf(rawPlan.pricingPhases[0], paidPhase),
            isInstallment = true,
        )

        val plan = ReferralSubscriptionPlan.create(installmentPlan).getOrNull()

        assertNotNull(plan)
        assertTrue(plan!!.isInstallment)
        // commitmentPaymentsCount should be 0 because period is not Monthly
        assertEquals(0, plan.commitmentPaymentsCount)
        // totalCommitmentAmount should be 0 because commitmentPaymentsCount is 0
        assertEquals(0, plan.totalCommitmentAmount.compareTo(BigDecimal.ZERO))
        assertEquals("", plan.formattedTotalCommitmentAmount)
    }

    @Test
    fun `installment plan with zero price returns empty formatted amount`() {
        // Create an installment plan with zero price
        val zeroPrice = Price(
            amount = BigDecimal.ZERO,
            currencyCode = "USD",
            formattedPrice = "$0.00",
        )
        val paidPhase = PricingPhase(
            price = zeroPrice,
            schedule = PricingSchedule(
                recurrenceMode = RecurrenceMode.Infinite,
                period = Period.Monthly,
                periodCount = 12,
            ),
        )
        val installmentPlan = rawPlan.copy(
            billingCycle = BillingCycle.Monthly,
            pricingPhases = listOf(rawPlan.pricingPhases[0], paidPhase),
            isInstallment = true,
        )

        val plan = ReferralSubscriptionPlan.create(installmentPlan).getOrNull()

        assertNotNull(plan)
        assertTrue(plan!!.isInstallment)
        assertEquals(12, plan.commitmentPaymentsCount)
        assertEquals(0, plan.totalCommitmentAmount.compareTo(BigDecimal.ZERO))
        // formattedTotalCommitmentAmount should be empty because totalCommitmentAmount is zero
        assertEquals("", plan.formattedTotalCommitmentAmount)
    }
}
