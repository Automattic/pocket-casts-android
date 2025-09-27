package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.type.Membership
import au.com.shiftyjelly.pocketcasts.models.type.MembershipFeature
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
    @Json(name = "createdAt") val createdAt: Instant?,
    @Json(name = "features") val features: SubscriptionFeatures?,
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

@JsonClass(generateAdapter = true)
data class SubscriptionFeatures(
    @Json(name = "removeBannerAds") val removeBannerAds: Boolean,
    @Json(name = "removeDiscoverAds") val removeDiscoverAds: Boolean,
)

fun SubscriptionStatusResponse.toMembership(): Membership {
    val membershipFeatures = features?.toMembershipFeatures().orEmpty()

    if (paid == 0) {
        return Membership(
            subscription = null,
            createdAt = createdAt,
            features = membershipFeatures,
        )
    }

    val subscriptionResponse = subscriptions?.getOrNull(index) ?: fallbackSubscription
    // Some older accounts use an empty string for the subscription tier inside their subscriptions.
    // In these cases, the correct tier is only available at the top-level subscription status object.
    //
    // Therefore, we need to explicitly fall back to the top level object.
    val rawTier = subscriptionResponse.tier
        ?.takeUnless(String::isNullOrBlank)
        ?: fallbackSubscription.tier
    val tier = when (rawTier?.lowercase()) {
        "plus" -> SubscriptionTier.Plus
        "patron" -> SubscriptionTier.Patron
        else -> null
    }
    val subscription = tier?.let {
        Subscription(
            tier = tier,
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

    return Membership(
        subscription = subscription,
        createdAt = createdAt,
        features = membershipFeatures,
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

private fun SubscriptionFeatures.toMembershipFeatures() = buildList {
    if (removeBannerAds) {
        add(MembershipFeature.NoBannerAds)
    }
    if (removeDiscoverAds) {
        add(MembershipFeature.NoDiscoverAds)
    }
}
