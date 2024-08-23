package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.paywallfeatures

import android.content.res.Configuration
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.calculateMinimumHeightWithInsets
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private val SCREEN_HEIGHT_FOR_SMALL_DEVICES = 600.dp

@Composable
internal fun UpgradeLayoutFeatures(
    state: OnboardingUpgradeFeaturesState.Loaded,
    source: OnboardingUpgradeSource,
    scrollState: ScrollState,
    onNotNowPressed: () -> Unit,
    onClickSubscribe: () -> Unit,
    canUpgrade: Boolean,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeight = configuration.screenHeightDp.dp
    val isSmallDevice = screenHeight < SCREEN_HEIGHT_FOR_SMALL_DEVICES

    val offerText = when (state.currentSubscription) {
        is Subscription.Trial -> stringResource(LR.string.paywall_free_1_month_trial)
        is Subscription.Intro -> stringResource(LR.string.paywall_save_50_off)
        else -> null
    }

    val shouldShowOffer = offerText != null && !isLandscape

    AppTheme(Theme.ThemeType.DARK) {
        Box(
            modifier = modifier.fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxHeight()
                    .background(color = MaterialTheme.theme.colors.primaryUi02),
            ) {
                Box(modifier = Modifier.verticalScroll(scrollState)) {
                    Column(
                        Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .heightIn(min = this@BoxWithConstraints.calculateMinimumHeightWithInsets())
                            .padding(bottom = 100.dp),
                    ) {
                        if (isSmallDevice) {
                            SmallDeviceContent(onNotNowPressed, state, source, modifier)
                        } else {
                            Content(onNotNowPressed, state, source, modifier)
                        }
                    }
                }
            }

            if (canUpgrade) {
                Column(
                    modifier = Modifier,
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val bottomPadding = if (shouldShowOffer) 0.dp else if (isSmallDevice) 8.dp else 34.dp

                    SubscribeButton(onClickSubscribe, Modifier.padding(bottom = bottomPadding))

                    if (shouldShowOffer) {
                        offerText?.let {
                            TextP50(
                                text = it,
                                fontWeight = FontWeight.W400,
                            )
                            Spacer(Modifier.height(if (isSmallDevice) 12.dp else 50.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Content(
    onNotNowPressed: () -> Unit,
    state: OnboardingUpgradeFeaturesState.Loaded,
    source: OnboardingUpgradeSource,
    modifier: Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        RowTextButton(
            text = stringResource(LR.string.not_now),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White,
            ),
            fontSize = 18.sp,
            onClick = onNotNowPressed,
            fullWidth = false,
            includePadding = false,
        )
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, top = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            SubscriptionBadge(
                fontSize = 16.sp,
                padding = 8.dp,
                iconRes = R.drawable.ic_plus,
                shortNameRes = LR.string.pocket_casts_plus_short,
                iconColor = Color.Black,
                backgroundBrush = plusGradientBrush,
                textColor = Color.Black,
            )
        }

        Box(
            modifier = Modifier.padding(bottom = 40.dp),
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

        FeaturedPaywallCards(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp, bottom = 40.dp),
        )
    }
}

@Composable
private fun SmallDeviceContent(
    onNotNowPressed: () -> Unit,
    state: OnboardingUpgradeFeaturesState.Loaded,
    source: OnboardingUpgradeSource,
    modifier: Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        RowTextButton(
            text = stringResource(LR.string.not_now),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White,
            ),
            fontSize = 18.sp,
            onClick = onNotNowPressed,
            fullWidth = false,
            includePadding = false,
        )
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, top = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            SubscriptionBadge(
                fontSize = 16.sp,
                padding = 4.dp,
                iconRes = R.drawable.ic_plus,
                shortNameRes = LR.string.pocket_casts_plus_short,
                iconColor = Color.Black,
                backgroundBrush = plusGradientBrush,
                textColor = Color.Black,
            )
        }

        Box(
            modifier = Modifier.padding(bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            AutoResizeText(
                text = stringResource(state.currentFeatureCard.titleRes(source)),
                color = Color.White,
                maxFontSize = 22.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.W700,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            )
        }

        FeaturedPaywallCards(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
        )
    }
}

@Composable
fun SubscribeButton(
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier,
    ) {
        RowButton(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(LR.string.get_pocket_casts_plus),
            onClick = onClickSubscribe,
            fontWeight = FontWeight.W600,
            fontSize = 18.sp,
            textColor = Color.Black,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colorResource(UR.color.plus_gold),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SubscribeButtonPreview() {
    SubscribeButton(onClickSubscribe = { }, Modifier)
}
