package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.activity.SystemBarStyle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalPagerWrapper
import au.com.shiftyjelly.pocketcasts.compose.components.StyledToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.images.OfferBadge
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val MAX_OFFER_BADGE_TEXT_LENGTH = 23
private const val MIN_SCREEN_WIDTH_FOR_HORIZONTAL_DISPLAY = 400

@Composable
internal fun OnboardingUpgradeFeaturesPage(
    flow: OnboardingFlow,
    source: OnboardingUpgradeSource,
    onBackPressed: () -> Unit,
    onClickSubscribe: () -> Unit,
    onNotNowPressed: () -> Unit,
    canUpgrade: Boolean,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
) {
    val viewModel = hiltViewModel<OnboardingUpgradeFeaturesViewModel>()
    val state by viewModel.state.collectAsState()

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
    SetStatusBarBackground(scrollState, onUpdateSystemBars)

    when (state) {
        is OnboardingUpgradeFeaturesState.Loading -> Unit // Do Nothing
        is OnboardingUpgradeFeaturesState.Loaded -> {
            val loadedState = state as OnboardingUpgradeFeaturesState.Loaded
            if (FeatureFlag.isEnabled(Feature.PAYWALL_EXPERIMENT)) {
                UpgradePlusLayoutExperiment(
                    state = loadedState,
                    source = source,
                    scrollState = scrollState,
                    onBackPressed = onBackPressed,
                    onNotNowPressed = onNotNowPressed,
                    onSubscriptionFrequencyChanged = { viewModel.onSubscriptionFrequencyChanged(it) },
                    onFeatureCardChanged = { viewModel.onFeatureCardChanged(loadedState.featureCardsState.featureCards[it]) },
                    onClickSubscribe = onClickSubscribe,
                    canUpgrade = canUpgrade,
                )
            } else {
                UpgradeLayout(
                    state = loadedState,
                    source = source,
                    scrollState = scrollState,
                    onBackPressed = onBackPressed,
                    onNotNowPressed = onNotNowPressed,
                    onSubscriptionFrequencyChanged = { viewModel.onSubscriptionFrequencyChanged(it) },
                    onFeatureCardChanged = { viewModel.onFeatureCardChanged(loadedState.featureCardsState.featureCards[it]) },
                    onClickSubscribe = onClickSubscribe,
                    canUpgrade = canUpgrade,
                )
            }
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
    source: OnboardingUpgradeSource,
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
                .background(OnboardingUpgradeHelper.backgroundColor),
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
                                .width(48.dp),
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
                            contentAlignment = Alignment.Center,
                        ) {
                            AutoResizeText(
                                text = stringResource(state.currentFeatureCard.titleRes(source)),
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
                            contentAlignment = Alignment.Center,
                        ) {
                            StyledToggle(
                                items = state.subscriptionFrequencies
                                    .map { stringResource(id = it.localisedLabelRes) },
                                defaultSelectedItemIndex = state.subscriptionFrequencies.indexOf(
                                    state.currentSubscriptionFrequency,
                                ),
                            ) {
                                val selectedFrequency = state.subscriptionFrequencies[it]
                                onSubscriptionFrequencyChanged(selectedFrequency)
                            }
                        }

                        FeatureCards(
                            state = state,
                            upgradeButton = state.currentUpgradeButton,
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
    upgradeButton: UpgradeButton,
    onFeatureCardChanged: (Int) -> Unit,
) {
    val featureCardsState = state.featureCardsState
    val currentSubscriptionFrequency = state.currentSubscriptionFrequency
    HorizontalPagerWrapper(
        pageCount = featureCardsState.featureCards.size,
        initialPage = featureCardsState.featureCards.indexOf(state.currentFeatureCard),
        onPageChanged = onFeatureCardChanged,
        showPageIndicator = featureCardsState.showPageIndicator,
        pageSize = PageSize.Fixed(LocalConfiguration.current.screenWidthDp.dp - 64.dp),
        contentPadding = PaddingValues(horizontal = 32.dp),
    ) { index, pagerHeight ->
        FeatureCard(
            subscription = state.currentSubscription,
            card = featureCardsState.featureCards[index],
            subscriptionFrequency = currentSubscriptionFrequency,
            upgradeButton = upgradeButton,
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
    upgradeButton: UpgradeButton,
    subscription: Subscription,
    subscriptionFrequency: SubscriptionFrequency,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp,
        backgroundColor = Color.White,
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            var offerBadgeTextLength by remember { mutableStateOf(MAX_OFFER_BADGE_TEXT_LENGTH) }
            val screenWidth = LocalConfiguration.current.screenWidthDp
            val displayInHorizontal = screenWidth >= MIN_SCREEN_WIDTH_FOR_HORIZONTAL_DISPLAY && offerBadgeTextLength <= MAX_OFFER_BADGE_TEXT_LENGTH

            if (displayInHorizontal) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    SubscriptionBadge(
                        iconRes = card.iconRes,
                        shortNameRes = card.shortNameRes,
                        backgroundColor = Color.Black,
                        textColor = Color.White,
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight(),
                    )

                    if (subscription is Subscription.WithOffer) {
                        val offerText = subscription.badgeOfferText(LocalContext.current.resources)
                        offerBadgeTextLength = offerText.length
                        OfferBadge(
                            text = offerText,
                            backgroundColor = upgradeButton.backgroundColorRes,
                            textColor = upgradeButton.textColorRes,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    SubscriptionBadge(
                        iconRes = card.iconRes,
                        shortNameRes = card.shortNameRes,
                        backgroundColor = Color.Black,
                        textColor = Color.White,
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight(),
                    )

                    if (subscription is Subscription.WithOffer) {
                        val offerText = subscription.badgeOfferText(LocalContext.current.resources)
                        offerBadgeTextLength = offerText.length
                        OfferBadge(
                            text = offerText,
                            backgroundColor = upgradeButton.backgroundColorRes,
                            textColor = upgradeButton.textColorRes,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Column {
                SubscriptionProductAmountHorizontal(subscription, hasBackgroundAlwaysWhite = true)

                Spacer(modifier = Modifier.padding(vertical = 4.dp))

                card.featureItems(subscriptionFrequency).forEach {
                    UpgradeFeatureItem(it)
                }
                Spacer(modifier = Modifier.weight(1f))
                OnboardingUpgradeHelper.PrivacyPolicy(
                    color = Color.Black.copy(alpha = .5f),
                    textAlign = TextAlign.Start,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
internal fun UpgradeButton(
    button: UpgradeButton,
    onClickSubscribe: () -> Unit,
) {
    val resources = LocalContext.current.resources
    val shortName = resources.getString(button.shortNameRes)
    val primaryText = stringResource(LR.string.subscribe_to, shortName)

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fadeBackground(),
    ) {
        Column {
            UpgradeRowButton(
                primaryText = primaryText,
                backgroundColor = colorResource(button.backgroundColorRes),
                textColor = colorResource(button.textColorRes),
                fontWeight = FontWeight.W500,
                onClick = onClickSubscribe,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .heightIn(min = 48.dp),
            )
            Spacer(
                modifier = Modifier
                    .windowInsetsBottomHeight(WindowInsets.navigationBars),
            )
        }
    }
}

@Composable
private fun SetStatusBarBackground(
    scrollState: ScrollState,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
) {
    val hasScrolled = scrollState.value > 0

    val scrimAlpha: Float by animateFloatAsState(
        targetValue = if (hasScrolled) 0.6f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "scrimAlpha",
    )

    val statusBarBackground = if (scrimAlpha > 0) {
        OnboardingUpgradeHelper.backgroundColor.copy(alpha = scrimAlpha).toArgb()
    } else {
        Color.Transparent.toArgb()
    }

    LaunchedEffect(statusBarBackground) {
        val statusBar = SystemBarStyle.dark(statusBarBackground)
        val navigationBar = SystemBarStyle.dark(Color.Transparent.toArgb())
        onUpdateSystemBars(SystemBarsStyles(statusBar, navigationBar))
    }
}

@Composable
internal fun BoxWithConstraintsScope.calculateMinimumHeightWithInsets(): Dp {
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
fun NoSubscriptionsLayout(
    onBackPressed: () -> Unit,
    onNotNowPressed: () -> Unit,
    showNotNow: Boolean,
) {
    Column(
        Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxWidth(),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationIconButton(
                onNavigationClick = onBackPressed,
                iconColor = MaterialTheme.theme.colors.primaryText01,
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp),
            )
            if (showNotNow) {
                TextH30(
                    text = stringResource(LR.string.not_now),
                    color = MaterialTheme.theme.colors.primaryText01,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clickable { onNotNowPressed() },
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TextH30(
                text = stringResource(id = LR.string.onboarding_upgrade_no_subscriptions_found),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
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
                blendMode = BlendMode.DstIn,
            )
            drawContent()
        }
    }
