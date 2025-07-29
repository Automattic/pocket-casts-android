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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradePlanRow
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PrivacyPolicy
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual.FoldersAnimation
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule.RecurrenceMode
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingUpgradeScreen(
    state: OnboardingUpgradeFeaturesState.Loaded,
    source: OnboardingUpgradeSource,
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
            .fillMaxSize(),
    ) {
        UpgradeHeader(
            modifier = Modifier.padding(
                horizontal = 24.dp,
            ),
            selectedPlan = state.selectedPlan,
            source = source,
            onClosePress = onClosePress,
        )
        Spacer(modifier = Modifier.height(12.dp))
        UpgradeContent(
            modifier = Modifier.weight(1f),
            pages = state.onboardingVariant.toContentPages(
                currentPlan = state.selectedPlan,
                isEligibleForTrial = state.selectedBasePlan.offer == SubscriptionOffer.Trial,
                plan = state.selectedBasePlan,
                source = source,
            ),
        )
        Spacer(modifier = Modifier.height(24.dp))
        UpgradeFooter(
            modifier = Modifier
                .padding(
                    horizontal = 24.dp,
                )
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
        plans.forEachIndexed { index, item ->
            UpgradePlanRow(
                plan = item,
                isSelected = selectedOnboardingPlan.key == item.key,
                onClick = { onSelectedChange(item) },
                priceComparisonPlan = plans.getOrNull(index + 1),
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
    source: OnboardingUpgradeSource,
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
            text = stringResource(selectedPlan.customFeatureTitle(source)),
            color = MaterialTheme.theme.colors.primaryText01,
        )
    }
}

@Composable
private fun SubscriptionPlan.trialSchedule(): List<UpgradeTrialItem> {
    val offerPlan = this as? SubscriptionPlan.WithOffer ?: return emptyList()

    val discountedPhase = offerPlan.pricingPhases.find { it.schedule.recurrenceMode is RecurrenceMode.Recurring } ?: return emptyList()

    val recurringPeriods = (discountedPhase.schedule.recurrenceMode as RecurrenceMode.Recurring).value
    val chronoUnit = when (discountedPhase.schedule.period) {
        PricingSchedule.Period.Daily -> ChronoUnit.DAYS
        PricingSchedule.Period.Weekly -> ChronoUnit.WEEKS
        PricingSchedule.Period.Monthly -> ChronoUnit.MONTHS
        PricingSchedule.Period.Yearly -> ChronoUnit.YEARS
    }
    val now = ZonedDateTime.now()
    val dateFromNow = now.plus(recurringPeriods.toLong(), chronoUnit)
    val formattedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(dateFromNow)
    val daysFromNow = ChronoUnit.DAYS.between(now, dateFromNow)

    return listOf(
        UpgradeTrialItem(
            iconResId = R.drawable.ic_unlocked,
            title = stringResource(LR.string.onboarding_upgrade_schedule_today),
            message = stringResource(LR.string.onboarding_upgrade_schedule_today_message),
        ),
        UpgradeTrialItem(
            iconResId = R.drawable.ic_envelope,
            title = stringResource(LR.string.onboarding_upgrade_schedule_day, daysFromNow - 7),
            message = stringResource(LR.string.onboarding_upgrade_schedule_notify),
        ),
        UpgradeTrialItem(
            iconResId = R.drawable.ic_star,
            title = stringResource(LR.string.onboarding_upgrade_schedule_day, daysFromNow),
            message = stringResource(LR.string.onboarding_upgrade_schedule_billing, formattedDate),
        ),
    )
}

@Composable
private fun OnboardingUpgradeFeaturesState.NewOnboardingVariant.toContentPages(
    currentPlan: OnboardingSubscriptionPlan,
    source: OnboardingUpgradeSource,
    isEligibleForTrial: Boolean,
    plan: SubscriptionPlan,
) = buildList {
    when (source) {
        OnboardingUpgradeSource.FOLDERS_PODCAST_SCREEN,
        OnboardingUpgradeSource.SUGGESTED_FOLDERS,
        OnboardingUpgradeSource.FOLDERS,
        -> {
            add(UpgradePagerContent.Folders)
            add(
                UpgradePagerContent.Features(
                    features = currentPlan.featureItems,
                    showCta = false,
                ),
            )
        }

        else -> {
            when (this@toContentPages) {
                OnboardingUpgradeFeaturesState.NewOnboardingVariant.FEATURES_FIRST -> {
                    add(
                        UpgradePagerContent.Features(
                            features = currentPlan.featureItems,
                            showCta = isEligibleForTrial,
                        ),
                    )
                    if (isEligibleForTrial) {
                        add(
                            UpgradePagerContent.TrialSchedule(
                                timelineItems = plan.trialSchedule(),
                                showCta = false,
                            ),
                        )
                    }
                }

                OnboardingUpgradeFeaturesState.NewOnboardingVariant.TRIAL_FIRST_WHEN_ELIGIBLE -> {
                    if (isEligibleForTrial) {
                        add(
                            UpgradePagerContent.TrialSchedule(
                                timelineItems = plan.trialSchedule(),
                                showCta = true,
                            ),
                        )
                    }
                    add(
                        UpgradePagerContent.Features(
                            features = currentPlan.featureItems,
                            showCta = false,
                        ),
                    )
                }
            }
        }
    }
}

private sealed interface UpgradePagerContent {
    val showCta: Boolean

    data class Features(val features: List<UpgradeFeatureItem>, override val showCta: Boolean) : UpgradePagerContent
    data class TrialSchedule(val timelineItems: List<UpgradeTrialItem>, override val showCta: Boolean) : UpgradePagerContent
    data object Folders : UpgradePagerContent {
        override val showCta get() = true
    }
}

@Composable
private fun UpgradeContent(
    pages: List<UpgradePagerContent>,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    FadedLazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        itemsIndexed(pages) { index, page ->
            val scrollToNext: () -> Unit = {
                coroutineScope.launch {
                    listState.animateScrollToItem((index + 1) % pages.size)
                }
            }
            when (page) {
                is UpgradePagerContent.Features -> FeaturesContent(
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                    ),
                    features = page,
                    onCtaClick = scrollToNext,
                )

                is UpgradePagerContent.TrialSchedule -> ScheduleContent(
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                    ),
                    trialSchedule = page,
                    onCtaClick = scrollToNext,
                )

                is UpgradePagerContent.Folders -> FoldersUpgradeContent(onCtaClick = scrollToNext)
            }
        }
    }
}

