package au.com.shiftyjelly.pocketcasts.payment

import androidx.annotation.Keep
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule.RecurrenceMode
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
    val schedule: PricingSchedule,
)

data class Price(
    val amount: BigDecimal,
    val currencyCode: String,
    val formattedPrice: String,
)

data class PricingSchedule(
    val recurrenceMode: RecurrenceMode,
    val period: PricingSchedule.Period,
    val periodCount: Int,
) {
    enum class Period {
        Daily,
        Weekly,
        Monthly,
        Yearly,
    }

    sealed interface RecurrenceMode {
        data object NonRecurring : RecurrenceMode

        @JvmInline
        value class Recurring(
            val value: Int,
        ) : RecurrenceMode

        data object Infinite : RecurrenceMode
    }
}

@ConsistentCopyVisibility
data class SubscriptionPlans private constructor(
    private val plans: Map<SubscriptionPlan.Key, PaymentResult<SubscriptionPlan>>,
) {
    fun getBasePlan(
        tier: SubscriptionTier,
        billingCycle: BillingCycle,
    ): SubscriptionPlan.Base {
        val key = SubscriptionPlan.Key(tier, billingCycle, offer = null)
        // This is a safe cast because constructor is private and we validate data in the create function
        return plans.getValue(key).getOrNull() as SubscriptionPlan.Base
    }

    fun findOfferPlan(
        tier: SubscriptionTier,
        billingCycle: BillingCycle,
        offer: SubscriptionOffer,
    ): PaymentResult<SubscriptionPlan.WithOffer> {
        val key = SubscriptionPlan.Key(tier, billingCycle, offer)
        // This is a safe cast because constructor is private and we validate data in the create function
        @Suppress("UNCHECKED_CAST")
        return plans.getValue(key) as PaymentResult<SubscriptionPlan.WithOffer>
    }

    companion object {
        val Preview get() = SubscriptionPlans.create(FakePaymentDataSource.DefaultLoadedProducts).getOrNull()!!

        private val basePlanKeys = SubscriptionTier.entries.flatMap { tier ->
            BillingCycle.entries.map { billingCycle ->
                SubscriptionPlan.Key(tier, billingCycle, offer = null)
            }
        }

        private val offerPlanKeys = SubscriptionTier.entries.flatMap { tier ->
            BillingCycle.entries.flatMap { billingCycle ->
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
                    val pricingPhases = product.pricingPlans.offerPlans.singleOrNull { it.offerId == key.offerId }?.pricingPhases
                    val infinitePricingPhase = pricingPhases?.singleOrNull { it.schedule.recurrenceMode == RecurrenceMode.Infinite }
                    infinitePricingPhase != null && infinitePricingPhase == pricingPhases.last()
                } else {
                    val pricingPhase = product.pricingPlans.basePlan.pricingPhases.singleOrNull()
                    pricingPhase?.schedule?.recurrenceMode == RecurrenceMode.Infinite
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
    val billingCycle: BillingCycle
    val offer: SubscriptionOffer?

    val productId get() = key.productId
    val basePlanId get() = key.basePlanId
    val offerId get() = key.offerId
    val recurringPrice get() = when (this) {
        is Base -> pricingPhase.price
        is WithOffer -> pricingPhases.last().price
    }

    data class Base(
        override val name: String,
        override val tier: SubscriptionTier,
        override val billingCycle: BillingCycle,
        val pricingPhase: PricingPhase,
    ) : SubscriptionPlan {
        override val offer get() = null
        override val key get() = SubscriptionPlan.Key(tier, billingCycle, offer = null)
    }

    data class WithOffer(
        override val name: String,
        override val tier: SubscriptionTier,
        override val billingCycle: BillingCycle,
        override val offer: SubscriptionOffer,
        val pricingPhases: List<PricingPhase>,
    ) : SubscriptionPlan {
        override val key get() = SubscriptionPlan.Key(tier, billingCycle, offer)
    }

    data class Key(
        val tier: SubscriptionTier,
        val billingCycle: BillingCycle,
        val offer: SubscriptionOffer?,
    ) {
        val productId = SubscriptionPlan.productId(tier, billingCycle)
        val basePlanId = SubscriptionPlan.basePlanId(tier, billingCycle)
        val offerId = offer?.offerId(tier, billingCycle)
    }

    companion object {
        const val PLUS_MONTHLY_PRODUCT_ID = "com.pocketcasts.plus.monthly"
        const val PLUS_YEARLY_PRODUCT_ID = "com.pocketcasts.plus.yearly"
        const val PATRON_MONTHLY_PRODUCT_ID = "com.pocketcasts.monthly.patron"
        const val PATRON_YEARLY_PRODUCT_ID = "com.pocketcasts.yearly.patron"

        val PlusMonthlyPreview get() = SubscriptionPlans.Preview.getBasePlan(SubscriptionTier.Plus, BillingCycle.Monthly)
        val PlusYearlyPreview get() = SubscriptionPlans.Preview.getBasePlan(SubscriptionTier.Plus, BillingCycle.Yearly)
        val PatronMonthlyPreview get() = SubscriptionPlans.Preview.getBasePlan(SubscriptionTier.Patron, BillingCycle.Monthly)
        val PatronYearlyPreview get() = SubscriptionPlans.Preview.getBasePlan(SubscriptionTier.Patron, BillingCycle.Yearly)

        fun productId(
            tier: SubscriptionTier,
            billingCycle: BillingCycle,
        ) = when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> PLUS_MONTHLY_PRODUCT_ID
                BillingCycle.Yearly -> PLUS_YEARLY_PRODUCT_ID
            }

            SubscriptionTier.Patron -> when (billingCycle) {
                BillingCycle.Monthly -> PATRON_MONTHLY_PRODUCT_ID
                BillingCycle.Yearly -> PATRON_YEARLY_PRODUCT_ID
            }
        }

        fun basePlanId(
            tier: SubscriptionTier,
            billingCycle: BillingCycle,
        ) = when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> "p1m"
                BillingCycle.Yearly -> "p1y"
            }

            SubscriptionTier.Patron -> when (billingCycle) {
                BillingCycle.Monthly -> "patron-monthly"
                BillingCycle.Yearly -> "patron-yearly"
            }
        }
    }
}

