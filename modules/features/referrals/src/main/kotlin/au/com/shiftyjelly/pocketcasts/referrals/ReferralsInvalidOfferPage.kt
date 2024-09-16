package au.com.shiftyjelly.pocketcasts.referrals

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.GradientRowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.extensions.plusBackgroundBrush
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ReferralsInvalidOfferPage(
    onDismiss: () -> Unit,
) {
    AppTheme(Theme.ThemeType.DARK) {
        val context = LocalContext.current
        val windowSize = calculateWindowSizeClass(context.getActivity() as Activity)

        ReferralsInvalidOfferPageContent(
            windowWidthSizeClass = windowSize.widthSizeClass,
            windowHeightSizeClass = windowSize.heightSizeClass,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ReferralsInvalidOfferPageContent(
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    onDismiss: () -> Unit,
) {
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
        val showFullScreen = windowWidthSizeClass == WindowWidthSizeClass.Compact ||
            windowHeightSizeClass == WindowHeightSizeClass.Compact
        val cardCornerRadius = if (showFullScreen) 0.dp else 8.dp
        val cardWidth = if (showFullScreen) maxWidth else (maxWidth.value * .5).dp
        val cardModifier = if (showFullScreen) {
            Modifier
                .fillMaxSize()
        } else {
            Modifier
                .width(cardWidth)
                .wrapContentSize()
        }

        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(cardCornerRadius),
            backgroundColor = Color.Black,
            modifier = cardModifier
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
                    .padding(16.dp),
            ) {
                CloseButton(
                    modifier = Modifier
                        .align(Alignment.End),
                    onClick = onDismiss,
                )

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
                    modifier = Modifier.width(cardWidth * 0.8f),
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

@Preview(device = Devices.PortraitRegular)
@Composable
fun ReferralsInvalidOfferPagePortraitPhonePreview() {
    ReferralsInvalidOfferPageContentPreview(
        windowWidthSizeClass = WindowWidthSizeClass.Compact,
        windowHeightSizeClass = WindowHeightSizeClass.Medium,
    )
}

@Preview(device = Devices.LandscapeRegular)
@Composable
fun ReferralsInvalidOfferPageLandscapePhonePreview() {
    ReferralsInvalidOfferPageContentPreview(
        windowWidthSizeClass = WindowWidthSizeClass.Compact,
        windowHeightSizeClass = WindowHeightSizeClass.Compact,
    )
}

@Preview(device = Devices.PortraitTablet)
@Composable
fun ReferralsInvalidOfferPagePortraitTabletPreview() {
    ReferralsInvalidOfferPageContentPreview(
        windowWidthSizeClass = WindowWidthSizeClass.Medium,
        windowHeightSizeClass = WindowHeightSizeClass.Medium,
    )
}

@Preview(device = Devices.LandscapeTablet)
@Composable
fun ReferralsInvalidOfferPageLandscapeTabletPreview() {
    ReferralsInvalidOfferPageContentPreview(
        windowWidthSizeClass = WindowWidthSizeClass.Medium,
        windowHeightSizeClass = WindowHeightSizeClass.Expanded,
    )
}

@Composable
fun ReferralsInvalidOfferPageContentPreview(
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
) {
    AppTheme(Theme.ThemeType.DARK) {
        ReferralsInvalidOfferPageContent(
            windowWidthSizeClass = windowWidthSizeClass,
            windowHeightSizeClass = windowHeightSizeClass,
            onDismiss = {},
        )
    }
}
