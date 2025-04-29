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

class SubscriptionPlans private constructor(
    private val plans: Map<SubscriptionPlan.Key, PaymentResult<SubscriptionPlan>>,
) {
    fun getBasePlan(
        tier: SubscriptionTier,
        billingCycle: SubscriptionBillingCycle,
    ): SubscriptionPlan.Base {
        val key = SubscriptionPlan.Key(tier, billingCycle, offer = null)
        // This is a safe cast because constructor is private and we validate data in the create function
        return plans.getValue(key).getOrNull() as SubscriptionPlan.Base
    }

    fun findOfferPlan(
        tier: SubscriptionTier,
        billingCycle: SubscriptionBillingCycle,
        offer: SubscriptionOffer,
    ): PaymentResult<SubscriptionPlan.WithOffer> {
        val key = SubscriptionPlan.Key(tier, billingCycle, offer)
        // This is a safe cast because constructor is private and we validate data in the create function
        @Suppress("UNCHECKED_CAST")
        return plans.getValue(key) as PaymentResult<SubscriptionPlan.WithOffer>
    }

    override fun equals(other: Any?) = (other === this) || (other is SubscriptionPlans && other.plans == this.plans)

    override fun hashCode() = plans.hashCode()

    override fun toString() = "SubscriptionPlans(plans=$plans)"

    companion object {
        private val basePlanKeys = SubscriptionTier.entries.flatMap { tier ->
            SubscriptionBillingCycle.entries.map { billingCycle ->
                SubscriptionPlan.Key(tier, billingCycle, offer = null)
            }
        }

        private val offerPlanKeys = SubscriptionTier.entries.flatMap { tier ->
            SubscriptionBillingCycle.entries.flatMap { billingCycle ->
                SubscriptionOffer.entries.map { offer ->
                    SubscriptionPlan.Key(tier, billingCycle, offer)
                }
            }
        }

        fun create(products: List<Product>): PaymentResult<SubscriptionPlans> {
            val basePlans = basePlanKeys.associateWith { key -> products.findMatchingSubscriptionPlan(key) }
            val basePlanFailure = basePlans.values
                .filterIsInstance<PaymentResult.Failure>()
                .firstOrNull()
            if (basePlanFailure != null) {
                return basePlanFailure
            }
            val offerPlans = offerPlanKeys.associateWith { key -> products.findMatchingSubscriptionPlan(key) }

            return PaymentResult.Success(SubscriptionPlans(basePlans + offerPlans))
        }

        private fun List<Product>.findMatchingSubscriptionPlan(key: SubscriptionPlan.Key): PaymentResult<SubscriptionPlan> {
            val matchingProducts = findMatchingProducts(key)
            return when (matchingProducts.size) {
                1 -> PaymentResult.Success(
                    if (key.offer != null) {
                        matchingProducts[0].toOfferSubscriptionPlan(key)
                    } else {
                        matchingProducts[0].toBaseSubscriptionPlan(key)
                    },
                )

                0 -> PaymentResult.Failure(PaymentResultCode.DeveloperError, "No matching product found for $key")
                else -> PaymentResult.Failure(PaymentResultCode.DeveloperError, "Multiple matching products found for $key. $matchingProducts")
            }
        }

        private fun List<Product>.findMatchingProducts(key: SubscriptionPlan.Key): List<Product> {
            return filter { product ->
                val offerCondition = if (key.offer != null) {
                    product.pricingPlans.offerPlans.singleOrNull { it.offerId == key.offerId } != null
                } else {
                    product.pricingPlans.basePlan.pricingPhases.size == 1
                }
                product.id == key.productId && product.pricingPlans.basePlan.planId == key.basePlanId && offerCondition
            }
        }

        private fun Product.toBaseSubscriptionPlan(key: SubscriptionPlan.Key): SubscriptionPlan.Base {
            return SubscriptionPlan.Base(
                name,
                key.tier,
                key.billingCycle,
                pricingPlans.basePlan.pricingPhases[0],
            )
        }

        private fun Product.toOfferSubscriptionPlan(key: SubscriptionPlan.Key): SubscriptionPlan.WithOffer {
            checkNotNull(key.offer)
            val matchingPricingPhases = pricingPlans.offerPlans.single { it.offerId == key.offerId }.pricingPhases
            return SubscriptionPlan.WithOffer(
                name,
                key.tier,
                key.billingCycle,
                key.offer,
                matchingPricingPhases,
            )
        }
    }
}

