package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.ScheduleItemConnection
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradePlanSelector
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeTrialScheduleItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PrivacyPolicy
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.Price
import au.com.shiftyjelly.pocketcasts.payment.PricingPhase
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import java.math.BigDecimal
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingUpgradeVariantsScreen(
    onClosePressed: () -> Unit,
    onSubscribePressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(color = MaterialTheme.colors.background)) {
        IconButton(
            onClick = onClosePressed,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color = MaterialTheme.theme.colors.primaryUi05, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(IR.drawable.ic_close),
                    contentDescription = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.close),
                    tint = MaterialTheme.theme.colors.primaryIcon01,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
        ) {
            val mockPlan = OnboardingSubscriptionPlan.create(
                SubscriptionPlan.WithOffer(
                    name = "plus",
                    tier = SubscriptionTier.Plus,
                    billingCycle = BillingCycle.Yearly,
                    offer = SubscriptionOffer.Trial,
                    pricingPhases = listOf(
                        PricingPhase(
                            price = Price(
                                amount = BigDecimal.valueOf(0),
                                currencyCode = "USD",
                                formattedPrice = "$29.9",
                            ),
                            schedule = PricingSchedule(
                                recurrenceMode = PricingSchedule.RecurrenceMode.Recurring(1),
                                period = PricingSchedule.Period.Monthly,
                                periodCount = 1,
                            ),
                        ),
                        PricingPhase(
                            price = Price(
                                amount = BigDecimal.valueOf(39.9),
                                currencyCode = "USD",
                                formattedPrice = "$29.9",
                            ),
                            schedule = PricingSchedule(
                                recurrenceMode = PricingSchedule.RecurrenceMode.Infinite,
                                period = PricingSchedule.Period.Monthly,
                                periodCount = 11,
                            ),
                        ),
                    ),
                ),
            ).getOrNull()
            checkNotNull(mockPlan)
            UpgradeHeader(subscriptionPlan = mockPlan)
            Spacer(modifier = Modifier.height(24.dp))
            UpgradeContent(
                modifier = Modifier.weight(1f),
                subscriptionPlan = mockPlan,
            )
            Spacer(modifier = Modifier.height(16.dp))
            UpgradeFooter(
                modifier = Modifier
                    .fillMaxWidth(),
                selectedPlan = mockPlan,
                onClickSubscribe = onSubscribePressed,
            )
        }
    }
}

