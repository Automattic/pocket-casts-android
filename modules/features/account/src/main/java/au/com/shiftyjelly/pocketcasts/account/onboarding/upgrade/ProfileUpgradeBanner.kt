package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ProfileUpgradeBannerViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ProfileUpgradeBannerViewModel.State
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalPagerWrapper
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ProfileUpgradeBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<ProfileUpgradeBannerViewModel>()
    val state by viewModel.state.collectAsState()

    when (state) {
        is State.Loaded -> {
            val loadedState = state as State.Loaded
            ProfileUpgradeBannerView(
                state = loadedState,
                onClick = onClick,
                onFeatureCardChanged = {
                    viewModel.onFeatureCardChanged(loadedState.featureCardsState.featureCards[it])
                },
                modifier = modifier,
            )
        }
        is State.Loading -> Unit // Do nothing
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileUpgradeBannerView(
    state: State.Loaded,
    onFeatureCardChanged: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val featureCardsState = state.featureCardsState
    HorizontalPagerWrapper(
        pageCount = featureCardsState.featureCards.size,
        initialPage = featureCardsState.featureCards.indexOf(state.featureCardsState.currentFeatureCard),
        onPageChanged = onFeatureCardChanged,
        showPageIndicator = featureCardsState.showPageIndicator,
        pageIndicatorColor = MaterialTheme.theme.colors.primaryText01,
        modifier = modifier,
    ) { index, pagerHeight ->
        val currentTier = featureCardsState.featureCards[index].subscriptionTier
        FeatureCard(
            card = featureCardsState.featureCards[index],
            button = requireNotNull(state.upgradeButtons.find { it.subscription.tier == currentTier }),
            onClick = onClick,
            modifier = if (pagerHeight > 0) {
                Modifier.height(pagerHeight.pxToDp(LocalContext.current).dp)
            } else {
                Modifier
            },
        )
    }
}

@Composable
private fun FeatureCard(
    card: UpgradeFeatureCard,
    button: UpgradeButton,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            SubscriptionBadge(
                iconRes = card.iconRes,
                shortNameRes = card.shortNameRes,
            )

            AmountView(button.subscription)
        }

        Column {
            card.featureItems.forEach {
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
            UpgradeButton.PlanType.SUBSCRIBE -> {
                when (button.subscription) {
                    is Subscription.Simple -> stringResource(LR.string.subscribe_to, stringResource(button.shortNameRes))
                    is Subscription.Trial -> { stringResource(LR.string.trial_start) }
                    is Subscription.Intro -> { stringResource(LR.string.subscribe_to, stringResource(button.shortNameRes)) }
                    else -> { stringResource(LR.string.subscribe_to, stringResource(button.shortNameRes)) }
                }
            }
            UpgradeButton.PlanType.UPGRADE -> stringResource(LR.string.upgrade_to, stringResource(button.shortNameRes))
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

@Composable
private fun AmountView(
    subscription: Subscription,
) {
    Text(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        color = MaterialTheme.theme.colors.primaryText01,
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.W700)) {
                append("${subscription.recurringPricingPhase.formattedPrice} ")
            }

            withStyle(style = SpanStyle(fontWeight = FontWeight.W400)) {
                append(
                    stringResource(subscription.recurringPricingPhase.perPeriod)
                        .lowercase(Locale.getDefault()),
                )
            }
        },
    )
}
