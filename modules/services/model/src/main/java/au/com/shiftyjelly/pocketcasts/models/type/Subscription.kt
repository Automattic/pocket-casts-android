package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import com.squareup.moshi.JsonClass
import java.time.Instant
import java.time.temporal.ChronoUnit

@JsonClass(generateAdapter = true)
data class Subscription(
    val tier: SubscriptionTier,
    val billingCycle: BillingCycle?,
    val platform: SubscriptionPlatform,
    val expiryDate: Instant,
    val isAutoRenewing: Boolean,
    val giftDays: Int,
) {
    val isExpiring
        get() = !isAutoRenewing && expiryDate.isBefore(Instant.now().plus(30, ChronoUnit.DAYS))

    val isChampion
        get() = platform == SubscriptionPlatform.Gift && giftDays > ChampionGiftLowerBound
}

private const val ChampionGiftLowerBound = 10 * 365