@Composable
private fun UpgradeFooter(
    selectedPlan: OnboardingSubscriptionPlan,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isYearlySelected by remember { mutableStateOf(true) }

    Column(
        modifier = modifier,
    ) {
        UpgradePlanSelector(
            plan = stringResource(LR.string.onboarding_upgrade_billing_cycle_yearly),
            priceAndPeriod = "$39.99/year",
            pricePerWeek = "$0.77/week",
            isSelected = isYearlySelected,
            onSelected = { isYearlySelected = true },
            savings = stringResource(LR.string.onboarding_upgrade_save_percent, 16),
        )
        Spacer(modifier = Modifier.height(10.dp))
        UpgradePlanSelector(
            plan = stringResource(LR.string.onboarding_upgrade_billing_cycle_monthly),
            priceAndPeriod = "$3.99/month",
            isSelected = !isYearlySelected,
            onSelected = { isYearlySelected = false },
        )
        Spacer(modifier = Modifier.height(16.dp))
        UpgradeRowButton(
            primaryText = selectedPlan.ctaButtonText(isRenewingSubscription = false),
            backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
            textColor = MaterialTheme.theme.colors.primaryInteractive02,
            fontWeight = FontWeight.W500,
            onClick = onClickSubscribe,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        PrivacyPolicy(
            color = MaterialTheme.theme.colors.secondaryText02,
            textAlign = TextAlign.Center,
            onPrivacyPolicyClick = {},
            onTermsAndConditionsClick = {},
        )
    }
}

@Composable
private fun UpgradeHeader(
    subscriptionPlan: OnboardingSubscriptionPlan,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SubscriptionBadge(
            iconRes = subscriptionPlan.badgeIconRes,
            shortNameRes = subscriptionPlan.shortNameRes,
            backgroundColor = Color.Black,
            textColor = Color.White,
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextH10(
            text = stringResource(LR.string.onboarding_upgrade_generic_title),
            color = MaterialTheme.theme.colors.primaryText01,
        )
    }
}

@Composable
private fun UpgradeContent(
    subscriptionPlan: OnboardingSubscriptionPlan,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val pagerState = rememberPagerState(initialPage = 0) { 2 }
        val coroutineScope = rememberCoroutineScope()
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                FeaturesContent(
                    featureList = subscriptionPlan.featureItems,
                    onShowScheduleClicked = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                )
            } else {
                ScheduleContent(
                    pricingPhase = subscriptionPlan.pricingPhase,
                    onShowFeaturesClicked = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FeaturesContent(
    featureList: List<UpgradeFeatureItem>,
    onShowScheduleClicked: (() -> Unit)? = null,
) {
    LazyColumn {
        featureList.forEach {
            item {
                UpgradeFeatureItem(
                    item = it,
                    textColor = MaterialTheme.theme.colors.secondaryText02,
                    iconColor = MaterialTheme.theme.colors.primaryText01,
                )
            }
        }
        item {
            onShowScheduleClicked?.let {
                Spacer(modifier = Modifier.height(24.dp))
                TextP40(
                    text = stringResource(LR.string.onboarding_upgrade_features_trial_schedule),
                    modifier = Modifier.clickable { it() },
                    color = MaterialTheme.theme.colors.primaryInteractive01,
                )
            }
        }
    }
}

@Composable
private fun ScheduleContent(
    pricingPhase: PricingPhase,
    onShowFeaturesClicked: (() -> Unit)? = null,
) {
    Column {
        val gradientColors = listOf(
            Color.Transparent,
            MaterialTheme.colors.background.copy(alpha = 0.8f),
        )
        val density = LocalDensity.current
        val iconSizePx = density.run { 43.dp.toPx() }
        Column(
            modifier = Modifier.drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = gradientColors,
                    ),
                    topLeft = Offset(x = 0f, y = 0f),
                    size = Size(width = iconSizePx, size.height),
                )
            },
        ) {
            UpgradeTrialScheduleItem(
                modifier = Modifier.heightIn(min = 64.dp),
                title = stringResource(LR.string.onboarding_upgrade_schedule_today),
                message = stringResource(LR.string.onboarding_upgrade_schedule_today_message),
                icon = painterResource(IR.drawable.ic_unlocked),
                connection = ScheduleItemConnection.BOTTOM,
            )
            UpgradeTrialScheduleItem(
                modifier = Modifier.heightIn(min = 64.dp),
                title = stringResource(LR.string.onboarding_upgrade_schedule_day, pricingPhase.schedule.periodCount - 7),
                message = stringResource(LR.string.onboarding_upgrade_schedule_notify),
                icon = painterResource(IR.drawable.ic_envelope),
                connection = ScheduleItemConnection.TOP_AND_BOTTOM,
            )
            UpgradeTrialScheduleItem(
                modifier = Modifier.heightIn(min = 64.dp),
                title = stringResource(LR.string.onboarding_upgrade_schedule_day, pricingPhase.schedule.periodCount),
                message = stringResource(LR.string.onboarding_upgrade_schedule_billing, "September 31th"),
                icon = painterResource(IR.drawable.ic_star),
                connection = ScheduleItemConnection.TOP,
            )
        }
        onShowFeaturesClicked?.let {
            Spacer(modifier = Modifier.height(24.dp))
            TextP40(
                text = stringResource(LR.string.onboarding_upgrade_schedule_see_features),
                modifier = Modifier.clickable { it() },
                color = MaterialTheme.theme.colors.primaryInteractive01,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewOnboardingUpgradeFeaturesScreen(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        OnboardingUpgradeVariantsScreen(
            modifier = Modifier.fillMaxSize(),
            onSubscribePressed = {},
            onClosePressed = {},
        )
    }
}
