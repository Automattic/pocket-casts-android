package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Brush
import au.com.shiftyjelly.pocketcasts.compose.patronGradientBrush
import au.com.shiftyjelly.pocketcasts.compose.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

sealed class UpgradeButton(
    @StringRes val shortNameRes: Int,
    @ColorRes val backgroundColorRes: Int,
    @ColorRes val textColorRes: Int,
    open val gradientBackgroundColor: Brush,
    open val subscription: Subscription,
    open val planType: PlanType,
) {
    data class Plus(
        override val subscription: Subscription,
        override val planType: PlanType,
    ) : UpgradeButton(
        shortNameRes = LR.string.pocket_casts_plus_short,
        backgroundColorRes = UR.color.plus_gold,
        textColorRes = UR.color.black,
        subscription = subscription,
        planType = planType,
        gradientBackgroundColor = Brush.plusGradientBrush,
    )

    data class Patron(
        override val subscription: Subscription,
        override val planType: PlanType,
    ) : UpgradeButton(
        shortNameRes = LR.string.pocket_casts_patron_short,
        backgroundColorRes = UR.color.patron_purple,
        textColorRes = UR.color.white,
        subscription = subscription,
        planType = planType,
        gradientBackgroundColor = Brush.patronGradientBrush,
    )

    enum class PlanType { RENEW, SUBSCRIBE, UPGRADE }
}

fun Subscription.toUpgradeButton(
    planType: UpgradeButton.PlanType = UpgradeButton.PlanType.SUBSCRIBE,
) = when (this.tier) {
    SubscriptionTier.PLUS -> UpgradeButton.Plus(this, planType)
    SubscriptionTier.PATRON -> UpgradeButton.Patron(this, planType)
    SubscriptionTier.NONE -> throw IllegalStateException("Unknown subscription tier")
}
