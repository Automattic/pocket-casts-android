package com.android.billingclient.api

import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import org.json.JSONArray
import org.json.JSONObject
import com.android.billingclient.api.Purchase as GooglePurchase

fun createGoogleProductDetails(
    productId: String = "product-id",
    type: String = "subs",
    title: String = "title",
    name: String = "name",
    skuDetailsToken: String = "sku-details-token",
    subscriptionOfferDetails: List<SubscriptionOfferDetails>? = listOf(createGoogleOfferDetails()),
): ProductDetails {
    val json = JSONObject()
        .put("productId", productId)
        .put("type", type)
        .put("title", title)
        .put("name", name)
        .put("skuDetailsToken", skuDetailsToken)
        .putOpt("subscriptionOfferDetails", subscriptionOfferDetails?.map(SubscriptionOfferDetails::toJson)?.let(::JSONArray))
    return ProductDetails(json.toString())
}

fun createGoogleOfferDetails(
    basePlanId: String = "base-plan-id",
    offerId: String? = null,
    offerIdToken: String = "offer-id-token",
    pricingPhases: List<PricingPhase> = listOf(createGooglePricingPhase()),
    offerTags: List<String> = emptyList(),
): SubscriptionOfferDetails {
    val json = JSONObject()
        .put("basePlanId", basePlanId)
        .putOpt("offerId", offerId)
        .put("offerIdToken", offerIdToken)
        .put("pricingPhases", JSONArray(pricingPhases.map(PricingPhase::toJson)))
        .put("offerTags", JSONArray(offerTags))
    return SubscriptionOfferDetails(json)
}

fun createGooglePricingPhase(
    priceAmountMicros: Long = 100_000_000,
    priceCurrencyCode: String = "USD",
    formattedPrice: String = "$100.00",
    billingPeriod: String = "P1M",
    recurrenceMode: Int = 1,
    billingCycleCount: Int = 0,
): PricingPhase {
    val json = JSONObject()
        .put("priceAmountMicros", priceAmountMicros)
        .put("priceCurrencyCode", priceCurrencyCode)
        .put("formattedPrice", formattedPrice)
        .put("billingPeriod", billingPeriod)
        .put("recurrenceMode", recurrenceMode)
        .put("billingCycleCount", billingCycleCount)
    return PricingPhase(json)
}

fun createGooglePurchase(
    orderId: String? = "order-id",
    purchaseToken: String = "purchase-token",
    productIds: List<String> = listOf("product-id"),
    isAcknowledged: Boolean = true,
    isAutoRenewing: Boolean = true,
    isPurchased: Boolean = true,
): GooglePurchase {
    val quantity = productIds.size
    val productsField: Any? = if (quantity > 1) {
        JSONArray(productIds)
    } else {
        productIds.getOrNull(0)
    }
    val json = JSONObject()
        .put("orderId", orderId)
        .put("purchaseToken", purchaseToken)
        .put("purchaseState", if (isPurchased) 1 else 4)
        .put("quantity", quantity)
        .put(if (quantity > 1) "productIds" else "productId", productsField)
        .put("acknowledged", isAcknowledged)
        .put("autoRenewing", isAutoRenewing)

    return GooglePurchase(json.toString(), "")
}

private fun PricingPhase.toJson() = JSONObject()
    .put("priceAmountMicros", priceAmountMicros)
    .put("priceCurrencyCode", priceCurrencyCode)
    .put("formattedPrice", formattedPrice)
    .put("billingPeriod", billingPeriod)
    .put("recurrenceMode", recurrenceMode)
    .put("billingCycleCount", billingCycleCount)

private fun SubscriptionOfferDetails.toJson() = JSONObject()
    .put("basePlanId", basePlanId)
    .putOpt("offerId", offerId)
    .put("offerIdToken", offerToken)
    .put("pricingPhases", JSONArray(pricingPhases.pricingPhaseList.map(PricingPhase::toJson)))
    .put("offerTags", JSONArray(offerTags))
