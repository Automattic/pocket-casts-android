package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeDisplayMode
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeForTier
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun UpgradeLayoutFeatures(
    state: OnboardingUpgradeFeaturesState.Loaded,
    source: OnboardingUpgradeSource,
    scrollState: ScrollState,
    onNotNowPressed: () -> Unit,
    onFeatureCardChanged: (Int) -> Unit,
    onClickSubscribe: () -> Unit,
    canUpgrade: Boolean,
    modifier: Modifier = Modifier,
) {
    AppTheme(Theme.ThemeType.DARK) { // We need to set Dark since this screen will have dark colors for all themes
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
                                SubscriptionBadgeForTier(
                                    tier = SubscriptionTier.PLUS,
                                    displayMode = SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
                                    fontSize = 16.sp,
                                    padding = 8.dp,
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

                            FeatureCards(
                                // TODO: it will be replaced for the new one
                                state = state,
                                upgradeButton = state.currentUpgradeButton,
                                onFeatureCardChanged = onFeatureCardChanged,
                            )
                        }
                    }
                }
            }

            if (canUpgrade) {
                SubscribeButton(onClickSubscribe)
            }
        }
    }
}

@Composable
private fun SubscribeButton(
    onClickSubscribe: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
    ) {
        RowButton(
            modifier = Modifier.padding(bottom = 80.dp),
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
    SubscribeButton(onClickSubscribe = { })
}
