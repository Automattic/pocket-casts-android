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
        val installmentPlanDetails: InstallmentPlanDetails? = null,
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

data class InstallmentPlanDetails(
    val commitmentPaymentsCount: Int,
    val subsequentCommitmentPaymentsCount: Int,
)

@ConsistentCopyVisibility
data class SubscriptionPlans private constructor(
    private val plans: Map<SubscriptionPlan.Key, PaymentResult<SubscriptionPlan>>,
) {
    fun getBasePlan(
        tier: SubscriptionTier,
        billingCycle: BillingCycle,
    ): SubscriptionPlan.Base {
        val key = SubscriptionPlan.Key(tier, billingCycle, offer = null, isInstallment = false)
        // This is a safe cast because constructor is private and we validate data in the create function
        return plans.getValue(key).getOrNull() as SubscriptionPlan.Base
    }

    fun findInstallmentPlan(
        tier: SubscriptionTier,
        billingCycle: BillingCycle,
    ): PaymentResult<SubscriptionPlan.Base> {
        val key = SubscriptionPlan.Key(tier, billingCycle, offer = null, isInstallment = true)
        // This is a safe cast because constructor is private and we validate data in the create function
        @Suppress("UNCHECKED_CAST")
        return plans.getValue(key) as PaymentResult<SubscriptionPlan.Base>
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
            BillingCycle.entries.flatMap { billingCycle ->
                listOf(false, true).map { isInstallment ->
                    SubscriptionPlan.Key(tier, billingCycle, offer = null, isInstallment)
                }
            }
        }

        private val offerPlanKeys = basePlanKeys
            .filter { !it.isInstallment } // Installment plans don't support promotional offers
            .flatMap { baseKey ->
                SubscriptionOffer.entries.map { offer ->
                    baseKey.copy(offer = offer)
                }
            }

        fun create(products: List<Product>): PaymentResult<SubscriptionPlans> {
            val requiredBasePlanKeys = basePlanKeys.filter { !it.isInstallment }
            val basePlans = requiredBasePlanKeys.associateWith { key -> products.findMatchingSubscriptionPlan(key) }
            val basePlanFailure = basePlans.values
                .filterIsInstance<PaymentResult.Failure>()
                .firstOrNull()
            if (basePlanFailure != null) {
                return basePlanFailure
            }

            val installmentPlanKeys = basePlanKeys.filter { it.isInstallment }
            val installmentPlans = installmentPlanKeys.associateWith { key ->
                products.findMatchingSubscriptionPlan(key)
            }

            val offerPlans = offerPlanKeys.associateWith { key -> products.findMatchingSubscriptionPlan(key) }

            return PaymentResult.Success(SubscriptionPlans(basePlans + installmentPlans + offerPlans))
        }

        private fun List<Product>.findMatchingSubscriptionPlan(key: SubscriptionPlan.Key): PaymentResult<SubscriptionPlan> {
            val matchingProducts = findMatchingProducts(key)
            return when (matchingProducts.size) {
                1 -> {
                    val product = matchingProducts[0]
                    PaymentResult.Success(
                        if (key.offer != null) {
                            product.toOfferSubscriptionPlan(key)
                        } else {
                            product.toBaseSubscriptionPlan(key)
                        },
                    )
                }

                0 -> {
                    // Check if there's a product that matches ID/basePlan but fails installment validation
                    if (key.isInstallment && key.productId != null && key.basePlanId != null) {
                        val productWithMissingDetails = firstOrNull { product ->
                            product.id == key.productId &&
                                product.pricingPlans.basePlan.planId == key.basePlanId &&
                                product.pricingPlans.basePlan.installmentPlanDetails == null
                        }
                        if (productWithMissingDetails != null) {
                            return PaymentResult.Failure(
                                PaymentResultCode.DeveloperError,
                                "Product ${key.productId} claims to be installment plan but is missing installmentPlanDetails",
                            )
                        }
                    }
                    PaymentResult.Failure(PaymentResultCode.DeveloperError, "No matching product found for $key")
                }

                else -> PaymentResult.Failure(PaymentResultCode.DeveloperError, "Multiple matching products found for $key. $matchingProducts")
            }
        }

        private fun List<Product>.findMatchingProducts(key: SubscriptionPlan.Key): List<Product> {
            // If key has null productId or basePlanId, it's an unsupported combination (e.g., Plus Monthly installment)
            if (key.productId == null || key.basePlanId == null) {
                return emptyList()
            }

            return filter { product ->
                val offerCondition = if (key.offer != null) {
                    val pricingPhases = product.pricingPlans.offerPlans.singleOrNull { it.offerId == key.offerId }?.pricingPhases
                    val infinitePricingPhase = pricingPhases?.singleOrNull { it.schedule.recurrenceMode == RecurrenceMode.Infinite }
                    infinitePricingPhase != null && infinitePricingPhase == pricingPhases.last()
                } else {
                    val pricingPhase = product.pricingPlans.basePlan.pricingPhases.singleOrNull()
                    pricingPhase?.schedule?.recurrenceMode == RecurrenceMode.Infinite
                }

                val installmentCondition = if (key.isInstallment) {
                    // Installment plans must have installmentPlanDetails
                    product.pricingPlans.basePlan.installmentPlanDetails != null
                } else {
                    true
                }

                product.id == key.productId &&
                    product.pricingPlans.basePlan.planId == key.basePlanId &&
                    offerCondition &&
                    installmentCondition
            }
        }

        private fun Product.toBaseSubscriptionPlan(key: SubscriptionPlan.Key): SubscriptionPlan.Base {
            return SubscriptionPlan.Base(
                name,
                key.tier,
                key.billingCycle,
                pricingPlans.basePlan.pricingPhases[0],
                pricingPlans.basePlan.installmentPlanDetails,
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

    val productId: String? get() = key.productId
    val basePlanId: String? get() = key.basePlanId
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
        val installmentPlanDetails: InstallmentPlanDetails? = null,
    ) : SubscriptionPlan {
        override val offer get() = null
        val isInstallment: Boolean get() = installmentPlanDetails != null
        override val key get() = SubscriptionPlan.Key(tier, billingCycle, offer = null, isInstallment)
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
        val isInstallment: Boolean = false,
    ) {
        init {
            require(!(offer != null && isInstallment)) {
                "Installment plans cannot have promotional offers. Key: tier=$tier, billingCycle=$billingCycle, offer=$offer, isInstallment=$isInstallment"
            }
        }

        val productId: String? = SubscriptionPlan.productId(tier, billingCycle, isInstallment)
        val basePlanId: String? = SubscriptionPlan.basePlanId(tier, billingCycle, isInstallment)
        val offerId = offer?.offerId(tier, billingCycle)
    }

    companion object {
        const val PLUS_MONTHLY_PRODUCT_ID = "com.pocketcasts.plus.monthly"
        const val PLUS_YEARLY_PRODUCT_ID = "com.pocketcasts.plus.yearly"
        const val PLUS_YEARLY_INSTALLMENT_PRODUCT_ID = "com.pocketcasts.plus.yearly.installment"
        const val PATRON_MONTHLY_PRODUCT_ID = "com.pocketcasts.monthly.patron"
        const val PATRON_YEARLY_PRODUCT_ID = "com.pocketcasts.yearly.patron"

        val PlusMonthlyPreview get() = SubscriptionPlans.Preview.getBasePlan(SubscriptionTier.Plus, BillingCycle.Monthly)
        val PlusYearlyPreview get() = SubscriptionPlans.Preview.getBasePlan(SubscriptionTier.Plus, BillingCycle.Yearly)
        val PatronMonthlyPreview get() = SubscriptionPlans.Preview.getBasePlan(SubscriptionTier.Patron, BillingCycle.Monthly)
        val PatronYearlyPreview get() = SubscriptionPlans.Preview.getBasePlan(SubscriptionTier.Patron, BillingCycle.Yearly)

        fun productId(
            tier: SubscriptionTier,
            billingCycle: BillingCycle,
            isInstallment: Boolean = false,
        ): String? = when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> if (isInstallment) {
                    null // Plus Monthly installment plan doesn't exist
                } else {
                    PLUS_MONTHLY_PRODUCT_ID
                }

                BillingCycle.Yearly -> if (isInstallment) {
                    PLUS_YEARLY_INSTALLMENT_PRODUCT_ID
                } else {
                    PLUS_YEARLY_PRODUCT_ID
                }
            }

            SubscriptionTier.Patron -> when (billingCycle) {
                BillingCycle.Monthly -> if (isInstallment) {
                    null // Patron installment plans don't exist
                } else {
                    PATRON_MONTHLY_PRODUCT_ID
                }

                BillingCycle.Yearly -> if (isInstallment) {
                    null // Patron installment plans don't exist
                } else {
                    PATRON_YEARLY_PRODUCT_ID
                }
            }
        }

        fun basePlanId(
            tier: SubscriptionTier,
            billingCycle: BillingCycle,
            isInstallment: Boolean = false,
        ): String? = when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> if (isInstallment) {
                    null // Plus Monthly installment plan doesn't exist
                } else {
                    "p1m"
                }

                BillingCycle.Yearly -> if (isInstallment) {
                    "p1-installment"
                } else {
                    "p1y"
                }
            }

            SubscriptionTier.Patron -> when (billingCycle) {
                BillingCycle.Monthly -> if (isInstallment) {
                    null // Patron installment plans don't exist
                } else {
                    "patron-monthly"
                }

                BillingCycle.Yearly -> if (isInstallment) {
                    null // Patron installment plans don't exist
                } else {
                    "patron-yearly"
                }
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
    val isInstallment: Boolean = false,
) {
    val productId: String? get() = SubscriptionPlan.productId(tier, billingCycle, isInstallment)
}
