package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.GradientRowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.extensions.plusBackgroundBrush
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.referrals.ReferralPageDefaults.PAGE_WIDTH_PERCENT
import au.com.shiftyjelly.pocketcasts.referrals.ReferralPageDefaults.pageCornerRadius
import au.com.shiftyjelly.pocketcasts.referrals.ReferralPageDefaults.shouldShowFullScreen
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ReferralsInvalidOfferPage(
    onDismiss: () -> Unit,
) {
    AppTheme(Theme.ThemeType.DARK) {
        ReferralsInvalidOfferPageContent(
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ReferralsInvalidOfferPageContent(
    onDismiss: () -> Unit,
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color.Transparent)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            )
            .fillMaxSize(),
    ) {
        val showFullScreen = shouldShowFullScreen(windowSize)
        val pageWidth = if (showFullScreen) maxWidth else (maxWidth.value * PAGE_WIDTH_PERCENT).dp
        val pageModifier = if (showFullScreen) {
            Modifier
                .fillMaxSize()
        } else {
            Modifier
                .width(pageWidth)
                .wrapContentSize()
        }

        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(pageCornerRadius(showFullScreen)),
            backgroundColor = Color.Black,
            modifier = pageModifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                SubscriptionBadge(
                    fontSize = 16.sp,
                    padding = 4.dp,
                    iconRes = IR.drawable.ic_plus,
                    shortNameRes = LR.string.pocket_casts_plus_short,
                    iconColor = Color.Black,
                    backgroundBrush = plusBackgroundBrush,
                    textColor = Color.Black,
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextH10(
                    text = stringResource(LR.string.referrals_invalid_offer_title),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextH50(
                    text = stringResource(LR.string.referrals_invalid_offer_description),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(pageWidth * 0.8f),
                )

                if (showFullScreen) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                GradientRowButton(
                    primaryText = stringResource(LR.string.got_it),
                    textColor = Color.Black,
                    gradientBackgroundColor = plusBackgroundBrush,
                    modifier = Modifier.padding(16.dp),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun ReferralsInvalidOfferPagePortraitPhonePreview() {
    ReferralsInvalidOfferPageContentPreview()
}

@Preview(device = Devices.LANDSCAPE_REGULAR)
@Composable
private fun ReferralsInvalidOfferPageLandscapePhonePreview() {
    ReferralsInvalidOfferPageContentPreview()
}

@Preview(device = Devices.PORTRAIT_TABLET)
@Composable
private fun ReferralsInvalidOfferPagePortraitTabletPreview() {
    ReferralsInvalidOfferPageContentPreview()
}

@Preview(device = Devices.LANDSCAPE_TABLET)
@Composable
private fun ReferralsInvalidOfferPageLandscapeTabletPreview() {
    ReferralsInvalidOfferPageContentPreview()
}

@Composable
fun ReferralsInvalidOfferPageContentPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        ReferralsInvalidOfferPageContent(
            onDismiss = {},
        )
    }
}
