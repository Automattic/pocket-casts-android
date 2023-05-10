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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.SubscriptionTierPill
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.IconRow
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ProfileUpgradeBannerViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ProfileUpgradeBannerViewModel.State
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalPagerWrapper
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
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

        is State.OldLoaded ->
            ProfileOldUpgradeBannerView(
                state = state as State.OldLoaded,
                onClick = onClick,
            )

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
            } else Modifier
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
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            SubscriptionTierPill(
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

        val primaryText = stringResource(LR.string.upgrade_to, stringResource(button.shortNameRes))
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
                        .lowercase(Locale.getDefault())
                )
            }
        }
    )
}

@Composable
fun ProfileOldUpgradeBannerView(
    state: State.OldLoaded,
    onClick: () -> Unit,
) {
    OnboardingUpgradeHelper.OldPlusBackground {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(24.dp))
            IconRow()

            Spacer(Modifier.height(16.dp))
            TextH60(
                text = stringResource(LR.string.plus_take_your_podcasting_to_next_level),
                color = Color.White,
            )

            state.numPeriodFree?.let { numPeriodFree ->
                Spacer(Modifier.height(16.dp))
                OnboardingUpgradeHelper.TopText(
                    topText = numPeriodFree,
                    subscriptionTier = SubscriptionTier.PLUS,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Not using a lazy grid here because lazy grids cannot be used inside a ScrollView
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OldPlusUpgradeFeatureItem.values().toList().chunked(2).forEach { chunk ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        chunk.forEach {
                            FeatureItemComposable(it, Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
            OnboardingUpgradeHelper.PlusRowButton(
                text = stringResource(LR.string.profile_upgrade_to_plus),
                onClick = onClick,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureItemComposable(
    item: OldPlusUpgradeFeatureItem,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(item.image),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(12.dp))
        TextH60(
            text = stringResource(item.title),
            color = Color.White,
        )
    }
}
