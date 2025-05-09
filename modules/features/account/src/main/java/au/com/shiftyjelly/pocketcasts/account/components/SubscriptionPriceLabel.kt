package au.com.shiftyjelly.pocketcasts.account.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingSubscriptionPlan
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.flatMap
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType

@Composable
fun SubscriptionPriceLabel(
    subscriptionPlan: OnboardingSubscriptionPlan,
    modifier: Modifier = Modifier,
    primaryTextColor: Color = MaterialTheme.theme.colors.primaryText01,
    secondaryTextColor: Color = MaterialTheme.theme.colors.primaryText02,
    isFocusable: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        TextH30(
            text = subscriptionPlan.highlightedPrice.formattedPrice,
            color = primaryTextColor,
            fontSize = 22.sp,
            modifier = if (isFocusable) {
                Modifier.focusable()
            } else {
                Modifier
            },
        )

        TextP60(
            text = subscriptionPlan.pricePerPeriodWithSlashText,
            color = secondaryTextColor,
            modifier = Modifier.padding(start = 4.dp),
        )

        val crossedPrice = subscriptionPlan.crossedPrice
        if (crossedPrice != null) {
            TextP60(
                text = crossedPrice.formattedPrice,
                color = secondaryTextColor,
                modifier = Modifier.padding(start = 4.dp),
                style = TextStyle(textDecoration = TextDecoration.LineThrough),
            )
        }
    }
}

@Preview
@Composable
private fun SubscriptionPriceLabelPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SubscriptionPriceLabel(
            SubscriptionPlans.Preview
                .findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.IntroOffer)
                .flatMap { OnboardingSubscriptionPlan.create(it) }
                .getOrNull()!!,
        )
    }
}

@Preview
@Composable
private fun SubscriptionPriceLabelYearlyPreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        SubscriptionPriceLabel(
            OnboardingSubscriptionPlan.create(SubscriptionPlan.PlusYearlyPreview),
        )
    }
}

@Preview
@Composable
private fun SubscriptionPriceLabelMonthlyPreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        SubscriptionPriceLabel(
            OnboardingSubscriptionPlan.create(SubscriptionPlan.PlusMonthlyPreview),
        )
    }
}
