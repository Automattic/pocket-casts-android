package au.com.shiftyjelly.pocketcasts.utils.extensions

import com.android.billingclient.api.ProductDetails

val ProductDetails.shortTitle: String
    get() = title.split(" (").first()

val ProductDetails.firstSubscriptionPricingPhase: ProductDetails.PricingPhase?
    get() = subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()

val ProductDetails.price: String?
    get() = firstSubscriptionPricingPhase?.formattedPrice

val ProductDetails.priceDouble: Double?
    get() = firstSubscriptionPricingPhase?.priceAmountMicros?.let { it * 1_000_000.0 }

val ProductDetails.priceCurrencyCode: String?
    get() = firstSubscriptionPricingPhase?.priceCurrencyCode

val ProductDetails.billingPeriod: String?
    get() = firstSubscriptionPricingPhase?.billingPeriod
