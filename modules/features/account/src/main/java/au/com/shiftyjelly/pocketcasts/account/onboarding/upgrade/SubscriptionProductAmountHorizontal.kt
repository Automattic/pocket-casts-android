package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.components.ProductAmountHorizontalText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription

@Composable
fun SubscriptionProductAmountHorizontal(
    subscription: Subscription,
    modifier: Modifier = Modifier,
    hasBackgroundAlwaysWhite: Boolean = false,
    secondaryTextColor: Color = MaterialTheme.theme.colors.primaryText02,
    isFocusable: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    if (subscription is Subscription.WithOffer) {
        if (subscription is Subscription.Intro) {
            ProductAmountHorizontalText(
                secondaryTextColor = secondaryTextColor,
                price = subscription.offerPricingPhase.pricingPhase.formattedPrice,
                period = subscription.offerPricingPhase.slashPeriod(LocalContext.current.resources),
                originalPrice = subscription.recurringPricingPhase.priceSlashPeriod(LocalContext.current.resources),
                hasBackgroundAlwaysWhite = hasBackgroundAlwaysWhite,
                isFocusable = isFocusable,
                focusRequester = focusRequester,
            )
        } else if (subscription is Subscription.Trial) {
            ProductAmountHorizontalText(
                secondaryTextColor = secondaryTextColor,
                price = subscription.recurringPricingPhase.formattedPrice,
                originalPrice = subscription.recurringPricingPhase.slashPeriod(LocalContext.current.resources),
                lineThroughOriginalPrice = false,
                hasBackgroundAlwaysWhite = hasBackgroundAlwaysWhite,
                isFocusable = isFocusable,
                focusRequester = focusRequester,
            )
        }

        Spacer(modifier = modifier.padding(vertical = 4.dp))
    } else if (subscription is Subscription.Simple) {
        ProductAmountHorizontalText(
            secondaryTextColor = secondaryTextColor,
            price = subscription.recurringPricingPhase.formattedPrice,
            originalPrice = subscription.recurringPricingPhase.slashPeriod(LocalContext.current.resources),
            lineThroughOriginalPrice = false,
            hasBackgroundAlwaysWhite = hasBackgroundAlwaysWhite,
            isFocusable = isFocusable,
            focusRequester = focusRequester,
        )
    }
}
