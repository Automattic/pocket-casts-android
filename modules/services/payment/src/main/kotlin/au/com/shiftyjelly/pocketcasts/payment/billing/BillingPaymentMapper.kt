package au.com.shiftyjelly.pocketcasts.payment.billing

import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.Price
import au.com.shiftyjelly.pocketcasts.payment.PricingPhase
import au.com.shiftyjelly.pocketcasts.payment.PricingPlan
import au.com.shiftyjelly.pocketcasts.payment.PricingPlans
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule
import au.com.shiftyjelly.pocketcasts.payment.Product
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.PurchaseState
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams.ReplacementMode
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.android.billingclient.api.ProductDetails as GoogleProduct
import com.android.billingclient.api.ProductDetails.PricingPhase as GooglePricingPhase
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails as GoogleOfferDetails
import com.android.billingclient.api.Purchase as GooglePurchase
import com.android.billingclient.api.Purchase.PurchaseState as GooglePurchaseState

internal class BillingPaymentMapper(
    private val listeners: Set<PaymentClient.Listener>,
) {
    fun toProduct(productDetails: GoogleProduct): Product? {
        val mappingContext = mapOf("productId" to productDetails.productId)

        if (productDetails.productType != SUBSCRIPTION_TYPE) {
            dispatchMessage("Unrecognized product type '${productDetails.productType}'", mappingContext)
            return null
        }

        val offerDetails = productDetails.subscriptionOfferDetails
        if (offerDetails.isNullOrEmpty()) {
            dispatchMessage("No subscription offers", mappingContext)
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
        key: SubscriptionPlan.Key,
        productDetails: List<ProductDetails>,
        purchases: List<GooglePurchase>,
    ): BillingFlowRequest? {
        val (product, offerToken) = findMatchForPlan(productDetails, key) ?: return null

        val productQuery = BillingFlowRequest.ProductQuery(product, offerToken)
        val updateQuery = findActivePurchase(purchases)?.let { purchasedProductId ->
            findReplacementMode(purchasedProductId, key)?.let { replacementMode ->
                BillingFlowRequest.SubscriptionUpdateQuery(
                    oldProductId = purchasedProductId,
                    replacementMode = replacementMode,
                )
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
            dispatchMessage("No single base offer", mappingContext)
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
        val (count, period) = toPricingSchedulePeriod(pricingPhase.billingPeriod, mappingContext) ?: return null

        return PricingPhase(
            price = Price(
                amount = pricingPhase.priceAmountMicros.toBigDecimal().movePointLeft(6),
                currencyCode = pricingPhase.priceCurrencyCode,
                formattedPrice = pricingPhase.formattedPrice,
            ),
            schedule = PricingSchedule(
                periodCount = count,
                period = period,
                recurrenceMode = when (pricingPhase.recurrenceMode) {
                    RecurrenceMode.NON_RECURRING -> PricingSchedule.RecurrenceMode.NonRecurring
                    RecurrenceMode.FINITE_RECURRING -> PricingSchedule.RecurrenceMode.Recurring(
                        value = pricingPhase.billingCycleCount,
                    )
                    RecurrenceMode.INFINITE_RECURRING -> PricingSchedule.RecurrenceMode.Infinite
                    else -> {
                        dispatchMessage("Unrecognized recurrence mode '${pricingPhase.recurrenceMode}'", mappingContext)
                        return null
                    }
                },
            ),
        )
    }

    private fun toPricingSchedulePeriod(
        iso8601Duration: String,
        mappingContext: Map<String, Any?>,
    ): Pair<Int, PricingSchedule.Period>? {
        val context = mappingContext + mapOf("rawDuration" to iso8601Duration)

        val designator = iso8601Duration[0]
        if (designator != 'P') {
            dispatchMessage("Missing billing period duration designator", context)
            return null
        }
        val valuePart = iso8601Duration.drop(1)

        val rawCount = valuePart.takeWhile(Char::isDigit)
        val count = rawCount.toIntOrNull()
        if (count == null) {
            dispatchMessage("Invalid billing period interval count '$rawCount'", context)
            return null
        }

        val rawInterval = valuePart.drop(rawCount.length)
        val period = when (rawInterval) {
            "D" -> PricingSchedule.Period.Daily
            "W" -> PricingSchedule.Period.Weekly
            "M" -> PricingSchedule.Period.Monthly
            "Y" -> PricingSchedule.Period.Yearly
            else -> null
        }
        if (period == null) {
            dispatchMessage("Unrecognized billing interval period designator '$rawInterval'", context)
            return null
        }

        return count to period
    }

    private fun findMatchForPlan(
        products: List<ProductDetails>,
        key: SubscriptionPlan.Key,
    ): Pair<ProductDetails, String>? {
        val mappingContext = mapOf(
            "tier" to key.tier,
            "billingCycle" to key.billingCycle,
            "offer" to key.offer,
        )

        val matchingProducts = products.filter { it.productId == key.productId }
        if (matchingProducts.size > 1) {
            dispatchMessage("Found multiple matching products", mappingContext)
            return null
        }

        val matchingProduct = matchingProducts.firstOrNull()
        if (matchingProduct == null) {
            dispatchMessage("Found no matching products", mappingContext)
            return null
        }

        val matchingOffers = matchingProduct.subscriptionOfferDetails
            ?.filter { offer -> offer.basePlanId == key.basePlanId && offer.offerId == key.offerId }
            .orEmpty()
        if (matchingOffers.size > 1) {
            dispatchMessage("Found multiple matching offers", mappingContext)
            return null
        }

        val token = matchingOffers.firstOrNull()?.offerToken
        if (token == null) {
            dispatchMessage("Found no matching offers", mappingContext)
            return null
        }

        return matchingProduct to token
    }

    private fun findActivePurchase(purchases: List<GooglePurchase>): String? {
        val activePurchases = purchases.filter { it.isAcknowledged && it.isAutoRenewing }
        if (activePurchases.size > 1) {
            val context = mapOf("purchases" to activePurchases.joinToString { "${it.orderId}: ${it.products}" })
            dispatchMessage("Found more than one active purchase", context)
            return null
        }

        val activePurchase = activePurchases.firstOrNull() ?: return null
        if (activePurchase.products.size != 1) {
            val context = mapOf(
                "orderId" to activePurchase.orderId,
                "products" to activePurchase.products,
            )
            dispatchMessage("Active purchase should have only a single product", context)
            return null
        }

        return activePurchase.products.first()
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
        newPlanKey: SubscriptionPlan.Key,
    ) = when (newPlanKey.offer) {
        null -> when (currentPlanId) {
            SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID -> when (newPlanKey.productId) {
                SubscriptionPlan.PATRON_MONTHLY_PRODUCT_ID -> ReplacementMode.CHARGE_PRORATED_PRICE
                SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID -> ReplacementMode.CHARGE_FULL_PRICE
                SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID -> ReplacementMode.CHARGE_FULL_PRICE
                else -> null
            }

            SubscriptionPlan.PATRON_MONTHLY_PRODUCT_ID -> when (newPlanKey.productId) {
                SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID -> ReplacementMode.WITH_TIME_PRORATION
                SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID -> ReplacementMode.CHARGE_FULL_PRICE
                SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID -> ReplacementMode.CHARGE_FULL_PRICE
                else -> null
            }

            SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID -> when (newPlanKey.productId) {
                SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID -> ReplacementMode.WITH_TIME_PRORATION
                SubscriptionPlan.PATRON_MONTHLY_PRODUCT_ID -> ReplacementMode.WITH_TIME_PRORATION
                SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID -> ReplacementMode.CHARGE_PRORATED_PRICE
                else -> null
            }

            SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID -> when (newPlanKey.productId) {
                SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID -> ReplacementMode.WITH_TIME_PRORATION
                SubscriptionPlan.PATRON_MONTHLY_PRODUCT_ID -> ReplacementMode.WITH_TIME_PRORATION
                SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID -> ReplacementMode.WITH_TIME_PRORATION
                else -> null
            }

            else -> null
        }
        SubscriptionOffer.IntroOffer -> ReplacementMode.CHARGE_FULL_PRICE
        SubscriptionOffer.Trial -> ReplacementMode.CHARGE_FULL_PRICE
        SubscriptionOffer.Referral -> ReplacementMode.CHARGE_FULL_PRICE
        SubscriptionOffer.Winback -> ReplacementMode.CHARGE_FULL_PRICE
    }

    private fun dispatchMessage(message: String, context: Map<String, Any?>) {
        listeners.forEach { it.onMessage("$message in ${context.toSortedMap()}") }
    }
}

private const val SUBSCRIPTION_TYPE = "subs"
