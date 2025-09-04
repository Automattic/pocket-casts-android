package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import UpgradeTrialItem
import UpgradeTrialTimeline
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradePlanRow
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PrivacyPolicy
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual.BookmarksAnimation
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual.FoldersAnimation
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual.PreselectChaptersAnimation
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual.ShuffleAnimation
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
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
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
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
    BoxWithConstraints(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(color = MaterialTheme.colors.background)
            .fillMaxSize(),
    ) {
        if (this.maxHeight <= 480.dp || (this.maxHeight <= 640.dp && LocalConfiguration.current.fontScale >= 1.5f)) {
            CompactHeightUpscaledFontUpgradeScreen(
                state = state,
                source = source,
                onClosePress = onClosePress,
                onSubscribePress = onSubscribePress,
                onChangeSelectedPlan = onChangeSelectedPlan,
                onClickPrivacyPolicy = onClickPrivacyPolicy,
                onClickTermsAndConditions = onClickTermsAndConditions,
            )
        } else {
            RegularUpgradeScreen(
                state = state,
                source = source,
                onClosePress = onClosePress,
                onSubscribePress = onSubscribePress,
                onChangeSelectedPlan = onChangeSelectedPlan,
                onClickPrivacyPolicy = onClickPrivacyPolicy,
                onClickTermsAndConditions = onClickTermsAndConditions,
            )
        }
    }
}

