package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ProfileUpgradeBannerViewModel.State
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalPagerWrapper
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.images.OfferBadge
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ProfileUpgradeBanner(
    state: State.Loaded,
    onFeatureCardChanged: (UpgradeFeatureCard) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Due to dependency on the billing classes in loaded state
    // the banner is not previawable as we cannot
    // instantiate the state ourselves.
    if (LocalInspectionMode.current) {
        TextH20(
            text = "Dummy upgrade banner for preview",
            textAlign = TextAlign.Center,
            modifier = modifier
                .background(MaterialTheme.theme.colors.primaryText01.copy(alpha = 0.1f))
                .padding(32.dp),
        )
    } else {
        val featureCardsState = state.featureCardsState
        HorizontalPagerWrapper(
            pageCount = featureCardsState.featureCards.size,
            initialPage = featureCardsState.featureCards.indexOf(state.featureCardsState.currentFeatureCard),
            onPageChanged = { onFeatureCardChanged(state.featureCardsState.featureCards[it]) },
            showPageIndicator = featureCardsState.showPageIndicator,
            pageIndicatorColor = MaterialTheme.theme.colors.primaryText01,
            modifier = modifier,
        ) { index, pagerHeight ->
            val currentTier = featureCardsState.featureCards[index].subscriptionTier
            FeatureCard(
                card = featureCardsState.featureCards[index],
                button = requireNotNull(state.upgradeButtons.find { it.subscription.tier == currentTier }),
                onClick = onClick,
                subscriptionFrequency = state.featureCardsState.currentFrequency,
                modifier = if (pagerHeight > 0) {
                    Modifier.height(pagerHeight.pxToDp(LocalContext.current).dp)
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun FeatureCard(
    card: UpgradeFeatureCard,
    button: UpgradeButton,
    onClick: () -> Unit,
    subscriptionFrequency: SubscriptionFrequency,
    modifier: Modifier = Modifier,
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
                iconRes = card.iconRes,
                shortNameRes = card.shortNameRes,
            )

            if (button.subscription is Subscription.WithOffer) {
                OfferBadge(
                    text = (button.subscription as Subscription.WithOffer).badgeOfferText(LocalContext.current.resources),
                    backgroundColor = button.backgroundColorRes,
                    textColor = button.textColorRes,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        Column {
            SubscriptionProductAmountHorizontal(
                subscription = button.subscription,
            )

            card.featureItems(subscriptionFrequency).forEach {
                UpgradeFeatureItem(
                    item = it,
                    color = MaterialTheme.theme.colors.primaryText01,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(8.dp))

        val primaryText = when (button.planType) {
            UpgradeButton.PlanType.RENEW -> stringResource(LR.string.renew_your_subscription)
            UpgradeButton.PlanType.SUBSCRIBE, UpgradeButton.PlanType.UPGRADE -> { stringResource(LR.string.subscribe_to, stringResource(button.shortNameRes)) }
        }
        OnboardingUpgradeHelper.UpgradeRowButton(
            primaryText = primaryText,
            backgroundColor = colorResource(button.backgroundColorRes),
            fontWeight = FontWeight.W500,
            textColor = colorResource(button.textColorRes),
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        )
    }
}
