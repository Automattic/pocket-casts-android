package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant
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
    @field:Json(name = "platform") val platform: Int,
    @field:Json(name = "frequency") val frequency: Int,
    @field:Json(name = "expiryDate") val expiryDate: Date?,
    @field:Json(name = "autoRenewing") val autoRenewing: Boolean,
    @field:Json(name = "giftDays") val giftDays: Int,
)

fun SubscriptionStatusResponse.toSubscription(): Subscription? {
    if (paid == 0) {
        return null
    }

    val subscriptionResponse = subscriptions?.getOrNull(index) ?: fallbackSubscription

    return Subscription(
        tier = when (subscriptionResponse.tier?.lowercase()) {
            "plus" -> SubscriptionTier.Plus
            "patron" -> SubscriptionTier.Patron
            else -> return null
        },
        billingCycle = when (subscriptionResponse.frequency) {
            1 -> BillingCycle.Monthly
            2 -> BillingCycle.Yearly
            else -> null
        },
        platform = when (subscriptionResponse.platform) {
            1 -> SubscriptionPlatform.iOS
            2 -> SubscriptionPlatform.Android
            3 -> SubscriptionPlatform.Web
            4 -> SubscriptionPlatform.Gift
            else -> SubscriptionPlatform.Unknown
        },
        expiryDate = subscriptionResponse.expiryDate?.toInstant() ?: Instant.MAX,
        isAutoRenewing = subscriptionResponse.autoRenewing,
        giftDays = subscriptionResponse.giftDays,
    )
}

private val SubscriptionStatusResponse.fallbackSubscription get() = SubscriptionResponse(type, tier, platform, frequency, expiryDate, autoRenewing, giftDays)