@Composable
private fun FeaturesContent(
    features: UpgradePagerContent.Features,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        features.features.forEach { item ->
            UpgradeFeatureItem(
                item = item,
                iconColor = MaterialTheme.theme.colors.primaryText01,
                textColor = MaterialTheme.theme.colors.secondaryText02,
            )
        }
        if (features.showCta) {
            TextP40(
                text = stringResource(LR.string.onboarding_upgrade_features_trial_schedule),
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .clickable { onCtaClick() },
                color = MaterialTheme.theme.colors.primaryInteractive01,
            )
        }
    }
}

@Composable
private fun ScheduleContent(
    trialSchedule: UpgradePagerContent.TrialSchedule,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        UpgradeTrialTimeline(
            items = trialSchedule.timelineItems,
        )
        if (trialSchedule.showCta) {
            TextP40(
                text = stringResource(LR.string.onboarding_upgrade_schedule_see_features),
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .clickable { onCtaClick() },
                color = MaterialTheme.theme.colors.primaryInteractive01,
            )
        }
    }
}

@Composable
private fun FoldersUpgradeContent(
    onCtaClick: () -> Unit,
) {
    Column {
        TextP40(
            text = stringResource(LR.string.onboarding_upgrade_schedule_see_features),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .clickable { onCtaClick() },
            color = MaterialTheme.theme.colors.primaryInteractive01,
        )

        FoldersAnimation(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp),
        )
    }
}

@Preview
@Composable
private fun PreviewOnboardingUpgradeScreen(
    @PreviewParameter(ThemedTierParameterProvider::class) pair: Pair<ThemeType, SubscriptionTier>,
) {
    AppThemeWithBackground(pair.first) {
        OnboardingUpgradeScreen(
            state = OnboardingUpgradeFeaturesState.Loaded(
                selectedTier = pair.second,
                selectedBillingCycle = BillingCycle.Yearly,
                subscriptionPlans = SubscriptionPlans.Preview,
                plansFilter = when (pair.second) {
                    SubscriptionTier.Plus -> OnboardingUpgradeFeaturesState.LoadedPlansFilter.PLUS_ONLY
                    SubscriptionTier.Patron -> OnboardingUpgradeFeaturesState.LoadedPlansFilter.PATRON_ONLY
                },
                purchaseFailed = false,
                onboardingVariant = OnboardingUpgradeFeaturesState.NewOnboardingVariant.FEATURES_FIRST,
            ),
            modifier = Modifier.fillMaxSize(),
            onSubscribePress = {},
            onClosePress = {},
            onClickPrivacyPolicy = {},
            onClickTermsAndConditions = {},
            onChangeSelectedPlan = {},
            source = OnboardingUpgradeSource.ACCOUNT_DETAILS,
        )
    }
}

private class ThemedTierParameterProvider : PreviewParameterProvider<Pair<ThemeType, SubscriptionTier>> {
    override val values = ThemeType.entries.map { theme ->
        SubscriptionTier.entries.map { tier ->
            theme to tier
        }
    }.flatten()
        .asSequence()
}
