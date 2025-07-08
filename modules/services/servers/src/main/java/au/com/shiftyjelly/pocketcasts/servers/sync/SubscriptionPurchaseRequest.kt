package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubscriptionPurchaseRequest(
    @Json(name = "purchaseToken") val purchaseToken: String,
    @Json(name = "sku") val sku: String,
)