sealed interface SubscriptionPlan {
    val name: String
    val key: SubscriptionPlan.Key
    val tier: SubscriptionTier
    val billingCycle: SubscriptionBillingCycle
    val offer: SubscriptionOffer?

    val productId get() = key.productId
    val basePlanId get() = key.basePlanId
    val offerId get() = key.offerId

    data class Base(
        override val name: String,
        override val tier: SubscriptionTier,
        override val billingCycle: SubscriptionBillingCycle,
        val pricingPhase: PricingPhase,
    ) : SubscriptionPlan {
        override val offer get() = null
        override val key get() = SubscriptionPlan.Key(tier, billingCycle, offer = null)
    }

    data class WithOffer(
        override val name: String,
        override val tier: SubscriptionTier,
        override val billingCycle: SubscriptionBillingCycle,
        override val offer: SubscriptionOffer,
        val pricingPhases: List<PricingPhase>,
    ) : SubscriptionPlan {
        override val key get() = SubscriptionPlan.Key(tier, billingCycle, offer)
    }

    data class Key(
        val tier: SubscriptionTier,
        val billingCycle: SubscriptionBillingCycle,
        val offer: SubscriptionOffer?,
    ) {
        val productId = SubscriptionPlan.productId(tier, billingCycle)
        val basePlanId = SubscriptionPlan.basePlanId(tier, billingCycle)
        val offerId = offer?.offerId(tier, billingCycle)
    }

    companion object {
        fun productId(
            tier: SubscriptionTier,
            billingCycle: SubscriptionBillingCycle,
        ) = when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                SubscriptionBillingCycle.Monthly -> "com.pocketcasts.plus.monthly"
                SubscriptionBillingCycle.Yearly -> "com.pocketcasts.plus.yearly"
            }

            SubscriptionTier.Patron -> when (billingCycle) {
                SubscriptionBillingCycle.Monthly -> "com.pocketcasts.monthly.patron"
                SubscriptionBillingCycle.Yearly -> "com.pocketcasts.yearly.patron"
            }
        }

        fun basePlanId(
            tier: SubscriptionTier,
            billingCycle: SubscriptionBillingCycle,
        ) = when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                SubscriptionBillingCycle.Monthly -> "p1m"
                SubscriptionBillingCycle.Yearly -> "p1y"
            }

            SubscriptionTier.Patron -> when (billingCycle) {
                SubscriptionBillingCycle.Monthly -> "patron-monthly"
                SubscriptionBillingCycle.Yearly -> "patron-yearly"
            }
        }
    }
}

enum class SubscriptionTier {
    Plus,
    Patron,
}

enum class SubscriptionBillingCycle {
    Monthly,
    Yearly,
}

enum class SubscriptionOffer {
    Trial,
    Referral,
    Winback,
    ;

    fun offerId(
        tier: SubscriptionTier,
        billingCycle: SubscriptionBillingCycle,
    ) = when (this) {
        Trial -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                SubscriptionBillingCycle.Monthly -> null
                SubscriptionBillingCycle.Yearly -> "plus-yearly-trial-30days"
            }

            SubscriptionTier.Patron -> null
        }

        Referral -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                SubscriptionBillingCycle.Monthly -> null
                SubscriptionBillingCycle.Yearly -> "plus-yearly-referral-two-months-free"
            }

            SubscriptionTier.Patron -> null
        }

        Winback -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                SubscriptionBillingCycle.Monthly -> "plus-monthly-winback"
                SubscriptionBillingCycle.Yearly -> "plus-yearly-winback"
            }

            SubscriptionTier.Patron -> when (billingCycle) {
                SubscriptionBillingCycle.Monthly -> "patron-monthly-winback"
                SubscriptionBillingCycle.Yearly -> "patron-yearly-winback"
            }
        }
    }
}

data class Purchase(
    val state: PurchaseState,
    val token: String,
    val productIds: List<String>,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean,
)

sealed interface PurchaseState {
    val orderId: String?

    data object Pending : PurchaseState {
        override val orderId get() = null
    }

    data class Purchased(
        override val orderId: String,
    ) : PurchaseState

    data object Unspecified : PurchaseState {
        override val orderId get() = null
    }
}
