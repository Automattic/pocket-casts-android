package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PrivacyPolicy
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
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
internal fun OnboardingUpgradeFeaturesScreen(
    onClosePressed: () -> Unit,
    onPlanChanged: (SubscriptionPlan) -> Unit,
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
            val create = OnboardingSubscriptionPlan.create(
                SubscriptionPlan.WithOffer(
                    name = "plus", tier = SubscriptionTier.Plus, billingCycle = BillingCycle.Yearly, offer = SubscriptionOffer.Trial, pricingPhases = listOf(
                        PricingPhase(
                            price = Price(
                                amount = BigDecimal.valueOf(0), currencyCode = "USD", formattedPrice = "$29.9"
                            ),
                            schedule = PricingSchedule(recurrenceMode = PricingSchedule.RecurrenceMode.Recurring(1), period = PricingSchedule.Period.Monthly, periodCount = 1)
                        ),
                        PricingPhase(
                            price = Price(
                                amount = BigDecimal.valueOf(39.9), currencyCode = "USD", formattedPrice = "$29.9"
                            ),
                            schedule = PricingSchedule(recurrenceMode = PricingSchedule.RecurrenceMode.Infinite, period = PricingSchedule.Period.Monthly, periodCount = 11)
                        ),
                    )
                )
            )
            UpgradeHeader(subscriptionPlan = create.getOrNull()!!)
            Spacer(modifier = Modifier.height(24.dp))
            UpgradeContent(
                modifier = Modifier.weight(1f),
                subscriptionPlan = create.getOrNull()!!
            )
            Spacer(modifier = Modifier.height(16.dp))
            UpgradeFooter(
                modifier = Modifier
                    .fillMaxWidth(),
                selectedPlan = create.getOrNull()!!,
                onClickSubscribe = onSubscribePressed,
            )
        }

    }
}

