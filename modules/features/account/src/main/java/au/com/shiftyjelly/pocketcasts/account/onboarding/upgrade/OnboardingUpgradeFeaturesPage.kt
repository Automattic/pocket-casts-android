package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.IconRow
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PlusOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PlusRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import java.lang.Long.max
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingUpgradeFeaturesPage(
    flow: OnboardingFlow,
    source: OnboardingUpgradeSource,
    onUpgradePressed: () -> Unit,
    onNotNowPressed: () -> Unit,
    onBackPressed: () -> Unit,
    canUpgrade: Boolean
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

    // Need this BoxWithConstraints so we can force the inner column to fill the screen with vertical scroll enabled
    BoxWithConstraints(
        Modifier
            .fillMaxHeight()
            .background(OnboardingUpgradeHelper.backgroundColor)
    ) {

        OnboardingUpgradeHelper.PlusBackground(Modifier.verticalScroll(scrollState)) {
            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .heightIn(min = this@BoxWithConstraints.calculateMinimumHeightWithInsets())
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

    val scrimAlpha by animateFloatAsState(
        targetValue = if (hasScrolled) 0.6f else 0f,
        animationSpec = tween(durationMillis = 400)
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
        PlusUpgradeFeatureItem.values().forEach {
            FeatureItem(it)
        }
    }
}

@Composable
private fun FeatureItem(
    content: PlusUpgradeFeatureItem,
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

enum class PlusUpgradeFeatureItem(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val text: Int,
) {
    DesktopApps(
        image = IR.drawable.desktop_apps,
        title = LR.string.onboarding_plus_feature_desktop_apps_title,
        text = LR.string.onboarding_plus_feature_desktop_apps_text,
    ),
    Folders(
        image = IR.drawable.folder,
        title = LR.string.onboarding_plus_feature_folders_title,
        text = LR.string.onboarding_plus_feature_folders_text,
    ),
    CloudStorage(
        image = IR.drawable.cloud_storage,
        title = LR.string.onboarding_plus_feature_cloud_storage_title,
        text = LR.string.onboarding_plus_feature_cloud_storage_text,
    ),
    ThemesIcons(
        image = IR.drawable.themes_icons,
        title = LR.string.onboarding_plus_feature_themes_icons_title,
        text = LR.string.onboarding_plus_feature_themes_icons_text,
    ),
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

@Preview
@Composable
private fun OnboardingUpgradeFeaturesPreview() {
    OnboardingUpgradeFeaturesPage(
        flow = OnboardingFlow.InitialOnboarding,
        source = OnboardingUpgradeSource.RECOMMENDATIONS,
        onBackPressed = {},
        onUpgradePressed = {},
        onNotNowPressed = {},
        canUpgrade = true,
    )
}
