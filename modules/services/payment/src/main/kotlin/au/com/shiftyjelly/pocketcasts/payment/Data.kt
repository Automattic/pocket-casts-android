package au.com.shiftyjelly.pocketcasts.payment

import java.math.BigDecimal

data class Product(
    val id: String,
    val name: String,
    val pricingPlans: PricingPlans,
)

data class PricingPlans(
    val basePlan: PricingPlan.Base,
    val offerPlans: List<PricingPlan.Offer>,
)

sealed interface PricingPlan {
    val planId: String
    val pricingPhases: List<PricingPhase>
    val tags: List<String>

    data class Base(
        override val planId: String,
        override val pricingPhases: List<PricingPhase>,
        override val tags: List<String>,
    ) : PricingPlan

    data class Offer(
        val offerId: String,
        override val planId: String,
        override val pricingPhases: List<PricingPhase>,
        override val tags: List<String>,
    ) : PricingPlan
}

data class PricingPhase(
    val price: Price,
    val billingPeriod: BillingPeriod,
)

data class Price(
    val amount: BigDecimal,
    val currencyCode: String,
    val formattedPrice: String,
)

data class BillingPeriod(
    val cycle: BillingPeriod.Cycle,
    val interval: BillingPeriod.Interval,
    val intervalCount: Int,
) {
    enum class Interval {
        Weekly,
        Monthly,
        Yearly,
    }

    sealed interface Cycle {
        data object NonRecurring : Cycle

        @JvmInline
        value class Recurring(
            val value: Int,
        ) : Cycle

        data object Infinite : Cycle
    }
}
