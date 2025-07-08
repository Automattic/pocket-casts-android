package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradePlanRow
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PrivacyPolicy
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingUpgradeScreen(
    onClosePress: () -> Unit,
    onSubscribePress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val plans = listOf(
        SubscriptionPlan.PlusYearlyPreview,
        SubscriptionPlan.PlusMonthlyPreview,
    )

    var selectedPlan by remember { mutableStateOf(plans[0]) }

    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(color = MaterialTheme.colors.background)
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
            ),
    ) {
        UpgradeHeader(
            selectedPlan = selectedPlan,
            onClosePress = onClosePress,
        )
        Spacer(modifier = Modifier.height(24.dp))
        UpgradeContent(
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        UpgradeFooter(
            modifier = Modifier
                .fillMaxWidth(),
            plans = plans,
            selectedPlan = selectedPlan,
            onSelectedChange = { selectedPlan = it },
            onClickSubscribe = { onSubscribePress() },
        )
    }
}

@Composable
private fun UpgradeFooter(
    plans: List<SubscriptionPlan.Base>,
    selectedPlan: SubscriptionPlan.Base,
    onSelectedChange: (SubscriptionPlan.Base) -> Unit,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedOnboardingPlan = remember(selectedPlan) { OnboardingSubscriptionPlan.create(selectedPlan) }

    Column(
        modifier = modifier,
    ) {
        plans.forEach { item ->
            UpgradePlanRow(
                plan = item,
                isSelected = selectedPlan == item,
                onClick = { onSelectedChange(item) },
                priceComparisonPlan = SubscriptionPlan.PlusMonthlyPreview,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        UpgradeRowButton(
            primaryText = selectedOnboardingPlan.ctaButtonText(isRenewingSubscription = false),
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
    selectedPlan: SubscriptionPlan.Base,
    onClosePress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedOnboardingPlan = remember(selectedPlan) { OnboardingSubscriptionPlan.create(selectedPlan) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SubscriptionBadge(
                iconRes = selectedOnboardingPlan.badgeIconRes,
                shortNameRes = selectedOnboardingPlan.shortNameRes,
                backgroundColor = Color.Black,
                textColor = Color.White,
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
            )
            IconButton(
                onClick = onClosePress,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = MaterialTheme.theme.colors.primaryUi05, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(LR.string.close),
                        tint = MaterialTheme.theme.colors.primaryIcon01,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextH10(
            text = stringResource(LR.string.onboarding_upgrade_generic_title),
            color = MaterialTheme.theme.colors.primaryText01,
        )
    }
}

@Composable
private fun UpgradeContent(
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()
    VerticalPager(
        modifier = modifier,
        state = pagerState,
    ) { page ->
        if (page == 0) {
            FeaturesContent(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
            )
        } else {
            ScheduleContent(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
            )
        }
    }
}

@Composable
private fun FeaturesContent(
    onClick: (() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp),
    ) {
        TextH40(text = "Features content", modifier = Modifier.clickable { onClick?.invoke() })
    }
}

@Composable
private fun ScheduleContent(
    onClick: (() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp),
    ) {
        TextH40(text = "Schedule content", modifier = Modifier.clickable { onClick?.invoke() })
    }
}

@Preview
@Composable
private fun PreviewOnboardingUpgradeScreen(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        OnboardingUpgradeScreen(
            modifier = Modifier.fillMaxSize(),
            onSubscribePress = {},
            onClosePress = {},
        )
    }
}
