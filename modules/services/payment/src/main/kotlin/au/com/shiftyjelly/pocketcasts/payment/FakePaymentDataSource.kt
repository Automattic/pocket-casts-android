package au.com.shiftyjelly.pocketcasts.payment

import java.math.BigDecimal

class FakePaymentDataSource : PaymentDataSource {
    var customProductsResult: PaymentResult<List<Product>>? = null

    override suspend fun loadProducts(): PaymentResult<List<Product>> {
        return customProductsResult ?: PaymentResult.Success(KnownProducts)
    }
}

private val KnownProducts = SubscriptionTier.entries.flatMap { tier ->
    SubscriptionBillingCycle.entries.map { billingCycle ->
        Product(
            SubscriptionPlan.productId(tier, billingCycle),
            productName(tier, billingCycle),
            PricingPlans(
                PricingPlan.Base(
                    SubscriptionPlan.basePlanId(tier, billingCycle),
                    pricingPhases(tier, billingCycle, offer = null),
                    emptyList(),
                ),
                SubscriptionOffer.entries
                    .mapNotNull { offer -> offer.offerId(tier, billingCycle)?.let { offer to it } }
                    .map { (offer, offerId) ->
                        PricingPlan.Offer(
                            offerId,
                            SubscriptionPlan.basePlanId(tier, billingCycle),
                            pricingPhases(tier, billingCycle, offer),
                            emptyList(),
                        )
                    },
            ),
        )
    }
}

private fun productName(
    tier: SubscriptionTier,
    billingCycle: SubscriptionBillingCycle,
) = "$tier $billingCycle (Fake)"

private fun pricingPhases(
    tier: SubscriptionTier,
    billingCycle: SubscriptionBillingCycle,
    offer: SubscriptionOffer?,
): List<PricingPhase> = when (offer) {
    SubscriptionOffer.Trial -> when (billingCycle) {
        SubscriptionBillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusYearlyPricingPhase.withDiscount(priceFraction = 0.0),
                PlusYearlyPricingPhase,
            )

            SubscriptionTier.Patron -> emptyList()
        }

        SubscriptionBillingCycle.Monthly -> emptyList()
    }

    SubscriptionOffer.Referral -> when (billingCycle) {
        SubscriptionBillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusYearlyPricingPhase.withDiscount(priceFraction = 0.0, intervalCount = 2),
                PlusYearlyPricingPhase,
            )

            SubscriptionTier.Patron -> emptyList()
        }

        SubscriptionBillingCycle.Monthly -> emptyList()
    }

    SubscriptionOffer.Winback -> when (billingCycle) {
        SubscriptionBillingCycle.Monthly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusMonthlyPricingPhase.withDiscount(priceFraction = 0.5),
                PlusMonthlyPricingPhase,
            )

            SubscriptionTier.Patron -> listOf(
                PatronMonthlyPricingPhase.withDiscount(priceFraction = 0.5),
                PatronMonthlyPricingPhase,
            )
        }

        SubscriptionBillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusYearlyPricingPhase.withDiscount(priceFraction = 0.5, interval = BillingPeriod.Interval.Yearly),
                PlusYearlyPricingPhase,
            )

            SubscriptionTier.Patron -> listOf(
                PatronYearlyPricingPhase.withDiscount(priceFraction = 0.5, interval = BillingPeriod.Interval.Yearly),
                PatronYearlyPricingPhase,
            )
        }
    }

    null -> when (billingCycle) {
        SubscriptionBillingCycle.Monthly -> when (tier) {
            SubscriptionTier.Plus -> listOf(PlusMonthlyPricingPhase)
            SubscriptionTier.Patron -> listOf(PatronMonthlyPricingPhase)
        }

        SubscriptionBillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(PlusYearlyPricingPhase)
            SubscriptionTier.Patron -> listOf(PatronYearlyPricingPhase)
        }
    }
}

private val PlusMonthlyPricingPhase = PricingPhase(
    Price(3.99.toBigDecimal(), "USD", "$3.99"),
    BillingPeriod(BillingPeriod.Cycle.Infinite, BillingPeriod.Interval.Monthly, intervalCount = 0),
)
private val PlusYearlyPricingPhase = PricingPhase(
    Price(39.99.toBigDecimal(), "USD", "$39.99"),
    BillingPeriod(BillingPeriod.Cycle.Infinite, BillingPeriod.Interval.Monthly, intervalCount = 0),
)
private val PatronMonthlyPricingPhase = PricingPhase(
    Price(9.99.toBigDecimal(), "USD", "$9.99"),
    BillingPeriod(BillingPeriod.Cycle.Infinite, BillingPeriod.Interval.Monthly, intervalCount = 0),
)
private val PatronYearlyPricingPhase = PricingPhase(
    Price(99.99.toBigDecimal(), "USD", "$99.99"),
    BillingPeriod(BillingPeriod.Cycle.Infinite, BillingPeriod.Interval.Yearly, intervalCount = 0),
)

private fun PricingPhase.withDiscount(
    priceFraction: Double,
    cycle: BillingPeriod.Cycle = BillingPeriod.Cycle.Recurring(1),
    interval: BillingPeriod.Interval = BillingPeriod.Interval.Monthly,
    intervalCount: Int = 1,
): PricingPhase {
    val newAmount = price.amount.times(priceFraction.coerceIn(0.0..1.0).toBigDecimal())
    val newFormattedPrice = if (newAmount == BigDecimal.ZERO) "Free" else "$%.2f".format(newAmount.toDouble())
    return copy(
        price = price.copy(amount = newAmount, formattedPrice = newFormattedPrice),
        billingPeriod = BillingPeriod(cycle, interval, intervalCount),
    )
}
