package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.utils.toDurationFromNow
import com.squareup.moshi.JsonClass
import java.time.Instant
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

@JsonClass(generateAdapter = true)
data class Subscription(
    val tier: SubscriptionTier,
    val billingCycle: BillingCycle?,
    val platform: SubscriptionPlatform,
    val expiryDate: Instant,
    val isAutoRenewing: Boolean,
    val giftDays: Int,
    val isInstallment: Boolean = false,
) {
    val isExpiring
        get() = !isAutoRenewing && expiryDate.isBefore(Instant.now().plus(EXPIRING_WINDOW.toJavaDuration()))

    val expiresIn: Duration?
        get() = if (isExpiring) expiryDate.toDurationFromNow() else null

    val isChampion
        get() = platform == SubscriptionPlatform.Gift && giftDays > CHAMPION_GIFT_LOWER_BOUND

    companion object {
        val EXPIRING_WINDOW = 30.days

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
