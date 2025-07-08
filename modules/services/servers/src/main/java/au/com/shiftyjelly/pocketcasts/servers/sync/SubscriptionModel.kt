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
    @Json(name = "autoRenewing") val autoRenewing: Boolean,
    @Json(name = "expiryDate") val expiryDate: Date?,
    @Json(name = "giftDays") val giftDays: Int,
    @Json(name = "paid") val paid: Int,
    @Json(name = "platform") val platform: Int,
    @Json(name = "frequency") val frequency: Int,
    @Json(name = "subscriptions") val subscriptions: List<SubscriptionResponse>?,
    @Json(name = "type") val type: Int,
    @Json(name = "tier") val tier: String?,
    @Json(name = "index") val index: Int,
)

@JsonClass(generateAdapter = true)
data class SubscriptionResponse(
    @Json(name = "type") val type: Int,
    @Json(name = "tier") val tier: String?,
    @Json(name = "platform") val platform: Int,
    @Json(name = "frequency") val frequency: Int,
    @Json(name = "expiryDate") val expiryDate: Date?,
    @Json(name = "autoRenewing") val autoRenewing: Boolean,
    @Json(name = "giftDays") val giftDays: Int,
)

fun SubscriptionStatusResponse.toSubscription(): Subscription? {
    if (paid == 0) {
        return null
    }

    val subscriptionResponse = subscriptions?.getOrNull(index) ?: fallbackSubscription
    // Some older accounts use an empty string for the subscription tier inside their subscriptions.
    // In these cases, the correct tier is only available at the top-level subscription status object.
    //
    // Therefore, we need to explicitly fall back to the top level object.
    val tier = subscriptionResponse.tier
        ?.takeUnless(String::isNullOrBlank)
        ?: fallbackSubscription.tier

    return Subscription(
        tier = when (tier?.lowercase()) {
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

private val SubscriptionStatusResponse.fallbackSubscription
    get() = SubscriptionResponse(
        type = type,
        tier = tier,
        platform = platform,
        frequency = frequency,
        expiryDate = expiryDate,
        autoRenewing = autoRenewing,
        giftDays = giftDays,
    )
