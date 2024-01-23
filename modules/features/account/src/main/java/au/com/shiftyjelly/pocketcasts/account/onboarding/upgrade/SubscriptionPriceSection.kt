package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.components.ProductAmountHorizontalText
import au.com.shiftyjelly.pocketcasts.compose.images.OfferBadge
import au.com.shiftyjelly.pocketcasts.models.type.Subscription

@Composable
fun SubscriptionPriceSection(
    subscription: Subscription,
    upgradeButton: UpgradeButton,
    modifier: Modifier = Modifier,
) {
    if (subscription is Subscription.WithOffer) {
        if (subscription is Subscription.Intro) {
            ProductAmountHorizontalText(
                primaryText = subscription.offerPricingPhase.priceSlashPeriod(LocalContext.current.resources),
                secondaryText = subscription.recurringPricingPhase.priceSlashPeriod(LocalContext.current.resources),
            )
        } else if (subscription is Subscription.Trial) {
            ProductAmountHorizontalText(
                primaryText = subscription.recurringPricingPhase.formattedPrice,
                secondaryText = subscription.recurringPricingPhase.period(LocalContext.current.resources),
                lineThroughSecondaryText = false,
            )
        }

        Spacer(modifier = modifier.padding(vertical = 4.dp))

        OfferBadge(
            shortNameRes = subscription.badgeOfferText(),
            backgroundColor = upgradeButton.backgroundColorRes,
            textColor = upgradeButton.textColorRes,
        )
    } else if (subscription is Subscription.Simple) {
        ProductAmountHorizontalText(
            primaryText = subscription.recurringPricingPhase.formattedPrice,
            secondaryText = subscription.recurringPricingPhase.period(LocalContext.current.resources),
            lineThroughSecondaryText = false,
        )
    }
}
