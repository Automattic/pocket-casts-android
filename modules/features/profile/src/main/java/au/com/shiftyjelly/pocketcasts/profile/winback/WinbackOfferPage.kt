package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun WinbackOfferPage(
    offer: WinbackOffer,
    onAcceptOffer: () -> Unit,
    onCancelSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            modifier = Modifier.height(52.dp),
        )
        BoxWithConstraints {
            HeartImage(
                gradientColors = MaterialTheme.theme.type.heartColors,
                modifier = Modifier.size((maxWidth * 0.4f).coerceAtMost(162.dp)),
            )
        }
        Spacer(
            modifier = Modifier.height(20.dp),
        )
        Text(
            text = when (offer.billingCycle) {
                BillingCycle.Monthly -> stringResource(LR.string.winback_offer_free_offer_title, offer.formattedPrice)
                BillingCycle.Yearly -> stringResource(LR.string.winback_offer_free_offer_yearly_title, offer.formattedPrice)
            },
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 33.5.sp,
            color = MaterialTheme.theme.colors.primaryText01,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP30(
            text = when (offer.billingCycle) {
                BillingCycle.Monthly -> stringResource(LR.string.winback_offer_free_offer_description, offer.formattedPrice)
                BillingCycle.Yearly -> stringResource(LR.string.winback_offer_free_offer_yearly_description, offer.formattedPrice)
            },
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            onClick = onAcceptOffer,
        ) {
            TextH30(
                text = stringResource(LR.string.winback_cancel_subscription_accept_offer_button_label),
                color = MaterialTheme.theme.colors.primaryInteractive02,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp),
        )

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
            border = BorderStroke(2.dp, MaterialTheme.theme.colors.support05),
            shape = RoundedCornerShape(16.dp),
            onClick = onCancelSubscription,
        ) {
            TextH30(
                text = stringResource(LR.string.winback_cancel_subscription_continue_cancel_button_label),
                color = MaterialTheme.theme.colors.support05,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(
            modifier = Modifier.height(16.dp),
        )
    }
}

@Composable
private fun HeartImage(
    gradientColors: Pair<Color, Color>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        HeartIcon(
            painter = painterResource(IR.drawable.ic_winback_offer_heart_1),
            width = maxWidth * 0.728f,
            height = maxHeight * 0.654f,
            offsetX = maxWidth * 0.077f,
            offsetY = maxHeight * 0.225f,
            gradientColors = gradientColors,
            alpha = 1f,
            rotation = 0f,
        )
        HeartIcon(
            painter = painterResource(IR.drawable.ic_winback_offer_heart_2),
            width = maxWidth * 0.75f,
            height = maxHeight * 0.7f,
            offsetX = maxWidth * 0.23f,
            offsetY = maxHeight * 0.14f,
            gradientColors = gradientColors,
            alpha = 0.2f,
            rotation = 7.84f,
        )
    }
}

@Composable
private fun HeartIcon(
    painter: Painter,
    width: Dp,
    height: Dp,
    offsetX: Dp,
    offsetY: Dp,
    gradientColors: Pair<Color, Color>,
    alpha: Float,
    rotation: Float,
) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                this.alpha = alpha
                this.rotationZ = rotation
            }
            .padding(start = offsetX, top = offsetY)
            .size(width, height)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientColors.first, gradientColors.second),
                    ),
                    blendMode = BlendMode.SrcAtop,
                )
            },
    )
}

private val ThemeType.heartColors
    get() = when (this) {
        ThemeType.LIGHT, ThemeType.DARK, ThemeType.EXTRA_DARK, ThemeType.ELECTRIC -> blueHeart
        ThemeType.CLASSIC_LIGHT, ThemeType.ROSE -> redHeart
        ThemeType.INDIGO -> indigoHeart
        ThemeType.DARK_CONTRAST -> grayHeart
        ThemeType.LIGHT_CONTRAST -> blackHeart
        ThemeType.RADIOACTIVE -> greenHeart
    }

private val blackHeart = Color.Black to Color(0xFF6B7273)
private val blueHeart = Color(0xFF03A9F4) to Color(0xFF50D0F1)
private val redHeart = Color(0xFFF43769) to Color(0xFFFB5246)
private val indigoHeart = Color(0xFF5C8BCC) to Color(0xFF95B0E6)
private val greenHeart = Color(0xFF78D549) to Color(0xFF9BE45E)
private val grayHeart = Color(0xFFCCD6D9) to Color(0xFFE5F7FF)

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun WinbackOfferPageOfferPreview(
    @PreviewParameter(WinbackOfferParameterProvider::class) offer: WinbackOffer,
) {
    AppThemeWithBackground(
        themeType = ThemeType.LIGHT,
    ) {
        WinbackOfferPage(
            onAcceptOffer = {},
            onCancelSubscription = {},
            offer = offer,
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun WinbackOfferPageThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(
        themeType = theme,
    ) {
        WinbackOfferPage(
            onAcceptOffer = {},
            onCancelSubscription = {},
            offer = WinbackOffer(
                redeemCode = "",
                formattedPrice = "\$3.99",
                tier = SubscriptionTier.Plus,
                billingCycle = BillingCycle.Monthly,
            ),
        )
    }
}

private class WinbackOfferParameterProvider : PreviewParameterProvider<WinbackOffer> {
    override val values = sequenceOf(
        WinbackOffer(
            redeemCode = "",
            formattedPrice = "\$3.99",
            tier = SubscriptionTier.Plus,
            billingCycle = BillingCycle.Monthly,
        ),
        WinbackOffer(
            redeemCode = "",
            formattedPrice = "\$19.99",
            tier = SubscriptionTier.Plus,
            billingCycle = BillingCycle.Yearly,
        ),
    )
}
