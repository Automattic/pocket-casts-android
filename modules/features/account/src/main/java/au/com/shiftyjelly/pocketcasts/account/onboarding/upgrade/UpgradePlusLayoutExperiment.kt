package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeDisplayMode
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeForTier
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun UpgradePlusLayoutExperiment(
    state: OnboardingUpgradeFeaturesState.Loaded,
    source: OnboardingUpgradeSource,
    scrollState: ScrollState,
    onNotNowPressed: () -> Unit,
    onFeatureCardChanged: (Int) -> Unit,
    onClickSubscribe: () -> Unit,
    canUpgrade: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxHeight()
                .background(Color.Black),
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
                        TextP30(
                            text = stringResource(R.string.not_now),
                            color = Color.White,
                            modifier = Modifier
                                .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                                .clickable { onNotNowPressed() },
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
                            modifier = Modifier.padding(bottom = 20.dp),
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

                        Spacer(Modifier.height(20.dp))

                        FeatureCards( // TODO: it will be replaced for the new one
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
            SubscribeButton(onClickSubscribe)
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
        Column {
            Button(
                onClick = onClickSubscribe,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(UR.color.plus_gold),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AutoResizeText(
                        text = stringResource(LR.string.get_pocket_casts_plus),
                        color = Color.Black,
                        maxFontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.W600,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .windowInsetsBottomHeight(WindowInsets.navigationBars),
            )
        }
    }
}
