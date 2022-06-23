package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class SubscriptionStatusResponse(
    @field:Json(name = "autoRenewing") val autoRenewing: Boolean,
    @field:Json(name = "expiryDate") val expiryDate: Date?,
    @field:Json(name = "giftDays") val giftDays: Int,
    @field:Json(name = "paid") val paid: Int,
    @field:Json(name = "platform") val platform: Int,
    @field:Json(name = "frequency") val frequency: Int,
    @field:Json(name = "subscriptions") val subscriptions: List<SubscriptionResponse>?,
    @field:Json(name = "type") val type: Int,
    @field:Json(name = "index") val index: Int
)

@JsonClass(generateAdapter = true)
data class SubscriptionResponse(
    @field:Json(name = "type") val type: Int,
    @field:Json(name = "frequency") val frequency: Int,
    @field:Json(name = "expiryDate") val expiryDate: Date?,
    @field:Json(name = "autoRenewing") val autoRenewing: Boolean,
    @field:Json(name = "updateUrl") val updateUrl: String?
)
