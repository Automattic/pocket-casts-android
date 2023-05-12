package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

sealed class UpgradeButton(
    @StringRes val shortNameRes: Int,
    @ColorRes val backgroundColorRes: Int,
    @ColorRes val textColorRes: Int,
    open val subscription: Subscription,
) {
    data class Plus(
        override val subscription: Subscription,
    ) : UpgradeButton(
        shortNameRes = LR.string.pocket_casts_plus_short,
        backgroundColorRes = UR.color.plus_gold,
        textColorRes = UR.color.black,
        subscription = subscription,
    )

    data class Patron(
        override val subscription: Subscription,
    ) : UpgradeButton(
        shortNameRes = LR.string.pocket_casts_patron_short,
        backgroundColorRes = UR.color.patron_purple,
        textColorRes = UR.color.white,
        subscription = subscription,
    )
}

fun Subscription.toUpgradeButton() = when (this.tier) {
    Subscription.SubscriptionTier.PLUS -> UpgradeButton.Plus(this)
    Subscription.SubscriptionTier.PATRON -> UpgradeButton.Patron(this)
    Subscription.SubscriptionTier.UNKNOWN -> throw IllegalStateException("Unknown subscription tier")
}
