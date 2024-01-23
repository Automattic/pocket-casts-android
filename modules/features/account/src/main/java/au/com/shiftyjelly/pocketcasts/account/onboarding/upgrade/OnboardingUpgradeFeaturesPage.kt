package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalPagerWrapper
import au.com.shiftyjelly.pocketcasts.compose.components.StyledToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingUpgradeFeaturesPage(
    flow: OnboardingFlow,
    source: OnboardingUpgradeSource,
    onBackPressed: () -> Unit,
    onClickSubscribe: () -> Unit,
    onNotNowPressed: () -> Unit,
    canUpgrade: Boolean,
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
    SetStatusBarBackground(scrollState)

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
                onFeatureCardChanged = { viewModel.onFeatureCardChanged(loadedState.featureCardsState.featureCards[it]) },
                onClickSubscribe = onClickSubscribe,
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
    val featureCardsState = state.featureCardsState
    HorizontalPagerWrapper(
        pageCount = featureCardsState.featureCards.size,
        initialPage = featureCardsState.featureCards.indexOf(state.currentFeatureCard),
        onPageChanged = onFeatureCardChanged,
        showPageIndicator = featureCardsState.showPageIndicator,
        pageSize = PageSize.Fixed(LocalConfiguration.current.screenWidthDp.dp - 64.dp),
        contentPadding = PaddingValues(horizontal = 32.dp),
    ) { index, pagerHeight ->
        FeatureCard(
            card = featureCardsState.featureCards[index],
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                SubscriptionBadge(
                    iconRes = card.iconRes,
                    shortNameRes = card.shortNameRes,
                    backgroundColor = Color.Black,
                    textColor = Color.White,
                )
            }

            Column {
                card.featureItems.forEach {
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
private fun UpgradeButton(
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
private fun SetStatusBarBackground(scrollState: ScrollState) {
    val systemUiController = rememberSystemUiController()
    val hasScrolled = scrollState.value > 0

    val scrimAlpha: Float by animateFloatAsState(
        targetValue = if (hasScrolled) 0.6f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "scrimAlpha",
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
