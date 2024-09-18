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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.GradientRowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.extensions.plusBackgroundBrush
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.referrals.ReferralPageDefaults.pageCornerRadius
import au.com.shiftyjelly.pocketcasts.referrals.ReferralPageDefaults.pageWidthPercent
import au.com.shiftyjelly.pocketcasts.referrals.ReferralPageDefaults.shouldShowFullScreen
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ReferralsSendGuestPassPage(
    sharingClient: SharingClient,
    onDismiss: () -> Unit,
) {
    AppTheme(Theme.ThemeType.DARK) {
        val context = LocalContext.current
        val windowSize = calculateWindowSizeClass(context.getActivity() as Activity)
        val scope = rememberCoroutineScope()

        ReferralsSendGuestPassContent(
            windowWidthSizeClass = windowSize.widthSizeClass,
            windowHeightSizeClass = windowSize.heightSizeClass,
            onDismiss = onDismiss,
            onShare = {
                val referralCode = "test_code" // TODO - Referrals: Make it dynamic
                val request = SharingRequest.referralLink(
                    referralCode = referralCode,
                ).build()
                scope.launch {
                    sharingClient.share(
                        request = request,
                        event = AnalyticsEvent.REFERRAL_LINK_SHARED,
                        eventProperties = mapOf("code" to referralCode),
                    )
                }
            },
        )
    }
}

@Composable
private fun ReferralsSendGuestPassContent(
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
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
        val showFullScreen = shouldShowFullScreen(windowWidthSizeClass, windowHeightSizeClass)
        val pageWidth = if (showFullScreen) maxWidth else (maxWidth.value * pageWidthPercent).dp
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
                    text = stringResource(LR.string.referrals_send_guest_pass_title),
                    textAlign = TextAlign.Center,
                )

                if (windowHeightSizeClass != WindowHeightSizeClass.Compact) {
                    Spacer(modifier = Modifier.height(24.dp))

                    ReferralsPassCardsStack(
                        width = pageWidth,
                    )
                }

                if (showFullScreen) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                GradientRowButton(
                    primaryText = stringResource(LR.string.referrals_share_guest_pass),
                    textColor = Color.Black,
                    gradientBackgroundColor = plusBackgroundBrush,
                    modifier = Modifier.padding(16.dp),
                    onClick = onShare,
                )
            }
        }
    }
}

@Composable
private fun ReferralsPassCardsStack(
    cardsCount: Int = 3,
    width: Dp,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .width(width),
    ) {
        (0..<cardsCount).reversed().forEach { index ->
            val cardWidth = (maxWidth.value * 0.8 * (1 - index * 0.125)).dp
            val cardHeight = (cardWidth.value * ReferralGuestPassCardDefaults.cardAspectRatio).dp
            val cardOffset = (10 * ((cardsCount - 1) - index)).dp
            ReferralGuestPassCardView(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(cardWidth, cardHeight)
                    .offset(y = cardOffset),
                source = ReferralGuestPassCardViewSource.Send,
            )
        }
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
fun ReferralsSendGuestPassPortraitPhonePreview() {
    ReferralsSendGuestPassContentPreview(
        windowWidthSizeClass = WindowWidthSizeClass.Compact,
        windowHeightSizeClass = WindowHeightSizeClass.Medium,
    )
}

@Preview(device = Devices.LandscapeRegular)
@Composable
fun ReferralsSendGuestPassLandscapePhonePreview() {
    ReferralsSendGuestPassContentPreview(
        windowWidthSizeClass = WindowWidthSizeClass.Compact,
        windowHeightSizeClass = WindowHeightSizeClass.Compact,
    )
}

@Preview(device = Devices.PortraitTablet)
@Composable
fun ReferralsSendGuestPassPortraitTabletPreview() {
    ReferralsSendGuestPassContentPreview(
        windowWidthSizeClass = WindowWidthSizeClass.Medium,
        windowHeightSizeClass = WindowHeightSizeClass.Medium,
    )
}

@Preview(device = Devices.LandscapeTablet)
@Composable
fun ReferralsSendGuestPassLandscapeTabletPreview() {
    ReferralsSendGuestPassContentPreview(
        windowWidthSizeClass = WindowWidthSizeClass.Medium,
        windowHeightSizeClass = WindowHeightSizeClass.Expanded,
    )
}

@Composable
fun ReferralsSendGuestPassContentPreview(
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
) {
    AppTheme(Theme.ThemeType.DARK) {
        ReferralsSendGuestPassContent(
            windowWidthSizeClass = windowWidthSizeClass,
            windowHeightSizeClass = windowHeightSizeClass,
            onDismiss = {},
            onShare = {},
        )
    }
}
