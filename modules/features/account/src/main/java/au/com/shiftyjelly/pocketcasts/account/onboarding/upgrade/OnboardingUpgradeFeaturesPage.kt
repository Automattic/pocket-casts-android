package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.IconRow
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PlusOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PlusRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.UpgradeButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.UpgradeFeatureCard
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.StyledToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import java.lang.Long.max
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingUpgradeFeaturesPage(
    flow: OnboardingFlow,
    source: OnboardingUpgradeSource,
    onBackPressed: () -> Unit,
    onClickSubscribe: () -> Unit,
    onNotNowPressed: () -> Unit,
    onUpgradePressed: () -> Unit,
    canUpgrade: Boolean,
) {

    val viewModel = hiltViewModel<OnboardingUpgradeFeaturesViewModel>()
    val state by viewModel.state.collectAsState()

    @Suppress("NAME_SHADOWING")
    val onUpgradePressed = {
        viewModel.onUpgradePressed(flow, source)
        onUpgradePressed()
    }

    @Suppress("NAME_SHADOWING")
    val onNotNowPressed = {
        viewModel.onNotNow(flow, source)
        onNotNowPressed()
    }

    @Suppress("NAME_SHADOWING")
    val onBackPressed = {
        viewModel.onDismiss(flow, source)
        onBackPressed()
    }

    CallOnce {
        viewModel.onShown(flow, source)
    }

    val scrollState = rememberScrollState()
    setStatusBarBackground(scrollState)

    when (state) {
        is OnboardingUpgradeFeaturesState.Loading -> Unit // Do Nothing
        is OnboardingUpgradeFeaturesState.Loaded -> {
            val loadedState = state as OnboardingUpgradeFeaturesState.Loaded
            UpgradeLayout(
                state = loadedState,
                scrollState = scrollState,
                onBackPressed = onBackPressed,
                onNotNowPressed = onNotNowPressed,
                onSubscriptionFrequencyChanged = { viewModel.onSubscriptionFrequencyChanged(it) },
                onFeatureCardChanged = { viewModel.onFeatureCardChanged(loadedState.featureCards[it]) },
                onClickSubscribe = onClickSubscribe,
                canUpgrade = canUpgrade,
            )
        }

        is OnboardingUpgradeFeaturesState.OldLoaded -> {
            OldUpgradeLayout(
                state = state as OnboardingUpgradeFeaturesState.OldLoaded,
                scrollState = scrollState,
                onBackPressed = onBackPressed,
                onUpgradePressed = onUpgradePressed,
                onNotNowPressed = onNotNowPressed,
                canUpgrade = canUpgrade,
            )
        }

        is OnboardingUpgradeFeaturesState.NoSubscriptions -> {
            NoSubscriptionsLayout(
                showNotNow = (state as OnboardingUpgradeFeaturesState.NoSubscriptions).showNotNow,
                onBackPressed = onBackPressed,
                onNotNowPressed = onNotNowPressed,
            )
        }
    }
}

