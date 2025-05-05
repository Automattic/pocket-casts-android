package au.com.shiftyjelly.pocketcasts.payment.billing

import au.com.shiftyjelly.pocketcasts.payment.BillingPeriod
import au.com.shiftyjelly.pocketcasts.payment.Logger
import au.com.shiftyjelly.pocketcasts.payment.Price
import au.com.shiftyjelly.pocketcasts.payment.PricingPhase
import au.com.shiftyjelly.pocketcasts.payment.PricingPlan
import au.com.shiftyjelly.pocketcasts.payment.PricingPlans
import au.com.shiftyjelly.pocketcasts.payment.Product
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.PurchaseState
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionBillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.android.billingclient.api.ProductDetails as GoogleProduct
import com.android.billingclient.api.ProductDetails.PricingPhase as GooglePricingPhase
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails as GoogleOfferDetails
import com.android.billingclient.api.Purchase as GooglePurchase
import com.android.billingclient.api.Purchase.PurchaseState as GooglePurchaseState

internal class BillingPaymentMapper(
    val logger: Logger,
) {
    fun toProduct(productDetails: GoogleProduct): Product? {
        val mappingContext = mapOf("productId" to productDetails.productId)

        if (productDetails.productType != SubscriptionType) {
            logWarning("Unrecognized product type '${productDetails.productType}'", mappingContext)
            return null
        }

        val offerDetails = productDetails.subscriptionOfferDetails
        if (offerDetails.isNullOrEmpty()) {
            logWarning("No subscription offers", mappingContext)
            return null
        }

        return Product(
            id = productDetails.productId,
            name = productDetails.name,
            pricingPlans = toPlans(offerDetails, mappingContext) ?: return null,
        )
    }

    fun toPurchase(purchase: GooglePurchase): Purchase {
        return Purchase(
            state = when (purchase.purchaseState) {
                GooglePurchaseState.PENDING -> PurchaseState.Pending
                GooglePurchaseState.PURCHASED -> purchase.orderId?.let(PurchaseState::Purchased) ?: PurchaseState.Unspecified
                else -> PurchaseState.Unspecified
            },
            token = purchase.purchaseToken,
            productIds = purchase.products,
            isAcknowledged = purchase.isAcknowledged,
            isAutoRenewing = purchase.isAutoRenewing,
        )
    }

    fun toBillingFlowRequest(
        plan: SubscriptionPlan,
        productDetails: List<ProductDetails>,
        purchases: List<GooglePurchase>,
    ): BillingFlowRequest? {
        val (product, offerToken) = findMatchForPlan(productDetails, plan) ?: return null

        val productQuery = BillingFlowRequest.ProductQuery(product, offerToken)
        val updateQuery = findActivePurchase(purchases)?.let { (purchaseToken, purchasedProductId) ->
            findReplacementMode(purchasedProductId, plan)?.let { replacementMode ->
                BillingFlowRequest.SubscriptionUpdateQuery(purchaseToken, replacementMode)
            }
        }
        return BillingFlowRequest(productQuery, updateQuery)
    }

    private fun toPlans(
        offerDetails: List<GoogleOfferDetails>,
        mappingContext: Map<String, Any?>,
    ): PricingPlans? {
        return PricingPlans(
            basePlan = toBasePlan(offerDetails, mappingContext) ?: return null,
            offerPlans = toOfferPlans(offerDetails, mappingContext) ?: return null,
        )
    }

    private fun toBasePlan(
        offerDetails: List<GoogleOfferDetails>,
        mappingContext: Map<String, Any?>,
    ): PricingPlan.Base? {
        val noOfferDetails = offerDetails.singleOrNull { it.offerId == null }
        if (noOfferDetails == null) {
            logWarning("No single base offer", mappingContext)
            return null
        }

        return PricingPlan.Base(
            planId = noOfferDetails.basePlanId,
            pricingPhases = toPricingPhases(
                pricingPhases = noOfferDetails.pricingPhases.pricingPhaseList,
                mappingContext = mappingContext + mapOf(
                    "basePlanId" to noOfferDetails.basePlanId,
                ),
            ) ?: return null,
            tags = noOfferDetails.offerTags,
        )
    }

    private fun toOfferPlans(
        offerDetails: List<GoogleOfferDetails>,
        mappingContext: Map<String, Any?>,
    ): List<PricingPlan.Offer>? {
        val withOfferDetails = offerDetails.filter { it.offerId != null }
        val mappedPlans = withOfferDetails.mapNotNull { toOfferPlan(it, mappingContext) }

        return if (withOfferDetails.size != mappedPlans.size) {
            return null
        } else {
            mappedPlans
        }
    }

    private fun toOfferPlan(
        offerDetails: GoogleOfferDetails,
        mappingContext: Map<String, Any?>,
    ): PricingPlan.Offer? {
        return PricingPlan.Offer(
            offerId = requireNotNull(offerDetails.offerId),
            planId = offerDetails.basePlanId,
            pricingPhases = toPricingPhases(
                pricingPhases = offerDetails.pricingPhases.pricingPhaseList,
                mappingContext = mappingContext + mapOf(
                    "basePlanId" to offerDetails.basePlanId,
                    "offerId" to offerDetails.offerId,
                ),
            ) ?: return null,
            tags = offerDetails.offerTags,
        )
    }

    private fun toPricingPhases(
        pricingPhases: List<GooglePricingPhase>,
        mappingContext: Map<String, Any?>,
    ): List<PricingPhase>? {
        val mappedPhases = pricingPhases.mapNotNull { toPricingPhase(it, mappingContext) }

        return if (pricingPhases.size != mappedPhases.size) {
            return null
        } else {
            mappedPhases
        }
    }

    private fun toPricingPhase(
        pricingPhase: GooglePricingPhase,
        mappingContext: Map<String, Any?>,
    ): PricingPhase? {
        val (count, interval) = toBillingDuration(pricingPhase.billingPeriod, mappingContext) ?: return null

        return PricingPhase(
            price = Price(
                amount = pricingPhase.priceAmountMicros.toBigDecimal().movePointLeft(6),
                currencyCode = pricingPhase.priceCurrencyCode,
                formattedPrice = pricingPhase.formattedPrice,
            ),
            billingPeriod = BillingPeriod(
                intervalCount = count,
                interval = interval,
                cycle = when (pricingPhase.recurrenceMode) {
                    RecurrenceMode.NON_RECURRING -> BillingPeriod.Cycle.NonRecurring
                    RecurrenceMode.FINITE_RECURRING -> BillingPeriod.Cycle.Recurring(
                        value = pricingPhase.billingCycleCount,
                    )
                    RecurrenceMode.INFINITE_RECURRING -> BillingPeriod.Cycle.Infinite
                    else -> {
                        logWarning("Unrecognized recurrence mode '${pricingPhase.recurrenceMode}'", mappingContext)
                        return null
                    }
                },
            ),
        )
    }

    private fun toBillingDuration(
        iso8601Duration: String,
        mappingContext: Map<String, Any?>,
    ): Pair<Int, BillingPeriod.Interval>? {
        val context = mappingContext + mapOf("rawDuration" to iso8601Duration)

        val designator = iso8601Duration[0]
        if (designator != 'P') {
            logWarning("Missing billing period duration designator", context)
            return null
        }
        val valuePart = iso8601Duration.drop(1)

        val rawCount = valuePart.takeWhile(Char::isDigit)
        val count = rawCount.toIntOrNull()
        if (count == null) {
            logWarning("Invalid billing period interval count '$rawCount'", context)
            return null
        }

        val rawInterval = valuePart.drop(rawCount.length)
        val interval = when (rawInterval) {
            "W" -> BillingPeriod.Interval.Weekly
            "M" -> BillingPeriod.Interval.Monthly
            "Y" -> BillingPeriod.Interval.Yearly
            else -> null
        }
        if (interval == null) {
            logWarning("Unrecognized billing interval period designator '$rawInterval'", context)
            return null
        }

        return count to interval
    }

    private fun findMatchForPlan(
        products: List<ProductDetails>,
        plan: SubscriptionPlan,
    ): Pair<ProductDetails, String>? {
        val mappingContext = mapOf(
            "tier" to plan.tier,
            "billingCycle" to plan.billingCycle,
            "offer" to plan.offer,
        )

        val matchingProducts = products.filter { it.productId == plan.productId }
        if (matchingProducts.size > 1) {
            logWarning("Found multiple matching products", mappingContext)
            return null
        }

        val matchingProduct = matchingProducts.firstOrNull()
        if (matchingProduct == null) {
            logWarning("Found no matching products", mappingContext)
            return null
        }

        val matchingOffers = matchingProduct.subscriptionOfferDetails
            ?.filter { offer -> offer.basePlanId == plan.basePlanId && offer.offerId == plan.offerId }
            .orEmpty()
        if (matchingOffers.size > 1) {
            logWarning("Found multiple matching offers", mappingContext)
            return null
        }

        val token = matchingOffers.firstOrNull()?.offerToken
        if (token == null) {
            logWarning("Found no matching offers", mappingContext)
            return null
        }

        return matchingProduct to token
    }

    private fun findActivePurchase(purchases: List<GooglePurchase>): Pair<String, String>? {
        val activePurchases = purchases.filter { it.isAcknowledged && it.isAutoRenewing }
        if (activePurchases.size > 1) {
            val context = mapOf("purchases" to activePurchases.joinToString { "${it.orderId}: ${it.products}" })
            logWarning("Found more than one active purchase", context)
            return null
        }

        val activePurchase = activePurchases.firstOrNull() ?: return null
        if (activePurchase.products.size != 1) {
            val context = mapOf(
                "orderId" to activePurchase.orderId,
                "products" to activePurchase.products,
            )
            logWarning("Active purchase should have only a single product", context)
            return null
        }

        return activePurchase.purchaseToken to activePurchase.products.first()
    }

    /**
     * Determines the correct subscription replacement mode when a user switches subscriptions
     * from one plan to another, based on a predefined transition table. If the new plan is a plan with an offer
     * replacement mode is always `CHARGE_FULL_PRICE`.
     *
     * Transition Table:
     * ```
     * |                | Plus Monthly        | Patron Monthly        | Plus Yearly           | Patron Yearly         |
     * | Plus Monthly   | N/A                 | CHARGE_PRORATED_PRICE | CHARGE_FULL_PRICE     | CHARGE_FULL_PRICE     |
     * | Patron Monthly | WITH_TIME_PRORATION | N/A                   | CHARGE_FULL_PRICE     | CHARGE_FULL_PRICE     |
     * | Plus Yearly    | WITH_TIME_PRORATION | WITH_TIME_PRORATION   | N/A                   | CHARGE_PRORATED_PRICE |
     * | Patron Yearly  | WITH_TIME_PRORATION | WITH_TIME_PRORATION   | WITH_TIME_PRORATION   | N/A                   |
     * ```
     *
     * * `CHARGE_PRORATED_PRICE`: Charge the price difference and keep the billing cycle.
     * * `CHARGE_FULL_PRICE`: Upgrade or downgrade immediately. The remaining value is either carried over or prorated for time.
     * * `WITH_TIME_PRORATION`: Upgrade or downgrade immediately. The remaining time is adjusted based on the price difference.
     *
     * Note: We avoid using the DEFERRED replacement mode because it leaves users
     * without an active subscription until the deferred date, which complicates handling
     * further plan changes.
     *
     * See the [documentation](https://developer.android.com/google/play/billing/subscriptions#replacement-modes) for details
     * on replacement modes.
     */
    private fun findReplacementMode(
        currentPlanId: String,
        newPlan: SubscriptionPlan,
    ) = when (newPlan) {
        is SubscriptionPlan.Base -> when (currentPlanId) {
            PlusMonthlyId -> when (newPlan.productId) {
                PatronMonthlyId -> ReplacementMode.CHARGE_PRORATED_PRICE
                PlusYearlyId -> ReplacementMode.CHARGE_FULL_PRICE
                PatronYearlyId -> ReplacementMode.CHARGE_FULL_PRICE
                else -> null
            }

            PatronMonthlyId -> when (newPlan.productId) {
                PlusMonthlyId -> ReplacementMode.WITH_TIME_PRORATION
                PlusYearlyId -> ReplacementMode.CHARGE_FULL_PRICE
                PatronYearlyId -> ReplacementMode.CHARGE_FULL_PRICE
                else -> null
            }

            PlusYearlyId -> when (newPlan.productId) {
                PlusMonthlyId -> ReplacementMode.WITH_TIME_PRORATION
                PatronMonthlyId -> ReplacementMode.WITH_TIME_PRORATION
                PatronYearlyId -> ReplacementMode.CHARGE_PRORATED_PRICE
                else -> null
            }

            PatronYearlyId -> when (newPlan.productId) {
                PlusMonthlyId -> ReplacementMode.WITH_TIME_PRORATION
                PatronMonthlyId -> ReplacementMode.WITH_TIME_PRORATION
                PlusYearlyId -> ReplacementMode.WITH_TIME_PRORATION
                else -> null
            }

            else -> null
        }

        is SubscriptionPlan.WithOffer -> ReplacementMode.CHARGE_FULL_PRICE
    }

    private fun logWarning(message: String, context: Map<String, Any?>) {
        logger.warning("$message in ${context.toSortedMap()}")
    }
}

private const val SubscriptionType = "subs"
private val PlusMonthlyId = SubscriptionPlan.productId(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly)
private val PlusYearlyId = SubscriptionPlan.productId(SubscriptionTier.Plus, SubscriptionBillingCycle.Yearly)
private val PatronMonthlyId = SubscriptionPlan.productId(SubscriptionTier.Patron, SubscriptionBillingCycle.Monthly)
private val PatronYearlyId = SubscriptionPlan.productId(SubscriptionTier.Patron, SubscriptionBillingCycle.Yearly)