@Composable
private fun UpgradeFooter(
    selectedPlan: OnboardingSubscriptionPlan,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isYearlySelected by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
    ) {
        TierSelector(
            plan = stringResource(LR.string.onboarding_upgrade_billing_cycle_yearly),
            priceAndPeriod = "$39.99/year",
            pricePerWeek = "$0.77/week",
            isSelected = isYearlySelected,
            onSelected = { isYearlySelected = true },
            savings = stringResource(LR.string.onboarding_upgrade_save_percent, 16),
        )
        Spacer(modifier = Modifier.height(10.dp))
        TierSelector(
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
            onTermsAndConditionsClick = {}
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
    modifier: Modifier = Modifier
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
    Column {
        featureList.forEach {
            UpgradeFeatureItem(
                item = it,
                textColor = MaterialTheme.theme.colors.secondaryText02,
                iconColor = MaterialTheme.theme.colors.primaryText01,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        onShowScheduleClicked?.let {
            TextP40(
                text = stringResource(LR.string.onboarding_upgrade_features_trial_schedule),
                modifier = Modifier.clickable { it() },
                color = MaterialTheme.theme.colors.primaryInteractive01
            )
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
            MaterialTheme.colors.background.copy(alpha = 0.8f)
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
            ScheduleItem(
                modifier = Modifier.heightIn(min = 64.dp),
                title = stringResource(LR.string.onboarding_upgrade_schedule_today),
                message = stringResource(LR.string.onboarding_upgrade_schedule_today_message),
                icon = painterResource(IR.drawable.ic_unlocked),
                connection = ScheduleItemConnection.BOTTOM,
            )
            ScheduleItem(
                modifier = Modifier.heightIn(min = 64.dp),
                title = stringResource(LR.string.onboarding_upgrade_schedule_day, pricingPhase.schedule.periodCount - 7),
                message = stringResource(LR.string.onboarding_upgrade_schedule_notify),
                icon = painterResource(IR.drawable.ic_envelope),
                connection = ScheduleItemConnection.TOP_AND_BOTTOM,
            )
            ScheduleItem(
                modifier = Modifier.heightIn(min = 64.dp),
                title = stringResource(LR.string.onboarding_upgrade_schedule_day, pricingPhase.schedule.periodCount),
                message = stringResource(LR.string.onboarding_upgrade_schedule_billing, "September 31th"),
                icon = painterResource(IR.drawable.ic_star),
                connection = ScheduleItemConnection.TOP,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        onShowFeaturesClicked?.let {
            TextP40(
                text = stringResource(LR.string.onboarding_upgrade_schedule_see_features),
                modifier = Modifier.clickable { it() },
                color = MaterialTheme.theme.colors.primaryInteractive01
            )
        }
    }
}

@Composable
private fun ScheduleItem(
    title: String,
    message: String,
    icon: Painter,
    connection: ScheduleItemConnection,
    modifier: Modifier = Modifier
) {
    val iconBackground = MaterialTheme.theme.colors.primaryInteractive01
    val density = LocalDensity.current
    val connectingRodWidthPx = density.run { 8.dp.toPx() }
    val iconSize = 43.dp
    val iconSizePx = density.run { iconSize.toPx() }
    Row(
        modifier = modifier
            .drawWithContent {
                when (connection) {
                    ScheduleItemConnection.TOP -> drawRect(color = iconBackground, topLeft = Offset(x = iconSizePx / 2f - (connectingRodWidthPx / 2f), y = -1f), size = Size(width = connectingRodWidthPx, height = size.height / 2f))
                    ScheduleItemConnection.BOTTOM -> drawRect(color = iconBackground, topLeft = Offset(x = iconSizePx / 2f - (connectingRodWidthPx / 2f), y = size.height / 2f), size = Size(width = connectingRodWidthPx, height = (size.height / 2f) + 1f))
                    ScheduleItemConnection.TOP_AND_BOTTOM -> drawRect(color = iconBackground, topLeft = Offset(x = iconSizePx / 2f - (connectingRodWidthPx / 2f), y = 0f), size = Size(width = connectingRodWidthPx, height = size.height))
                }
                drawContent()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(color = iconBackground, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                tint = MaterialTheme.colors.background,
                contentDescription = "",
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            TextP50(text = title, color = MaterialTheme.theme.colors.primaryText01, fontWeight = FontWeight.W700)
            TextP50(text = message, color = MaterialTheme.theme.colors.secondaryText02)
        }
    }
}

private enum class ScheduleItemConnection {
    TOP,
    BOTTOM,
    TOP_AND_BOTTOM
}

@Composable
private fun TierSelector(
    plan: String,
    priceAndPeriod: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    savings: String? = null,
    pricePerWeek: String? = null,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val savingsBadge = savings?.let {
            val badgeMeasurable = subcompose("savings") {
                SavingsLabel(
                    savings = savings,
                )
            }
            badgeMeasurable.first().measure(constraints)
        }
        val badgeHeight = savingsBadge?.height ?: 0

        val row = subcompose("selector_row") {
            PlanRow(
                modifier = Modifier.fillMaxWidth(),
                plan = plan,
                priceAndPeriod = priceAndPeriod,
                isSelected = isSelected,
                onSelected = onSelected,
                pricePerWeek = pricePerWeek,
            )
        }
        val placeable = row.first().measure(constraints)
        val rowWidth = placeable.width
        val rowHeight = placeable.height

        val totalHeight = rowHeight + badgeHeight / 2
        layout(rowWidth, totalHeight) {
            placeable.placeRelative(x = 0, y = badgeHeight / 2)
            savingsBadge?.let {
                it.placeRelative(x = (rowWidth - it.width) / 2, y = 0)
            }
        }
    }
}

@Composable
private fun PlanRow(
    plan: String,
    priceAndPeriod: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    pricePerWeek: String? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.theme.colors.primaryInteractive01,
                        shape = RoundedCornerShape(12.dp),
                    )
                } else {
                    Modifier
                }
            )
            .background(
                color = MaterialTheme.theme.colors.primaryUi03,
            )
            .clickable { onSelected() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.background(color = MaterialTheme.theme.colors.primaryInteractive01)
                    } else Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.theme.colors.primaryIcon02,
                        shape = CircleShape,
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(IR.drawable.ic_check),
                    contentDescription = "",
                    tint = MaterialTheme.theme.colors.primaryInteractive02,
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            TextH40(
                text = plan,
                color = MaterialTheme.theme.colors.primaryText01,
                fontWeight = FontWeight.W700
            )
            TextH40(
                text = priceAndPeriod,
                color = MaterialTheme.theme.colors.secondaryText02
            )
        }
        pricePerWeek?.let {
            TextH40(
                text = pricePerWeek,
                color = MaterialTheme.theme.colors.secondaryText02,
            )
        }
    }
}

@Composable
private fun SavingsLabel(
    savings: String,
    modifier: Modifier = Modifier,
) {
    TextH50(
        modifier = modifier
            .background(
                color = MaterialTheme.theme.colors.primaryInteractive01,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 2.dp),
        text = savings,
        color = MaterialTheme.theme.colors.primaryInteractive02,
    )
}

@Preview
@Composable
private fun PreviewOnboardingUpgradeFeaturesScreen(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        OnboardingUpgradeFeaturesScreen(
            modifier = Modifier.fillMaxSize(),
            onSubscribePressed = {},
            onPlanChanged = {},
            onClosePressed = {},
        )
    }
}