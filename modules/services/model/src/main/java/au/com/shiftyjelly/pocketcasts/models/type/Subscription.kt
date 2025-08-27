package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import com.squareup.moshi.JsonClass
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

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
        get() = platform == SubscriptionPlatform.Gift && giftDays > CHAMPION_GIFT_LOWER_BOUND

    companion object {
        val PlusPreview
            get() = Subscription(
                tier = SubscriptionTier.Plus,
                billingCycle = BillingCycle.Monthly,
                platform = SubscriptionPlatform.Android,
                expiryDate = Instant.EPOCH,
                isAutoRenewing = true,
                giftDays = 0,
            )

        val PatronPreview
            get() = Subscription(
                tier = SubscriptionTier.Patron,
                billingCycle = BillingCycle.Monthly,
                platform = SubscriptionPlatform.Android,
                expiryDate = Instant.EPOCH,
                isAutoRenewing = true,
                giftDays = 0,
            )
    }
}

private const val CHAMPION_GIFT_LOWER_BOUND = 10 * 365
