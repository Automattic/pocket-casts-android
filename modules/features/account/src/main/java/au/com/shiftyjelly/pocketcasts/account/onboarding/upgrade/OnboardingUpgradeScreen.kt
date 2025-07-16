package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import UpgradeTrialItem
import UpgradeTrialTimeline
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
import androidx.compose.runtime.rememberCoroutineScope
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
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradePlanRow
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PrivacyPolicy
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class Variants {
    VARIANT_FEATURES,
    VARIANT_TRIAL_TIMELINE,
}

@Composable
fun OnboardingUpgradeScreen(
    state: OnboardingUpgradeFeaturesState.Loaded,
    variant: Variants,
    onClosePress: () -> Unit,
    onSubscribePress: () -> Unit,
    onChangeSelectedPlan: (SubscriptionPlan) -> Unit,
    onClickPrivacyPolicy: () -> Unit,
    onClickTermsAndConditions: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            selectedPlan = state.selectedPlan,
            onClosePress = onClosePress,
        )
        Spacer(modifier = Modifier.height(24.dp))
        UpgradeContent(
            modifier = Modifier.weight(1f),
            pages = variant.toContentPages(
                currentPlan = state.selectedPlan,
                isEligibleForTrial = state.availableBasePlans.any { it.offer == SubscriptionOffer.Trial },
            ),
        )
        Spacer(modifier = Modifier.height(24.dp))
        UpgradeFooter(
            modifier = Modifier
                .fillMaxWidth(),
            plans = state.availableBasePlans,
            selectedOnboardingPlan = state.selectedPlan,
            onSelectedChange = {
                onChangeSelectedPlan(it)
            },
            onClickSubscribe = onSubscribePress,
            onPrivacyPolicyClick = onClickPrivacyPolicy,
            onTermsAndConditionsClick = onClickTermsAndConditions,
        )
    }
}

@Composable
private fun UpgradeFooter(
    plans: List<SubscriptionPlan>,
    onSelectedChange: (SubscriptionPlan) -> Unit,
    selectedOnboardingPlan: OnboardingSubscriptionPlan,
    onClickSubscribe: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsAndConditionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        plans.forEach { item ->
            UpgradePlanRow(
                plan = item,
                isSelected = selectedOnboardingPlan.key == item.key,
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
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onTermsAndConditionsClick = onTermsAndConditionsClick,
        )
    }
}

@Composable
private fun UpgradeHeader(
    selectedPlan: OnboardingSubscriptionPlan,
    onClosePress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SubscriptionBadge(
                iconRes = selectedPlan.badgeIconRes,
                shortNameRes = selectedPlan.shortNameRes,
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
        Spacer(modifier = Modifier.height(8.dp))
        TextH10(
            text = stringResource(LR.string.onboarding_upgrade_generic_title),
            color = MaterialTheme.theme.colors.primaryText01,
        )
    }
}

@Composable
private fun Variants.toContentPages(currentPlan: OnboardingSubscriptionPlan, isEligibleForTrial: Boolean) = buildList {
    when (this@toContentPages) {
        Variants.VARIANT_FEATURES -> {
            add(
                UpgradePagerContent.Features(
                    features = currentPlan.featureItems,
                    showCta = isEligibleForTrial,
                ),
            )
            if (isEligibleForTrial) {
                add(
                    UpgradePagerContent.TrialSchedule(
                        timelineItems = UpgradeTrialItem.getPreviewItems(),
                        showCta = false,
                    ),
                )
            }
        }

        Variants.VARIANT_TRIAL_TIMELINE -> {
            add(
                UpgradePagerContent.TrialSchedule(
                    timelineItems = UpgradeTrialItem.getPreviewItems(),
                    showCta = true,
                ),
            )
            add(
                UpgradePagerContent.Features(
                    features = currentPlan.featureItems,
                    showCta = false,
                ),
            )
        }
    }
}

private sealed interface UpgradePagerContent {
    val showCta: Boolean

    data class Features(val features: List<UpgradeFeatureItem>, override val showCta: Boolean) : UpgradePagerContent
    data class TrialSchedule(val timelineItems: List<UpgradeTrialItem>, override val showCta: Boolean) : UpgradePagerContent
}

@Composable
private fun UpgradeContent(
    pages: List<UpgradePagerContent>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val pagerState = rememberPagerState(initialPage = 0) { pages.size }
        val coroutineScope = rememberCoroutineScope()
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (val currentPage = pages[page]) {
                is UpgradePagerContent.Features -> FeaturesContent(
                    features = currentPage,
                    onCtaClick = { coroutineScope.launch { pagerState.animateScrollToPage(pages.size - page) } },
                )

                is UpgradePagerContent.TrialSchedule -> ScheduleContent(
                    trialSchedule = currentPage,
                    onCtaClick = { coroutineScope.launch { pagerState.animateScrollToPage(pages.size - page) } },
                )
            }
        }
    }
}

@Composable
private fun FeaturesContent(
    features: UpgradePagerContent.Features,
    onCtaClick: () -> Unit,
) {
    FadedLazyColumn {
        items(features.features.size) {
            UpgradeFeatureItem(
                item = features.features[it],
                iconColor = MaterialTheme.theme.colors.primaryText01,
                textColor = MaterialTheme.theme.colors.secondaryText02,
            )
        }
        if (features.showCta) {
            item {
                TextP40(
                    text = stringResource(LR.string.onboarding_upgrade_features_trial_schedule),
                    modifier = Modifier.padding(top = 24.dp)
                        .clickable { onCtaClick() },
                    color = MaterialTheme.theme.colors.primaryInteractive01,
                )
            }
        }
    }
}

@Composable
private fun ScheduleContent(
    trialSchedule: UpgradePagerContent.TrialSchedule,
    onCtaClick: () -> Unit,
) {
    FadedLazyColumn {
        item {
            UpgradeTrialTimeline(
                items = trialSchedule.timelineItems,
            )
        }
        if (trialSchedule.showCta) {
            item {
                TextP40(
                    text = stringResource(LR.string.onboarding_upgrade_schedule_see_features),
                    modifier = Modifier.padding(top = 24.dp)
                        .clickable { onCtaClick() },
                    color = MaterialTheme.theme.colors.primaryInteractive01,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewOnboardingUpgradeScreen(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        OnboardingUpgradeScreen(
            state = OnboardingUpgradeFeaturesState.Loaded(
                selectedTier = SubscriptionTier.Plus,
                selectedBillingCycle = BillingCycle.Yearly,
                subscriptionPlans = SubscriptionPlans.Preview,
                plansFilter = OnboardingUpgradeFeaturesState.LoadedPlansFilter.PLUS_ONLY,
                purchaseFailed = false,
            ),
            modifier = Modifier.fillMaxSize(),
            onSubscribePress = {},
            onClosePress = {},
            variant = Variants.VARIANT_FEATURES,
            onClickPrivacyPolicy = {},
            onClickTermsAndConditions = {},
            onChangeSelectedPlan = {},
        )
    }
}
