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
    val period: Period,
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
    private val basePlans: Map<SubscriptionPlan.Key, PaymentResult<SubscriptionPlan.Base>>,
    private val offerPlans: Map<SubscriptionPlan.Key, PaymentResult<SubscriptionPlan.WithOffer>>,
) {
    fun getBasePlan(
        tier: SubscriptionTier,
        billingCycle: BillingCycle,
    ): SubscriptionPlan.Base {
        val key = SubscriptionPlan.Key(tier, billingCycle, offer = null, isInstallment = false)
        return requireNotNull(basePlans.getValue(key).getOrNull()) {
            "This should never happen. Base plans are asserted in the create method."
        }
    }

    fun findInstallmentPlan(
        tier: SubscriptionTier,
        billingCycle: BillingCycle,
    ): PaymentResult<SubscriptionPlan.Base> {
        val key = SubscriptionPlan.Key(tier, billingCycle, offer = null, isInstallment = true)
        return basePlans[key] ?: missingPlanResult(key)
    }

    fun findOfferPlan(
        tier: SubscriptionTier,
        billingCycle: BillingCycle,
        offer: SubscriptionOffer,
        isInstallment: Boolean = false,
    ): PaymentResult<SubscriptionPlan.WithOffer> {
        val key = SubscriptionPlan.Key(tier, billingCycle, offer, isInstallment)
        return offerPlans[key] ?: missingPlanResult(key)
    }

    companion object {
        val Preview get() = create(FakePaymentDataSource.DefaultLoadedProducts).getOrNull()!!

        private val basePlanKeys = SubscriptionTier.entries.flatMap { tier ->
            BillingCycle.entries.flatMap { billingCycle ->
                listOf(false, true).map { isInstallment ->
                    SubscriptionPlan.Key(tier, billingCycle, offer = null, isInstallment)
                }
            }
        }

        private val offerPlanKeys = basePlanKeys
            .flatMap { baseKey ->
                SubscriptionOffer.entries.map { offer ->
                    baseKey.copy(offer = offer)
                }
            }

        fun create(products: List<Product>): PaymentResult<SubscriptionPlans> {
            val requiredBasePlanKeys = basePlanKeys.filter { !it.isInstallment }
            val basePlans = requiredBasePlanKeys.associateWith { key ->
                products.findMatchingSubscriptionPlan(key) { product ->
                    product.toBaseSubscriptionPlan(key)
                }
            }
            val basePlanFailure = basePlans.values
                .filterIsInstance<PaymentResult.Failure>()
                .firstOrNull()
            if (basePlanFailure != null) {
                return basePlanFailure
            }

            val installmentPlanKeys = basePlanKeys.filter { it.isInstallment }
            val installmentPlans = installmentPlanKeys.associateWith { key ->
                products.findMatchingSubscriptionPlan(key) { product ->
                    product.toBaseSubscriptionPlan(key)
                }
            }

            val offerPlans = offerPlanKeys.associateWith { key ->
                products.findMatchingSubscriptionPlan(key) { product ->
                    product.toOfferSubscriptionPlan(key)
                }
            }

            return PaymentResult.Success(
                SubscriptionPlans(
                    basePlans = basePlans + installmentPlans,
                    offerPlans = offerPlans,
                ),
            )
        }

        private fun <T : SubscriptionPlan> List<Product>.findMatchingSubscriptionPlan(
            key: SubscriptionPlan.Key,
            mapper: (Product) -> PaymentResult<T>,
        ): PaymentResult<T> {
            val matchingProducts = findMatchingProducts(key)
            return when (matchingProducts.size) {
                1 -> mapper(matchingProducts[0])
                0 -> missingPlanResult(key)
                else -> PaymentResult.Failure(PaymentResultCode.DeveloperError, "Multiple matching products found for $key. $matchingProducts")
            }
        }

        private fun List<Product>.findMatchingProducts(key: SubscriptionPlan.Key): List<Product> {
            return asSequence()
                .filter(isMatchingProduct(key))
                .filter(isMatchingBasePlan(key))
                .filter(isValidPricingPlan(key))
                .filter(isValidInstallment(key))
                .toList()
        }

        private fun isMatchingProduct(key: SubscriptionPlan.Key) = { product: Product ->
            product.id == key.productId
        }

        private fun isMatchingBasePlan(key: SubscriptionPlan.Key) = { product: Product ->
            product.pricingPlans.basePlan.planId == key.basePlanId
        }

        private fun isValidPricingPlan(key: SubscriptionPlan.Key) = { product: Product ->
            if (key.offer != null) {
                val pricingPhases = product.pricingPlans.offerPlans.singleOrNull { it.offerId == key.offerId }?.pricingPhases
                val infinitePricingPhase = pricingPhases?.singleOrNull { it.schedule.recurrenceMode == RecurrenceMode.Infinite }
                infinitePricingPhase != null && infinitePricingPhase == pricingPhases.last()
            } else {
                val pricingPhase = product.pricingPlans.basePlan.pricingPhases.singleOrNull()
                pricingPhase?.schedule?.recurrenceMode == RecurrenceMode.Infinite
            }
        }

        private fun isValidInstallment(key: SubscriptionPlan.Key) = { product: Product ->
            if (key.isInstallment) {
                product.pricingPlans.basePlan.installmentPlanDetails != null
            } else {
                true
            }
        }

        private fun Product.toBaseSubscriptionPlan(key: SubscriptionPlan.Key): PaymentResult<SubscriptionPlan.Base> {
            val pricingPhase = pricingPlans.basePlan.pricingPhases.getOrNull(0)
            val hasInstallmentDetails = pricingPlans.basePlan.installmentPlanDetails != null
            return when {
                pricingPhase == null -> PaymentResult.Failure(PaymentResultCode.DeveloperError, "Missing pricing phase for $id")

                key.isInstallment && !hasInstallmentDetails -> PaymentResult.Failure(
                    PaymentResultCode.DeveloperError,
                    "Key expects installment plan but product $id has no installmentPlanDetails",
                )

                else -> PaymentResult.Success(
                    SubscriptionPlan.Base(
                        name,
                        key.tier,
                        key.billingCycle,
                        pricingPhase,
                        pricingPlans.basePlan.installmentPlanDetails,
                    ),
                )
            }
        }

        private fun Product.toOfferSubscriptionPlan(key: SubscriptionPlan.Key): PaymentResult<SubscriptionPlan.WithOffer> {
            val matchingOfferPlan = pricingPlans.offerPlans.singleOrNull { it.offerId == key.offerId }
            val hasInstallmentDetails = pricingPlans.basePlan.installmentPlanDetails != null
            return when {
                key.offer == null -> PaymentResult.Failure(PaymentResultCode.DeveloperError, "Missing offer for $id")

                matchingOfferPlan == null -> PaymentResult.Failure(PaymentResultCode.DeveloperError, "No matching offer plan for $id")

                key.isInstallment && !hasInstallmentDetails -> PaymentResult.Failure(
                    PaymentResultCode.DeveloperError,
                    "Key expects installment plan but product $id has no installmentPlanDetails",
                )

                else -> PaymentResult.Success(
                    SubscriptionPlan.WithOffer(
                        name,
                        key.tier,
                        key.billingCycle,
                        key.offer,
                        matchingOfferPlan.pricingPhases,
                        pricingPlans.basePlan.installmentPlanDetails,
                    ),
                )
            }
        }
    }
}

