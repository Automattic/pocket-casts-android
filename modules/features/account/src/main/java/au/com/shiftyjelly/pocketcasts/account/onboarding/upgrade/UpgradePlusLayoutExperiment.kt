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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.StyledToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource

@Composable
internal fun UpgradePlusLayoutExperiment(
    state: OnboardingUpgradeFeaturesState.Loaded,
    source: OnboardingUpgradeSource,
    scrollState: ScrollState,
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
