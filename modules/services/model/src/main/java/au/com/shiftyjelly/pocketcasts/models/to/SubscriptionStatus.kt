package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

private const val giftDaysCutOver = 10 * 365

sealed class SubscriptionStatus(val expiryDate: Date?, val subscriptions: List<Subscription>) {

    @JsonClass(generateAdapter = true)
    data class Free(
        @field:Json(name = "expiry") val expiry: Date? = null,
        @field:Json(name = "giftDays") val giftDays: Int = 0,
        @field:Json(name = "platform") val platform: SubscriptionPlatform = SubscriptionPlatform.NONE,
        @field:Json(name = "subscriptions") val subscriptionList: List<Subscription> = emptyList()
    ) : SubscriptionStatus(expiry, subscriptionList)

    @JsonClass(generateAdapter = true)
    data class Paid(
        @field:Json(name = "expiry") val expiry: Date,
        @field:Json(name = "autoRenew") val autoRenew: Boolean,
        @field:Json(name = "giftDays") val giftDays: Int = 0,
        @field:Json(name = "frequency") val frequency: SubscriptionFrequency = SubscriptionFrequency.NONE,
        @field:Json(name = "platform") val platform: SubscriptionPlatform,
        @field:Json(name = "subscriptions") val subscriptionList: List<Subscription> = emptyList(),
        @field:Json(name = "type") val type: SubscriptionType,
        @field:Json(name = "tier") val tier: SubscriptionTier,
        @field:Json(name = "index") val index: Int
    ) : SubscriptionStatus(expiry, subscriptionList)

    @JsonClass(generateAdapter = true)
    data class Subscription(
        @field:Json(name = "type") val type: SubscriptionType,
        @field:Json(name = "tier") val tier: SubscriptionTier,
        @field:Json(name = "frequency") val frequency: SubscriptionFrequency,
        @field:Json(name = "expiryDate") val expiryDate: Date?,
        @field:Json(name = "autoRenewing") val autoRenewing: Boolean,
        @field:Json(name = "updateUrl") val updateUrl: String?,
        @field:Json(name = "isPrimarySubscription") var isPrimarySubscription: Boolean = false // Server marks this one as the main subscription
    ) {
        val isExpired: Boolean
            get() = !autoRenewing && expiryDate?.before(Date()) ?: false
    }

    val isLifetimePlus: Boolean
        get() = this is Paid && this.platform == SubscriptionPlatform.GIFT && this.giftDays > giftDaysCutOver
}
