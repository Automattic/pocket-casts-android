package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.components.SubscriptionPriceLabel
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalPagerWrapper
import au.com.shiftyjelly.pocketcasts.compose.images.OfferBadge
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
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
fun ProfileUpgradeBanner(
    state: ProfileUpgradeBannerState.OldProfileUpgradeBannerState,
    onChangeFeatureCard: (SubscriptionPlan.Key) -> Unit,
    onClickSubscribe: (SubscriptionPlan.Key) -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalPagerWrapper(
        pageCount = state.onboardingPlans.size,
        initialPage = state.selectedFeatureCard?.let { selectedPlan ->
            state.onboardingPlans.indexOfFirst { it.key == selectedPlan }.takeIf { it != -1 }
        } ?: 0,
        onPageChange = { onChangeFeatureCard(state.onboardingPlans[it].key) },
        showPageIndicator = state.onboardingPlans.size > 1,
        pageIndicatorColor = MaterialTheme.theme.colors.primaryText01,
        modifier = modifier,
    ) { index, pagerHeight, _ ->
        val subscriptionPlan = state.onboardingPlans[index]
        FeatureCard(
            subscriptionPlan = subscriptionPlan,
            isRenewingSubscription = state.isRenewingSubscription,
            onClickSubscribe = { onClickSubscribe(subscriptionPlan.key) },
            expandContentHeight = true,
            modifier = if (pagerHeight > 0) {
                Modifier.height(LocalDensity.current.run { pagerHeight.toDp() })
            } else {
                Modifier
            },
        )
    }
}

@Composable
private fun FeatureCard(
    subscriptionPlan: OnboardingSubscriptionPlan,
    isRenewingSubscription: Boolean,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    expandContentHeight: Boolean = false,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            SubscriptionBadge(
                iconRes = subscriptionPlan.badgeIconRes,
                shortNameRes = subscriptionPlan.shortNameRes,
            )

            val offerText = subscriptionPlan.offerBadgeText
            if (offerText != null) {
                OfferBadge(
                    text = offerText,
                    backgroundColor = subscriptionPlan.offerBadgeColorRes,
                    textColor = subscriptionPlan.offerBadgeTextColorRes,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        SubscriptionPriceLabel(subscriptionPlan)

        subscriptionPlan.featureItems.forEach { item ->
            UpgradeFeatureItem(
                item = item,
                iconColor = MaterialTheme.theme.colors.primaryText01,
                textColor = MaterialTheme.theme.colors.primaryText01,
            )
        }

        if (expandContentHeight) {
            Spacer(
                modifier = Modifier.weight(1f),
            )
            Spacer(
                modifier = Modifier.height(8.dp),
            )
        }

        OnboardingUpgradeHelper.UpgradeRowButton(
            primaryText = subscriptionPlan.ctaButtonText(isRenewingSubscription),
            backgroundColor = subscriptionPlan.ctaButtonBackgroundColor,
            fontWeight = FontWeight.W500,
            textColor = subscriptionPlan.ctaButtonTextColor,
            onClick = onClickSubscribe,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        )
    }
}

@Preview
@Composable
private fun FeatureCardPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        FeatureCard(
            subscriptionPlan = OnboardingSubscriptionPlan.create(SubscriptionPlan.PlusYearlyPreview),
            isRenewingSubscription = false,
            onClickSubscribe = {},
        )
    }
}

@Preview
@Composable
private fun FeatureCardPlusPreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        FeatureCard(
            subscriptionPlan = OnboardingSubscriptionPlan.create(SubscriptionPlan.PlusYearlyPreview),
            isRenewingSubscription = false,
            onClickSubscribe = {},
        )
    }
}

@Preview
@Composable
private fun FeatureCardPatronPreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        FeatureCard(
            subscriptionPlan = OnboardingSubscriptionPlan.create(SubscriptionPlan.PatronYearlyPreview),
            isRenewingSubscription = false,
            onClickSubscribe = {},
        )
    }
}

@Preview
@Composable
private fun FeatureCardIntroOfferPreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        FeatureCard(
            subscriptionPlan = SubscriptionPlans.Preview
                .findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.IntroOffer)
                .flatMap { OnboardingSubscriptionPlan.create(it) }
                .getOrNull()!!,
            isRenewingSubscription = false,
            onClickSubscribe = {},
        )
    }
}

@Preview
@Composable
private fun FeatureCardTrialPreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        FeatureCard(
            subscriptionPlan = SubscriptionPlans.Preview
                .findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.Trial)
                .flatMap { OnboardingSubscriptionPlan.create(it) }
                .getOrNull()!!,
            isRenewingSubscription = false,
            onClickSubscribe = {},
        )
    }
}