@Keep
enum class SubscriptionTier(
    val analyticsValue: String,
) {
    Plus(
        analyticsValue = "plus",
    ),
    Patron(
        analyticsValue = "patron",
    ),
}

@Keep
enum class BillingCycle(
    val analyticsValue: String,
) {
    Monthly(
        analyticsValue = "monthly",
    ),
    Yearly(
        analyticsValue = "yearly",
    ),
}

enum class SubscriptionOffer(
    val analyticsValue: String,
) {
    IntroOffer(
        analyticsValue = "intro_offer",
    ),
    Trial(
        analyticsValue = "free_trial",
    ),
    Referral(
        analyticsValue = "referral",
    ),
    Winback(
        analyticsValue = "winback",
    ),
    ;

    fun offerId(
        tier: SubscriptionTier,
        billingCycle: BillingCycle,
    ) = when (this) {
        IntroOffer -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> null
                BillingCycle.Yearly -> "plus-yearly-intro-50percent"
            }

            SubscriptionTier.Patron -> null
        }
        Trial -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> null
                BillingCycle.Yearly -> "plus-yearly-trial-30days"
            }

            SubscriptionTier.Patron -> null
        }

        Referral -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> null
                BillingCycle.Yearly -> "plus-yearly-referral-two-months-free"
            }

            SubscriptionTier.Patron -> null
        }

        Winback -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> "plus-monthly-winback"
                BillingCycle.Yearly -> "plus-yearly-winback"
            }

            SubscriptionTier.Patron -> when (billingCycle) {
                BillingCycle.Monthly -> "patron-monthly-winback"
                BillingCycle.Yearly -> "patron-yearly-winback"
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

data class AcknowledgedSubscription(
    val orderId: String,
    val tier: SubscriptionTier,
    val billingCycle: BillingCycle,
    val isAutoRenewing: Boolean,
) {
    val productId get() = SubscriptionPlan.productId(tier, billingCycle)
}
