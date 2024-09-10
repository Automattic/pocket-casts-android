package au.com.shiftyjelly.pocketcasts.referrals

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
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
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val plusBackgroundBrush = Brush.horizontalGradient(
    0f to Color(0xFFFED745),
    1f to Color(0xFFFEB525),
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ReferralsSendGuestPassPage(
    passCount: Int,
    onDismiss: () -> Unit,
) {
    AppTheme(Theme.ThemeType.DARK) {
        val context = LocalContext.current
        val windowSize = calculateWindowSizeClass(context.getActivity() as Activity)

        ReferralsSendGuestPassContent(
            passCount = passCount,
            windowWidthSizeClass = windowSize.widthSizeClass,
            windowHeightSizeClass = windowSize.heightSizeClass,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ReferralsSendGuestPassContent(
    passCount: Int,
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    onDismiss: () -> Unit,
) {
    BoxWithConstraints {
        val cardCornerRadius = if (windowWidthSizeClass == WindowWidthSizeClass.Compact ||
            windowHeightSizeClass == WindowHeightSizeClass.Compact
        ) {
            0.dp
        } else {
            8.dp
        }

        val cardModifier = if (windowWidthSizeClass == WindowWidthSizeClass.Compact ||
            windowHeightSizeClass == WindowHeightSizeClass.Compact
        ) {
            Modifier
                .fillMaxSize()
        } else {
            Modifier
                .width((maxWidth.value * 0.6).dp)
                .wrapContentSize()
        }

        Box(
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
            Card(
                elevation = 8.dp,
                shape = RoundedCornerShape(cardCornerRadius),
                backgroundColor = Color.Black,
                modifier = cardModifier,
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

                    Spacer(modifier = Modifier.height(16.dp))

                    TextH10(
                        text = stringResource(LR.string.referrals_send_guest_pass_title),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextH50(
                        text = pluralStringResource(
                            LR.plurals.referrals_remaining_passes,
                            passCount,
                            passCount,
                        ),
                    )

                    if (windowHeightSizeClass != WindowHeightSizeClass.Compact) {
                        Spacer(modifier = Modifier.height(16.dp))

                        ReferralsPassCardsStack(
                            passCount = passCount,
                        )
                    }

                    if (windowWidthSizeClass == WindowWidthSizeClass.Compact
                        || windowHeightSizeClass == WindowHeightSizeClass.Compact) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    GradientRowButton(
                        primaryText = stringResource(LR.string.referrals_share_guest_pass),
                        textColor = Color.Black,
                        gradientBackgroundColor = plusBackgroundBrush,
                        modifier = Modifier.padding(16.dp),
                        onClick = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferralsPassCardsStack(
    passCount: Int,
) {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        (0..<passCount).forEach { index ->
            val cardWidth = (ReferralGuestPassCardView.cardSize.width.value * (1 + index * 0.125)).dp
            val cardHeight = (ReferralGuestPassCardView.cardSize.height.value * (1 + index * 0.125)).dp
            ReferralGuestPassCardView(
                modifier = Modifier
                    .padding(16.dp)
                    .size(
                        cardWidth,
                        cardHeight,
                    )
                    .offset(y = (20 * index).dp),
            )
        }
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
fun ReferralsSendGuestPassContentPreview() {
    ReferralsSendGuestPassContent(
        windowWidthSizeClass = WindowWidthSizeClass.Compact,
        windowHeightSizeClass = WindowHeightSizeClass.Medium,
        passCount = 3,
        onDismiss = {},
    )
}