@Composable
private fun CompactHeightUpscaledFontUpgradeScreen(
    state: OnboardingUpgradeFeaturesState.Loaded,
    source: OnboardingUpgradeSource,
    onClosePress: () -> Unit,
    onSubscribePress: () -> Unit,
    onChangeSelectedPlan: (SubscriptionPlan) -> Unit,
    onClickPrivacyPolicy: () -> Unit,
    onClickTermsAndConditions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val contentPages = state.onboardingVariant.toContentPages(
        currentPlan = state.selectedPlan,
        isEligibleForTrial = state.selectedBasePlan.offer == SubscriptionOffer.Trial && FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE_TRIAL_TIMELINE),
        plan = state.selectedBasePlan,
        source = source,
    )
    val listState = rememberScrollState()

    Column(
        modifier = modifier,
    ) {
        UpgradeHeader(
            modifier = Modifier.padding(
                horizontal = 24.dp,
            ),
            selectedPlan = state.selectedPlan,
            source = source,
            onClosePress = onClosePress,
        )
        Box {
            Column(
                modifier = Modifier
                    .verticalScroll(listState),
            ) {
                contentPages.forEachIndexed { index, content ->
                    var contentSize by remember { mutableIntStateOf(0) }
                    val scrollToNext: () -> Unit = {
                        coroutineScope.launch {
                            listState.animateScrollTo(((index + 1) % contentPages.size) * contentSize)
                        }
                    }
                    content.toComponent(
                        index = index,
                        scrollToNext = scrollToNext,
                        modifier = Modifier
                            .height(IntrinsicSize.Min)
                            .padding(top = if (index == 0) 32.dp else 0.dp)
                            .onSizeChanged {
                                contentSize = it.height
                            },
                    )()
                }
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
                    selfFocusRequester = FocusRequester.Default,
                    upFocusRequester = FocusRequester.Default,
                )
            }
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            0f to MaterialTheme.colors.background,
                            .15f to MaterialTheme.colors.background,
                            1f to Color.Transparent,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun RegularUpgradeScreen(
    state: OnboardingUpgradeFeaturesState.Loaded,
    source: OnboardingUpgradeSource,
    onClosePress: () -> Unit,
    onSubscribePress: () -> Unit,
    onChangeSelectedPlan: (SubscriptionPlan) -> Unit,
    onClickPrivacyPolicy: () -> Unit,
    onClickTermsAndConditions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (contentFocusRequester, footerFocusRequester) = remember { FocusRequester.createRefs() }

    Column(
        modifier = modifier,
    ) {
        UpgradeHeader(
            modifier = Modifier.padding(
                horizontal = 24.dp,
            ),
            selectedPlan = state.selectedPlan,
            source = source,
            onClosePress = onClosePress,
        )
        UpgradeContent(
            modifier = Modifier.weight(weight = 1f),
            pages = state.onboardingVariant.toContentPages(
                currentPlan = state.selectedPlan,
                isEligibleForTrial = state.selectedBasePlan.offer == SubscriptionOffer.Trial && FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE_TRIAL_TIMELINE),
                plan = state.selectedBasePlan,
                source = source,
            ),
            selfFocusRequester = contentFocusRequester,
            downFocusRequester = footerFocusRequester,
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
            selfFocusRequester = footerFocusRequester,
            upFocusRequester = contentFocusRequester,
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
    upFocusRequester: FocusRequester,
    selfFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .focusRequester(selfFocusRequester)
            .focusProperties {
                onExit = {
                    if (requestedFocusDirection == FocusDirection.Up) {
                        upFocusRequester.requestFocus()
                    }
                }
            },
    ) {
        plans.forEachIndexed { index, item ->
            UpgradePlanRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                plan = item,
                isSelected = selectedOnboardingPlan.key == item.key,
                onClick = { onSelectedChange(item) },
                priceComparisonPlan = plans.getOrNull(index + 1),
            )
            if (index < plans.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp))
            }
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
                .padding(horizontal = 16.dp)
                .heightIn(min = 48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        PrivacyPolicy(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 12.dp),
            color = MaterialTheme.theme.colors.secondaryText02,
            textAlign = TextAlign.Center,
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onTermsAndConditionsClick = onTermsAndConditionsClick,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.W600,
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
                        painter = painterResource(IR.drawable.ic_close),
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

        OnboardingUpgradeSource.BOOKMARKS,
        OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
        -> {
            add(UpgradePagerContent.Bookmarks)
            add(
                UpgradePagerContent.Features(
                    features = currentPlan.featureItems,
                    showCta = false,
                ),
            )
        }

        OnboardingUpgradeSource.UP_NEXT_SHUFFLE -> {
            add(UpgradePagerContent.Shuffle)
            add(
                UpgradePagerContent.Features(
                    features = currentPlan.featureItems,
                    showCta = false,
                ),
            )
        }

        OnboardingUpgradeSource.SKIP_CHAPTERS -> {
            add(UpgradePagerContent.PreselectChapters)
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

    data object Bookmarks : UpgradePagerContent {
        override val showCta get() = true
    }

    data object Shuffle : UpgradePagerContent {
        override val showCta get() = true
    }

    data object PreselectChapters : UpgradePagerContent {
        override val showCta get() = true
    }
}

@Composable
private fun UpgradeContent(
    pages: List<UpgradePagerContent>,
    selfFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var contentHeight by remember { mutableIntStateOf(0) }
    val backgroundColor = MaterialTheme.colors.background

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val itemHeight = this@BoxWithConstraints.maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(selfFocusRequester)
                .focusProperties {
                    onExit = {
                        if (this.requestedFocusDirection == FocusDirection.Down) {
                            downFocusRequester.requestFocus()
                        }
                    }
                }
                .verticalScroll(scrollState)
                .onSizeChanged {
                    contentHeight = it.height
                },
        ) {
            pages.forEachIndexed { index, content ->
                val bringIntoViewRequester = remember { BringIntoViewRequester() }

                val baseModifier = Modifier
                    .heightIn(min = itemHeight)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                        }
                    }
                    .padding(
                        top = if (index == 0) {
                            18.dp
                        } else {
                            0.dp
                        },
                    )
                val scrollToNext: () -> Unit = {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(((index + 1) % pages.size) * contentHeight)
                    }
                }
                content.toComponent(index = index, scrollToNext = scrollToNext, modifier = baseModifier)()
            }
        }
        Box(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        0f to backgroundColor,
                        .3f to backgroundColor,
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        .3f to Color.Transparent,
                        1f to backgroundColor,
                    ),
                ),
        )
    }
}