@Composable
private fun UpgradeLayout(
    state: OnboardingUpgradeFeaturesState.Loaded,
    scrollState: ScrollState,
    onBackPressed: () -> Unit,
    onNotNowPressed: () -> Unit,
    onSubscriptionFrequencyChanged: (SubscriptionFrequency) -> Unit,
    onFeatureCardChanged: (Int) -> Unit,
    onClickSubscribe: () -> Unit,
    canUpgrade: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Need this BoxWithConstraints so we can force the inner column to fill the screen with vertical scroll enabled
        BoxWithConstraints(
            Modifier
                .fillMaxHeight()
                .background(OnboardingUpgradeHelper.backgroundColor)
        ) {

            OnboardingUpgradeHelper.UpgradeBackground(
                modifier = Modifier.verticalScroll(scrollState),
                tier = state.currentFeatureCard.subscriptionTier,
                backgroundGlowsRes = state.currentFeatureCard.backgroundGlowsRes,
            ) {
                Column(
                    Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .heightIn(min = this.calculateMinimumHeightWithInsets())
                        .padding(bottom = 100.dp), // Added to allow scrolling feature cards beyond upgrade button in large font sizes
                ) {

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NavigationIconButton(
                            onNavigationClick = onBackPressed,
                            iconColor = Color.White,
                            modifier = Modifier
                                .height(48.dp)
                                .width(48.dp)
                        )
                        if (state.showNotNow) {
                            TextH30(
                                text = stringResource(LR.string.not_now),
                                color = Color.White,
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .clickable { onNotNowPressed() },
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Column {
                        Box(
                            modifier = Modifier.heightIn(min = 70.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AutoResizeText(
                                text = stringResource(state.currentFeatureCard.titleRes),
                                color = Color.White,
                                maxFontSize = 22.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.W700,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            StyledToggle(
                                items = state.subscriptionFrequencies
                                    .map { stringResource(id = it.localisedLabelRes) },
                                defaultSelectedItemIndex = state.subscriptionFrequencies.indexOf(
                                    state.currentSubscriptionFrequency
                                ),
                            ) {
                                val selectedFrequency = state.subscriptionFrequencies[it]
                                onSubscriptionFrequencyChanged(selectedFrequency)
                            }
                        }

                        FeatureCards(
                            state = state,
                            onFeatureCardChanged = onFeatureCardChanged,
                        )
                    }

                    Spacer(Modifier.weight(1f))
                }
            }
        }

        if (canUpgrade) {
            UpgradeButton(
                button = state.currentUpgradeButton,
                onClickSubscribe = onClickSubscribe,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeatureCards(
    state: OnboardingUpgradeFeaturesState.Loaded,
    onFeatureCardChanged: (Int) -> Unit,
) {
    val pagerState =
        rememberPagerState(initialPage = state.featureCards.indexOf(state.currentFeatureCard))
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { index ->
            onFeatureCardChanged(index)
        }
    }

    var pagerHeight by remember { mutableStateOf(0) }
    HorizontalPager(
        pageCount = state.featureCards.size,
        state = pagerState,
        pageSize = PageSize.Fixed(LocalConfiguration.current.screenWidthDp.dp - 64.dp),
        contentPadding = PaddingValues(horizontal = 32.dp),
    ) { index ->
        var pageHeight by remember { mutableStateOf(0) }
        Box(
            Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    /* https://github.com/google/accompanist/issues/1050#issuecomment-1097483476 */
                    val placeable = measurable.measure(constraints)
                    pageHeight = placeable.height
                    /* Restrict page height to the pager height */
                    layout(constraints.maxWidth, pagerHeight) {
                        placeable.placeRelative(0, 0)
                    }
                }
                .onGloballyPositioned {
                    /* Update pager height to the tallest page */
                    if (pageHeight > pagerHeight) {
                        pagerHeight = pageHeight
                    }
                }
        ) {
            FeatureCard(
                card = state.featureCards[index],
                modifier = if (pagerHeight > 0) {
                    Modifier.height(pagerHeight.pxToDp(LocalContext.current).dp)
                } else Modifier
            )
        }
    }

    if (state.showPageIndicator) {
        Row(
            Modifier
                .height(40.dp)
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(state.featureCards.size) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
fun FeatureCard(
    card: UpgradeFeatureCard,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp,
        backgroundColor = Color.White,
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                FeaturePill(card.iconRes, card.shortNameRes)
            }

            Column {
                card.featureItems.forEach {
                    FeatureItem(it)
                }
                Spacer(modifier = Modifier.weight(1f))
                OnboardingUpgradeHelper.PrivacyPolicy(
                    color = Color.Black.copy(alpha = .5f),
                    textAlign = TextAlign.Start,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun FeaturePill(
    @DrawableRes iconRes: Int,
    @StringRes shortNameRes: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(800.dp),
        backgroundColor = Color.Black,
    ) {
        Row(
            modifier = modifier
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = modifier
                    .size(16.dp),
                tint = Color.Unspecified,
            )
            Spacer(Modifier.width(4.dp))
            TextH50(
                text = stringResource(shortNameRes),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun FeatureItem(
    content: UpgradeFeatureItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(content.image),
            contentDescription = null,
            tint = Color.Black,
            modifier = modifier
                .size(20.dp)
                .padding(2.dp),
        )
        Spacer(Modifier.width(16.dp))
        TextH50(
            text = stringResource(content.title),
            color = Color.Black,
        )
    }
}

@Composable
private fun UpgradeButton(
    button: UpgradeButton,
    onClickSubscribe: () -> Unit,
) {
    val resources = LocalContext.current.resources
    val subscription = button.subscription
    val shortName = resources.getString(button.shortNameRes)
    val primaryText = when (subscription) {
        is Subscription.Simple -> stringResource(LR.string.subscribe_to, shortName)
        is Subscription.WithTrial -> stringResource(LR.string.trial_start)
    }
    val secondaryText = when (subscription) {
        is Subscription.Simple -> subscription.recurringPricingPhase.pricePerPeriod(resources)
        is Subscription.WithTrial -> subscription.tryFreeThenPricePerPeriod(resources)
    }
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fadeBackground()
    ) {
        Column {
            UpgradeRowButton(
                primaryText = primaryText,
                secondaryText = secondaryText,
                backgroundColor = button.backgroundColor,
                textColor = button.textColor,
                onClick = onClickSubscribe,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .fillMaxWidth(),
            )
            Spacer(
                modifier = Modifier
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
            )
        }
    }
}

@Composable
private fun OldUpgradeLayout(
    state: OnboardingUpgradeFeaturesState.OldLoaded,
    scrollState: ScrollState,
    onBackPressed: () -> Unit,
    onUpgradePressed: () -> Unit,
    onNotNowPressed: () -> Unit,
    canUpgrade: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxHeight()
            .background(OnboardingUpgradeHelper.backgroundColor)
    ) {
        OnboardingUpgradeHelper.OldPlusBackground(Modifier.verticalScroll(scrollState)) {
            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .heightIn(min = this.calculateMinimumHeightWithInsets())
            ) {

                Spacer(Modifier.height(8.dp))
                NavigationIconButton(
                    onNavigationClick = onBackPressed,
                    iconColor = Color.White,
                    modifier = Modifier
                        .height(48.dp)
                        .width(48.dp)
                )

                Spacer(Modifier.height(12.dp))

                IconRow(Modifier.padding(horizontal = 24.dp))

                Spacer(Modifier.height(36.dp))

                TextH10(
                    text = stringResource(LR.string.onboarding_upgrade_everything_you_love_about_pocket_casts_plus),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(Modifier.height(12.dp))

                TextP30(
                    text = stringResource(LR.string.onboarding_upgrade_exclusive_features_and_options),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(Modifier.height(58.dp))

                FeatureRow(scrollAutomatically = state.scrollAutomatically)

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(36.dp))

                if (canUpgrade) {
                    PlusRowButton(
                        text = stringResource(LR.string.onboarding_upgrade_unlock_all_features),
                        onClick = onUpgradePressed,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                PlusOutlinedRowButton(
                    text = stringResource(LR.string.not_now),
                    onClick = onNotNowPressed,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun setStatusBarBackground(scrollState: ScrollState) {
    val systemUiController = rememberSystemUiController()
    val hasScrolled = scrollState.value > 0

    val scrimAlpha: Float by animateFloatAsState(
        targetValue = if (hasScrolled) 0.6f else 0f,
        animationSpec = tween(durationMillis = 400), label = "scrimAlpha"
    )

    val statusBarBackground = if (scrimAlpha > 0) {
        OnboardingUpgradeHelper.backgroundColor.copy(alpha = scrimAlpha)
    } else {
        Color.Transparent
    }

    LaunchedEffect(statusBarBackground) {
        systemUiController.apply {
            setStatusBarColor(statusBarBackground, darkIcons = false)
            setNavigationBarColor(Color.Transparent, darkIcons = false)
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.calculateMinimumHeightWithInsets(): Dp {
    val statusBarPadding = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val navigationBarPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val fullHeight = this.maxHeight
    return fullHeight - statusBarPadding - navigationBarPadding
}

@Composable
private fun FeatureRow(scrollAutomatically: Boolean) {

    // Not using rememberLazyListState() because we want to reset
    // the scroll state on orientation changes so that the hardcoded column
    // is redisplayed, which insures the height is correctly calculated. For that
    // reason, we want to use remember, not rememberSaveable.
    val state = remember { LazyListState() }

    val localConfiguration = LocalConfiguration.current
    LaunchedEffect(scrollAutomatically) {
        if (scrollAutomatically) {
            // This seems to get a good scroll speed across multiple devices
            val scrollDelay = max(1L, (1000L - localConfiguration.densityDpi) / 125)
            autoScroll(scrollDelay, state)
        }
    }
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = !scrollAutomatically,
    ) {
        if (scrollAutomatically) {
            items(500) { // arbitrary large number that users will probably never hit
                // Nesting a Row of FeatureItems inside the LazyRow because a Row can use IntrinsidSize.Max
                // to determine the height of the tallest list item and keep a consistent
                // height, regardless of which items are visible. This ensures that the
                // LazyRow as a whole always has a single, consistent height that does not
                // change as items scroll into/out-of view. If IntrinsicSize.Max could work
                // with LazyRows, we wouldn't need to nest Rows in the LazyRow.
                FeatureItems()
            }
        } else {
            item {
                FeatureItems()
            }
        }
    }
}

@Composable
private fun FeatureItems() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .height(IntrinsicSize.Max)
    ) {
        OldPlusUpgradeFeatureItem.values().forEach {
            OldFeatureItem(it)
        }
    }
}

@Composable
private fun OldFeatureItem(
    content: OldPlusUpgradeFeatureItem,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .border(
                width = 1.dp,
                color = Color(0xFF383839),
                shape = shape,
            )
            .background(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF2A2A2B),
                    1f to Color(0xFF252525),
                ),
                shape = shape,
            )
            .width(156.dp)
            .fillMaxHeight()
            .padding(all = 16.dp)
    ) {

        Icon(
            painter = painterResource(content.image),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        TextH40(
            text = stringResource(content.title),
            color = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        TextP60(
            text = stringResource(content.text),
            color = Color.White,
            modifier = Modifier.alpha(0.72f),
        )
    }
}

@Composable
fun NoSubscriptionsLayout(
    onBackPressed: () -> Unit,
    onNotNowPressed: () -> Unit,
    showNotNow: Boolean,
) {
    Column(
        Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxWidth()
    ) {

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationIconButton(
                onNavigationClick = onBackPressed,
                iconColor = Color.White,
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp)
            )
            if (showNotNow) {
                TextH30(
                    text = stringResource(LR.string.not_now),
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clickable { onNotNowPressed() },
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextH30(
                text = stringResource(id = LR.string.onboarding_upgrade_no_subscriptions_found)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

// Based on https://stackoverflow.com/a/71344813/1910286
private tailrec suspend fun autoScroll(
    scrollDelay: Long,
    lazyListState: LazyListState,
) {
    val scrollAmount = lazyListState.scrollBy(1f)
    if (scrollAmount == 0f) {
        // If we can't scroll, we're at the end, so jump to the beginning.
        // This will be an abrupt jump, but users shouldn't really ever be
        // getting to the end of the list, so it should be very rare.
        lazyListState.scrollToItem(0)
    }
    delay(scrollDelay)
    autoScroll(scrollDelay, lazyListState)
}

private fun Modifier.fadeBackground() = this
    .graphicsLayer { alpha = 0.99f }
    .drawWithCache {
        onDrawWithContent {
            drawRect(Color.Black)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black),
                ),
                blendMode = BlendMode.DstIn
            )
            drawContent()
        }
    }

@Preview
@Composable
private fun OnboardingPlusFeatureCardPreview() {
    FeatureCard(card = UpgradeFeatureCard.PLUS)
}

@Preview
@Composable
private fun OnboardingPatonFeatureCardPreview() {
    FeatureCard(card = UpgradeFeatureCard.PATRON)
}
