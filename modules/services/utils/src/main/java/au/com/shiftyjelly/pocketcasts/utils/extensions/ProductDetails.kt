package au.com.shiftyjelly.pocketcasts.utils.extensions

import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import java.time.Period
import java.time.format.DateTimeParseException

val ProductDetails.shortTitle: String
    get() = title.split(" (").first()

val ProductDetails.recurringPrice: String?
    get() = recurringSubscriptionPricingPhase?.formattedPrice

val ProductDetails.recurringPriceDouble: Double?
    get() = recurringSubscriptionPricingPhase?.priceAmountMicros?.let { it * 1_000_000.0 }

val ProductDetails.recurringPriceCurrencyCode: String?
    get() = recurringSubscriptionPricingPhase?.priceCurrencyCode

val ProductDetails.recurringBillingPeriod: Period?
    get() = getPeriod(recurringSubscriptionPricingPhase?.billingPeriod)

val ProductDetails.trialBillingPeriod: Period?
    get() = trialSubscriptionPricingPhase?.billingPeriod?.let { getPeriod(it) }

private val ProductDetails.recurringSubscriptionPricingPhase: ProductDetails.PricingPhase?
    get() = findOnlyMatchingPricingPhase(
        predicate = { it.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING },
        errorMessageIfNotSingleMatch = { "ProductDetails did not have a single infinite recurring pricing phase, instead it had $it" }
    )

private val ProductDetails.trialSubscriptionPricingPhase: ProductDetails.PricingPhase?
    get() = findOnlyMatchingPricingPhase(
        predicate = { it.recurrenceMode == ProductDetails.RecurrenceMode.FINITE_RECURRING },
        errorMessageIfNotSingleMatch = { "ProductDetails did not have a single finite recurring pricing phase, instead it had $it" }
    )

private fun ProductDetails.findOnlyMatchingPricingPhase(
    predicate: (ProductDetails.PricingPhase) -> Boolean,
    errorMessageIfNotSingleMatch: (Int) -> String
): ProductDetails.PricingPhase? {
    val subscriptionOfferDetailsSafe = subscriptionOfferDetails
    if (subscriptionOfferDetailsSafe == null) {
        LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "ProductDetails had null subscriptionOfferDetails")
        return null
    }

    // TODO just taking the first one doesn't seem like the way to go here
    val pricingPhases = subscriptionOfferDetailsSafe.first().pricingPhases.pricingPhaseList
    val matchingPhases = pricingPhases.filter(predicate)

    if (matchingPhases.size != 1) {
        LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, errorMessageIfNotSingleMatch(matchingPhases.size))
        return null
    }
    return matchingPhases.first()
}

private fun getPeriod(billingPeriod: String?): Period? =
    try {
        Period.parse(billingPeriod)
    } catch (_: DateTimeParseException) {
        LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Unable to parse billingPeriod: $billingPeriod")
        null
    }