@Composable
private fun UpgradePagerContent.toComponent(
    index: Int,
    scrollToNext: () -> Unit,
    modifier: Modifier = Modifier,
): @Composable () -> Unit {
    val topPaddingForGenericContent = if (index != 0) 16.dp else 0.dp
    return {
        when (this) {
            is UpgradePagerContent.Features -> FeaturesContent(
                modifier = modifier
                    .padding(
                        horizontal = 24.dp,
                    )
                    .padding(
                        top = topPaddingForGenericContent,
                    ),
                features = this,
                onCtaClick = scrollToNext,
            )

            is UpgradePagerContent.TrialSchedule -> ScheduleContent(
                modifier = modifier
                    .padding(
                        horizontal = 24.dp,
                    )
                    .padding(
                        top = topPaddingForGenericContent,
                    ),
                trialSchedule = this,
                onCtaClick = scrollToNext,
            )

            is UpgradePagerContent.Folders -> FoldersUpgradeContent(
                modifier = modifier,
                onCtaClick = scrollToNext,
            )

            is UpgradePagerContent.Bookmarks -> BookmarksUpgradeContent(
                modifier = modifier,
                onCtaClick = scrollToNext,
            )

            is UpgradePagerContent.Shuffle -> ShuffleUpgradeContent(
                modifier = modifier,
                onCtaClick = scrollToNext,
            )

            is UpgradePagerContent.PreselectChapters -> PreselectChaptersUpgradeContent(
                modifier = modifier,
                onCtaClick = scrollToNext,
            )
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
                iconSize = 18.dp,
                spacing = 12.dp,
            )
        }
        if (features.showCta) {
            TextP40(
                text = stringResource(LR.string.onboarding_upgrade_features_trial_schedule),
                modifier = Modifier
                    .padding(vertical = 16.dp)
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TextP40(
            text = stringResource(LR.string.onboarding_upgrade_schedule_see_features),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .clickable { onCtaClick() },
            color = MaterialTheme.theme.colors.primaryInteractive01,
        )

        val isTablet = Util.isTablet(LocalContext.current)
        val widthFraction = if (isTablet) {
            0.7f
        } else {
            1f
        }
        val scaleFactor = if (isTablet) {
            1.4f
        } else {
            1f
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            FoldersAnimation(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .scale(scaleFactor),
            )
        }
    }
}

@Composable
private fun BookmarksUpgradeContent(
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TextP40(
            text = stringResource(LR.string.onboarding_upgrade_schedule_see_features),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .clickable { onCtaClick() },
            color = MaterialTheme.theme.colors.primaryInteractive01,
        )

        val isTablet = Util.isTablet(LocalContext.current)
        val widthFraction = if (isTablet) {
            0.6f
        } else {
            1f
        }
        val scaleFactor = if (isTablet) {
            1.8f
        } else {
            1f
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp * scaleFactor)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            BookmarksAnimation(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .scale(scaleFactor),
            )
        }
    }
}

@Composable
private fun ShuffleUpgradeContent(
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TextP40(
            text = stringResource(LR.string.onboarding_upgrade_schedule_see_features),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .clickable { onCtaClick() },
            color = MaterialTheme.theme.colors.primaryInteractive01,
        )

        val isTablet = Util.isTablet(LocalContext.current)
        val widthFraction = if (isTablet) {
            0.5f
        } else {
            1f
        }
        val scaleFactor = if (isTablet) {
            1.8f
        } else {
            1f
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp * scaleFactor)
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            ShuffleAnimation(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .scale(scaleFactor),
            )
        }
    }
}

@Composable
private fun PreselectChaptersUpgradeContent(
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TextP40(
            text = stringResource(LR.string.onboarding_upgrade_schedule_see_features),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .clickable { onCtaClick() },
            color = MaterialTheme.theme.colors.primaryInteractive01,
        )

        val isTablet = Util.isTablet(LocalContext.current)
        val widthFraction = if (isTablet) {
            0.45f
        } else {
            1f
        }
        val scaleFactor = if (isTablet) {
            2f
        } else {
            1f
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp * scaleFactor)
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            PreselectChaptersAnimation(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .scale(scaleFactor),
            )
        }
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

@Preview(fontScale = 2f, heightDp = 360)
@Composable
private fun PreviewOnboardingUpgradeScreenSmall(
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
