package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubscriptionPurchaseRequest(
    @field:Json(name = "purchaseToken") val purchaseToken: String,
    @field:Json(name = "sku") val sku: String
)