private fun missingPlanResult(key: SubscriptionPlan.Key): PaymentResult.Failure {
    return PaymentResult.Failure(PaymentResultCode.DeveloperError, "No matching product found for $key")
}

sealed interface SubscriptionPlan {
    val name: String
    val key: Key
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
    val installmentPlanDetails: InstallmentPlanDetails?
    val isInstallment get() = installmentPlanDetails != null

    data class Base(
        override val name: String,
        override val tier: SubscriptionTier,
        override val billingCycle: BillingCycle,
        val pricingPhase: PricingPhase,
        override val installmentPlanDetails: InstallmentPlanDetails? = null,
    ) : SubscriptionPlan {
        override val offer get() = null
        override val key get() = Key(tier, billingCycle, offer = null, isInstallment)
    }

    data class WithOffer(
        override val name: String,
        override val tier: SubscriptionTier,
        override val billingCycle: BillingCycle,
        override val offer: SubscriptionOffer,
        val pricingPhases: List<PricingPhase>,
        override val installmentPlanDetails: InstallmentPlanDetails? = null,
    ) : SubscriptionPlan {
        override val key get() = Key(tier, billingCycle, offer, isInstallment)
    }

    data class Key(
        val tier: SubscriptionTier,
        val billingCycle: BillingCycle,
        val offer: SubscriptionOffer?,
        val isInstallment: Boolean = false,
    ) {
        val productId: String? = productId(tier, billingCycle, isInstallment)
        val basePlanId: String? = basePlanId(tier, billingCycle, isInstallment)
        val offerId = offer?.offerId(tier, billingCycle, isInstallment)
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
                    "p1y-installments"
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
        isInstallment: Boolean = false,
    ) = when (this) {
        IntroOffer -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> null

                BillingCycle.Yearly -> if (isInstallment) {
                    null
                } else {
                    "plus-yearly-intro-50percent"
                }
            }

            SubscriptionTier.Patron -> null
        }

        Trial -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> null

                BillingCycle.Yearly -> if (isInstallment) {
                    "plus-yearly-installments-trial-30days"
                } else {
                    "plus-yearly-trial-30days"
                }
            }

            SubscriptionTier.Patron -> null
        }

        Referral -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> null

                BillingCycle.Yearly -> if (isInstallment) {
                    null
                } else {
                    "plus-yearly-referral-two-months-free"
                }
            }

            SubscriptionTier.Patron -> null
        }

        Winback -> when (tier) {
            SubscriptionTier.Plus -> when (billingCycle) {
                BillingCycle.Monthly -> "plus-monthly-winback"

                BillingCycle.Yearly -> if (isInstallment) {
                    "plus-yearly-installments-winback"
                } else {
                    "plus-yearly-winback"
                }
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
