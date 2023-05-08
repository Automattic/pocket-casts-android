package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class UpgradeButton(
    @StringRes val shortNameRes: Int,
    val backgroundColor: Long,
    val textColor: Long,
    open val subscription: Subscription,
) {
    data class Plus(
        override val subscription: Subscription,
    ) : UpgradeButton(
        shortNameRes = LR.string.pocket_casts_plus_short,
        backgroundColor = 0xFFFFD846,
        textColor = 0xFF000000,
        subscription = subscription,
    )

    data class Patron(
        override val subscription: Subscription,
    ) : UpgradeButton(
        shortNameRes = LR.string.pocket_casts_patron_short,
        backgroundColor = 0xFF6046F5,
        textColor = 0xFFFFFFFF,
        subscription = subscription,
    )
}

fun Subscription.toUpgradeButton() = when (this.tier) {
    Subscription.SubscriptionTier.PLUS -> UpgradeButton.Plus(this)
    Subscription.SubscriptionTier.PATRON -> UpgradeButton.Patron(this)
    Subscription.SubscriptionTier.UNKNOWN -> throw IllegalStateException("Unknown subscription tier")
}
