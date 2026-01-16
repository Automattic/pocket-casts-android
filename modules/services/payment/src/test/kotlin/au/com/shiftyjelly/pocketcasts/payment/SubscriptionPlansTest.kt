package au.com.shiftyjelly.pocketcasts.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionPlansTest {
    private val initialPricingPhase = PricingPhase(
        Price(100.toBigDecimal(), "USD", "$10.00"),
        PricingSchedule(PricingSchedule.RecurrenceMode.Recurring(1), PricingSchedule.Period.Yearly, periodCount = 1),
    )
    private val infinitePricingPhase = PricingPhase(
        Price(100.toBigDecimal(), "USD", "$100.00"),
        PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Yearly, periodCount = 0),
    )

    private val products = SubscriptionTier.entries.flatMap { tier ->
        BillingCycle.entries.map { billingCycle ->
            Product(
                id = SubscriptionPlan.productId(tier, billingCycle),
                name = "$tier $billingCycle",
                pricingPlans = PricingPlans(
                    basePlan = PricingPlan.Base(
                        planId = SubscriptionPlan.basePlanId(tier, billingCycle),
                        pricingPhases = listOf(infinitePricingPhase),
                        tags = emptyList(),
                    ),
                    offerPlans = SubscriptionOffer.entries
                        .mapNotNull { offer -> offer.offerId(tier, billingCycle) }
                        .map { offerId ->
                            PricingPlan.Offer(
                                offerId = offerId,
                                planId = SubscriptionPlan.basePlanId(tier, billingCycle),
                                pricingPhases = listOf(initialPricingPhase, infinitePricingPhase),
                                tags = emptyList(),
                            )
                        },
                ),
            )
        }
    }

    @Test
    fun `create subscription plans`() {
        val plans = SubscriptionPlans.create(products).getOrNull()

        assertNotNull(plans)
    }

    @Test
    fun `get plus monthly base plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Monthly)

        assertEquals("Plus Monthly", plan.name)
        assertEquals(SubscriptionTier.Plus, plan.tier)
        assertEquals(BillingCycle.Monthly, plan.billingCycle)
    }

    @Test
    fun `get plus yearly base plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Yearly)

        assertEquals("Plus Yearly", plan.name)
        assertEquals(SubscriptionTier.Plus, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
    }

    @Test
    fun `get patron monthly base plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.getBasePlan(SubscriptionTier.Patron, BillingCycle.Monthly)

        assertEquals("Patron Monthly", plan.name)
        assertEquals(SubscriptionTier.Patron, plan.tier)
        assertEquals(BillingCycle.Monthly, plan.billingCycle)
    }

    @Test
    fun `get patron yearly base plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.getBasePlan(SubscriptionTier.Patron, BillingCycle.Yearly)

        assertEquals("Patron Yearly", plan.name)
        assertEquals(SubscriptionTier.Patron, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
    }

    @Test
    fun `find plus monthly winback plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.findOfferPlan(
            SubscriptionTier.Plus,
            BillingCycle.Monthly,
            SubscriptionOffer.Winback,
        ).getOrNull()!!

        assertEquals("Plus Monthly", plan.name)
        assertEquals(SubscriptionTier.Plus, plan.tier)
        assertEquals(BillingCycle.Monthly, plan.billingCycle)
        assertEquals(SubscriptionOffer.Winback, plan.offer)
    }

    @Test
    fun `find plus yearly winback plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.findOfferPlan(
            SubscriptionTier.Plus,
            BillingCycle.Yearly,
            SubscriptionOffer.Winback,
        ).getOrNull()!!

        assertEquals("Plus Yearly", plan.name)
        assertEquals(SubscriptionTier.Plus, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
        assertEquals(SubscriptionOffer.Winback, plan.offer)
    }

    @Test
    fun `find patron monthly winback plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.findOfferPlan(
            SubscriptionTier.Patron,
            BillingCycle.Monthly,
            SubscriptionOffer.Winback,
        ).getOrNull()!!

        assertEquals("Patron Monthly", plan.name)
        assertEquals(SubscriptionTier.Patron, plan.tier)
        assertEquals(BillingCycle.Monthly, plan.billingCycle)
        assertEquals(SubscriptionOffer.Winback, plan.offer)
    }

    @Test
    fun `find patron yearly winback plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.findOfferPlan(
            SubscriptionTier.Patron,
            BillingCycle.Yearly,
            SubscriptionOffer.Winback,
        ).getOrNull()!!

        assertEquals("Patron Yearly", plan.name)
        assertEquals(SubscriptionTier.Patron, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
        assertEquals(SubscriptionOffer.Winback, plan.offer)
    }

    @Test
    fun `find plus yearly referral plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.findOfferPlan(
            SubscriptionTier.Plus,
            BillingCycle.Yearly,
            SubscriptionOffer.Referral,
        ).getOrNull()!!

        assertEquals("Plus Yearly", plan.name)
        assertEquals(SubscriptionTier.Plus, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
        assertEquals(SubscriptionOffer.Referral, plan.offer)
    }

    @Test
    fun `find plus yearly trial plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.findOfferPlan(
            SubscriptionTier.Plus,
            BillingCycle.Yearly,
            SubscriptionOffer.Trial,
        ).getOrNull()!!

        assertEquals("Plus Yearly", plan.name)
        assertEquals(SubscriptionTier.Plus, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
        assertEquals(SubscriptionOffer.Trial, plan.offer)
    }

    @Test
    fun `find plus yearly intro offer plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.findOfferPlan(
            SubscriptionTier.Plus,
            BillingCycle.Yearly,
            SubscriptionOffer.IntroOffer,
        ).getOrNull()!!

        assertEquals("Plus Yearly", plan.name)
        assertEquals(SubscriptionTier.Plus, plan.tier)
        assertEquals(BillingCycle.Yearly, plan.billingCycle)
        assertEquals(SubscriptionOffer.IntroOffer, plan.offer)
    }

    @Test
    fun `do not find unknown offer plan`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.findOfferPlan(
            SubscriptionTier.Patron,
            BillingCycle.Monthly,
            SubscriptionOffer.Trial,
        ).getOrNull()

        assertNull(plan)
    }

    @Test
    fun `do not create plans when plus monthly product is missing`() {
        val products = products.filter { it.id != SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID }

        val plans = SubscriptionPlans.create(products).getOrNull()

        assertNull(plans)
    }

    @Test
    fun `do not create plans when plus yearly product is missing`() {
        val products = products.filter { it.id != SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID }

        val plans = SubscriptionPlans.create(products).getOrNull()

        assertNull(plans)
    }

    @Test
    fun `do not create plans when patron monthly product is missing`() {
        val products = products.filter { it.id != SubscriptionPlan.PATRON_MONTHLY_PRODUCT_ID }

        val plans = SubscriptionPlans.create(products).getOrNull()

        assertNull(plans)
    }

    @Test
    fun `do not create plans when patron yearly product is missing`() {
        val products = products.filter { it.id != SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID }

        val plans = SubscriptionPlans.create(products).getOrNull()

        assertNull(plans)
    }

    @Test
    fun `do not create plans when base plan has multiple pricing phases`() {
        val products = products.map { product ->
            if (product.id == SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID) {
                val basePlan = product.pricingPlans.basePlan.copy(pricingPhases = listOf(initialPricingPhase, infinitePricingPhase))
                val pricingPlans = product.pricingPlans.copy(basePlan = basePlan)
                product.copy(pricingPlans = pricingPlans)
            } else {
                product
            }
        }

        val plans = SubscriptionPlans.create(products).getOrNull()

        assertNull(plans)
    }

    @Test
    fun `do not create plans when base plan has multiple matching products`() {
        val products = products + products[0]

        val plans = SubscriptionPlans.create(products).getOrNull()

        assertNull(plans)
    }

    @Test
    fun `create plans when offer plan has multiple matching products`() {
        val products = products.map { product ->
            val offerPlans = product.pricingPlans.offerPlans
            val pricingPlans = product.pricingPlans.copy(offerPlans = offerPlans + offerPlans)
            product.copy(pricingPlans = pricingPlans)
        }

        val plans = SubscriptionPlans.create(products).getOrNull()

        assertNotNull(plans)
    }

    @Test
    fun `do not find offers when there are multiple matching ones`() {
        val products = products.map { product ->
            val offerPlans = product.pricingPlans.offerPlans
            val pricingPlans = product.pricingPlans.copy(offerPlans = offerPlans + offerPlans)
            product.copy(pricingPlans = pricingPlans)
        }
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.findOfferPlan(
            SubscriptionTier.Plus,
            BillingCycle.Yearly,
            SubscriptionOffer.Winback,
        ).getOrNull()

        assertNull(plan)
    }

    @Test
    fun `do not create offers when base has non infinte pricing phase`() {
        val products = products.map { product ->
            if (product.id == SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID) {
                val basePlan = product.pricingPlans.basePlan.copy(pricingPhases = listOf(initialPricingPhase))
                val pricingPlans = product.pricingPlans.copy(basePlan = basePlan)
                product.copy(pricingPlans = pricingPlans)
            } else {
                product
            }
        }

        val plans = SubscriptionPlans.create(products).getOrNull()

        assertNull(plans)
    }

    @Test
    fun `do not find offers when there are multiple infinite pricing phases`() {
        val products = products.map { product ->
            val offerPlans = product.pricingPlans.offerPlans.map { offerPlan ->
                offerPlan.copy(pricingPhases = offerPlan.pricingPhases + initialPricingPhase)
            }
            val pricingPlans = product.pricingPlans.copy(offerPlans = offerPlans)
            product.copy(pricingPlans = pricingPlans)
        }
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val plan = plans.findOfferPlan(
            SubscriptionTier.Plus,
            BillingCycle.Yearly,
            SubscriptionOffer.Winback,
        ).getOrNull()

        assertNull(plan)
    }

    @Test
    fun `plans have correct recurring price`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!
        val basePlans = SubscriptionTier.entries.flatMap { tier ->
            BillingCycle.entries.map { billingCycle ->
                plans.getBasePlan(tier, billingCycle)
            }
        }
        val offerPlans = SubscriptionOffer.entries.flatMap { offer ->
            basePlans.mapNotNull { basePlan ->
                plans.findOfferPlan(basePlan.tier, basePlan.billingCycle, offer).getOrNull()
            }
        }
        val allPlans = basePlans + offerPlans

        for (plan in allPlans) {
            assertEquals(plan.recurringPrice, infinitePricingPhase.price)
        }
    }

    @Test
    fun `create plans with installment product`() {
        val installmentPricingPhase = PricingPhase(
            Price(3.33.toBigDecimal(), "USD", "$3.33"),
            PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Monthly, periodCount = 12),
        )
        val installmentProduct = Product(
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
        val productsWithInstallment = products + installmentProduct

        val plans = SubscriptionPlans.create(productsWithInstallment).getOrNull()

        assertNotNull(plans)
        val installmentPlan = plans?.findInstallmentPlan(SubscriptionTier.Plus, BillingCycle.Yearly)?.getOrNull()
        assertNotNull(installmentPlan)
    }

    @Test
    fun `get installment plan when available`() {
        val installmentPricingPhase = PricingPhase(
            Price(3.33.toBigDecimal(), "USD", "$3.33"),
            PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Monthly, periodCount = 12),
        )
        val installmentProduct = Product(
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
        val productsWithInstallment = products + installmentProduct
        val plans = SubscriptionPlans.create(productsWithInstallment).getOrNull()!!

        val installmentPlan = plans.findInstallmentPlan(SubscriptionTier.Plus, BillingCycle.Yearly).getOrNull()!!

        assertNotNull(installmentPlan)
        assertEquals("Plus Yearly Installment", installmentPlan.name)
        assertEquals(SubscriptionTier.Plus, installmentPlan.tier)
        assertEquals(BillingCycle.Yearly, installmentPlan.billingCycle)
        assertTrue(installmentPlan.isInstallment)
        assertEquals(3.33.toBigDecimal(), installmentPlan.pricingPhase.price.amount)
    }

    @Test
    fun `get failure when installment plan not available`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val result = plans.findInstallmentPlan(SubscriptionTier.Plus, BillingCycle.Yearly)

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentResultCode.ItemUnavailable, (result as PaymentResult.Failure).code)
    }

    @Test
    fun `create plans succeeds even without installment product`() {
        val plans = SubscriptionPlans.create(products).getOrNull()

        assertNotNull(plans)
        val result = plans?.findInstallmentPlan(SubscriptionTier.Plus, BillingCycle.Yearly)
        assertTrue(result is PaymentResult.Failure)
    }

    @Test
    fun `get regular yearly plan when installment not available`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val regularPlan = plans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Yearly)

        assertNotNull(regularPlan)
        assertFalse(regularPlan.isInstallment)
    }

    @Test
    fun `installment plan contains installment details`() {
        val installmentPricingPhase = PricingPhase(
            Price(3.33.toBigDecimal(), "USD", "$3.33"),
            PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Monthly, periodCount = 12),
        )
        val installmentProduct = Product(
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
        val productsWithInstallment = products + installmentProduct
        val plans = SubscriptionPlans.create(productsWithInstallment).getOrNull()!!

        val installmentPlan = plans.findInstallmentPlan(SubscriptionTier.Plus, BillingCycle.Yearly).getOrNull()!!

        assertNotNull(installmentPlan)
        assertNotNull(installmentPlan.pricingPhase)

        // Verify installment details are preserved on the SubscriptionPlan.Base
        assertNotNull(installmentPlan.installmentPlanDetails)
        assertEquals(12, installmentPlan.installmentPlanDetails?.commitmentPaymentsCount)
        assertEquals(0, installmentPlan.installmentPlanDetails?.subsequentCommitmentPaymentsCount)
    }

    @Test
    fun `regular plans do not have installment details`() {
        val plans = SubscriptionPlans.create(products).getOrNull()!!

        val regularYearlyPlan = plans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Yearly)
        val regularMonthlyPlan = plans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Monthly)

        // Regular plans should not have installment details
        assertNull(regularYearlyPlan.installmentPlanDetails)
        assertFalse(regularYearlyPlan.isInstallment)
        assertNull(regularMonthlyPlan.installmentPlanDetails)
        assertFalse(regularMonthlyPlan.isInstallment)
    }

    @Test
    fun `reject installment product without installment details`() {
        val installmentPricingPhase = PricingPhase(
            Price(3.33.toBigDecimal(), "USD", "$3.33"),
            PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Monthly, periodCount = 12),
        )
        val invalidInstallmentProduct = Product(
            id = SubscriptionPlan.PLUS_YEARLY_INSTALLMENT_PRODUCT_ID,
            name = "Plus Yearly Installment",
            pricingPlans = PricingPlans(
                basePlan = PricingPlan.Base(
                    planId = "p1y-installment",
                    pricingPhases = listOf(installmentPricingPhase),
                    tags = emptyList(),
                    installmentPlanDetails = null,
                ),
                offerPlans = emptyList(),
            ),
        )
        val productsWithInvalidInstallment = products + invalidInstallmentProduct
        val result = SubscriptionPlans.create(productsWithInvalidInstallment)
        assertTrue(result is PaymentResult.Failure)
        val failure = result as PaymentResult.Failure
        assertEquals(PaymentResultCode.DeveloperError, failure.code)
        assertTrue(failure.message.contains("installmentPlanDetails"))
    }
}
