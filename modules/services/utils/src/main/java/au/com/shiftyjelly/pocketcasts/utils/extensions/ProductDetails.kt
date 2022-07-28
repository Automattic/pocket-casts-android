package au.com.shiftyjelly.pocketcasts.utils.extensions

import com.android.billingclient.api.ProductDetails

val ProductDetails.shortTitle: String
    get() {
        return title.split(" (").first()
    }

val ProductDetails.price: String
    get() {
        return subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "0"
    }

val ProductDetails.priceDouble: Double
    get() {
        val priceAmountMicros = subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros ?: 0
        return priceAmountMicros * 1_000_000.0
    }

val ProductDetails.priceCurrencyCode: String
    get() {
        return subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceCurrencyCode ?: ""
    }

val ProductDetails.billingPeriod: String?
    get() {
        return subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.billingPeriod
    }
