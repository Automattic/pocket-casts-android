package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
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
    @field:Json(name = "tier") val tier: String?,
    @field:Json(name = "index") val index: Int,
)

@JsonClass(generateAdapter = true)
data class SubscriptionResponse(
    @field:Json(name = "type") val type: Int,
    @field:Json(name = "tier") val tier: String?,
    @field:Json(name = "frequency") val frequency: Int,
    @field:Json(name = "expiryDate") val expiryDate: Date?,
    @field:Json(name = "autoRenewing") val autoRenewing: Boolean,
    @field:Json(name = "updateUrl") val updateUrl: String?,
)

fun SubscriptionStatusResponse.toStatus(): SubscriptionStatus {
    val originalPlatform = SubscriptionPlatform.entries.getOrNull(platform) ?: SubscriptionPlatform.NONE

    return if (paid == 0) {
        SubscriptionStatus.Free(expiryDate, giftDays, originalPlatform)
    } else {
        val subs = subscriptions?.map { it.toSubscription() } ?: emptyList()
        subs.getOrNull(index)?.isPrimarySubscription = true // Mark the subscription that the server says is the main one
        val freq = SubscriptionFrequency.entries.getOrNull(frequency) ?: SubscriptionFrequency.NONE
        val enumTier = SubscriptionTier.fromString(tier)
        SubscriptionStatus.Paid(expiryDate ?: Date(), autoRenewing, giftDays, freq, originalPlatform, subs, enumTier, index)
    }
}

private fun SubscriptionResponse.toSubscription(): SubscriptionStatus.Subscription {
    val enumTier = SubscriptionTier.fromString(tier)
    val freq = SubscriptionFrequency.entries.getOrNull(frequency) ?: SubscriptionFrequency.NONE
    return SubscriptionStatus.Subscription(enumTier, freq, expiryDate, autoRenewing, updateUrl)
}
