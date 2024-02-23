package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.components.ProductAmountHorizontalText
import au.com.shiftyjelly.pocketcasts.models.type.Subscription

@Composable
fun SubscriptionProductAmountHorizontal(
    subscription: Subscription,
    modifier: Modifier = Modifier,
    hasBackgroundAlwaysWhite: Boolean = false,
) {
    if (subscription is Subscription.WithOffer) {
        if (subscription is Subscription.Intro) {
            ProductAmountHorizontalText(
                price = subscription.offerPricingPhase.pricingPhase.formattedPrice,
                period = subscription.offerPricingPhase.slashPeriod(LocalContext.current.resources),
                originalPrice = subscription.recurringPricingPhase.priceSlashPeriod(LocalContext.current.resources),
                hasBackgroundAlwaysWhite = hasBackgroundAlwaysWhite,
            )
        } else if (subscription is Subscription.Trial) {
            ProductAmountHorizontalText(
                price = subscription.recurringPricingPhase.formattedPrice,
                originalPrice = subscription.recurringPricingPhase.slashPeriod(LocalContext.current.resources),
                lineThroughOriginalPrice = false,
                hasBackgroundAlwaysWhite = hasBackgroundAlwaysWhite,
            )
        }

        Spacer(modifier = modifier.padding(vertical = 4.dp))
    } else if (subscription is Subscription.Simple) {
        ProductAmountHorizontalText(
            price = subscription.recurringPricingPhase.formattedPrice,
            originalPrice = subscription.recurringPricingPhase.slashPeriod(LocalContext.current.resources),
            lineThroughOriginalPrice = false,
            hasBackgroundAlwaysWhite = hasBackgroundAlwaysWhite,
        )
    }
}
