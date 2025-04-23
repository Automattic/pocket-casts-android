package au.com.shiftyjelly.pocketcasts.payment.billing

import au.com.shiftyjelly.pocketcasts.payment.BillingPeriod
import au.com.shiftyjelly.pocketcasts.payment.Logger
import au.com.shiftyjelly.pocketcasts.payment.Plan
import au.com.shiftyjelly.pocketcasts.payment.Plans
import au.com.shiftyjelly.pocketcasts.payment.Price
import au.com.shiftyjelly.pocketcasts.payment.PricingPhase
import au.com.shiftyjelly.pocketcasts.payment.Product
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.android.billingclient.api.ProductDetails as GoogleProduct
import com.android.billingclient.api.ProductDetails.PricingPhase as GooglePricingPhase
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails as GoogleOfferDetails

internal class ProductDetailsMapper(
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
            plans = toPlans(offerDetails, mappingContext) ?: return null,
        )
    }

    private fun toPlans(
        offerDetails: List<GoogleOfferDetails>,
        mappingContext: Map<String, Any?>,
    ): Plans? {
        return Plans(
            offerPlans = toOfferPlans(offerDetails, mappingContext) ?: return null,
            basePlan = toBasePlan(offerDetails, mappingContext) ?: return null,
        )
    }

    private fun toBasePlan(
        offerDetails: List<GoogleOfferDetails>,
        mappingContext: Map<String, Any?>,
    ): Plan.Base? {
        val noOfferDetails = offerDetails.singleOrNull { it.offerId == null }
        if (noOfferDetails == null) {
            logWarning("No single base offer", mappingContext)
            return null
        }

        return Plan.Base(
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
    ): List<Plan.Offer>? {
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
    ): Plan.Offer? {
        return Plan.Offer(
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

    private fun logWarning(message: String, context: Map<String, Any?>) {
        logger.warning("$message in ${context.toSortedMap()}")
    }
}

private const val SubscriptionType = "subs"
