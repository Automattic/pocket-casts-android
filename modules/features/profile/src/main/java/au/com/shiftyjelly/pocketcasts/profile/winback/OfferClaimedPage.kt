package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OfferClaimedPage(
    theme: ThemeType,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            modifier = Modifier.height(52.dp),
        )
        BoxWithConstraints {
            SparkleImage(
                gradientColors = theme.sparkleColors,
                modifier = Modifier.size((maxWidth * 0.4f).coerceAtMost(162.dp)),
            )
        }
        Spacer(
            modifier = Modifier.height(20.dp),
        )
        Text(
            text = stringResource(LR.string.winback_claimed_offer_message_1),
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 38.5.sp,
            color = MaterialTheme.theme.colors.primaryText01,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP30(
            text = stringResource(LR.string.winback_claimed_offer_message_2),
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        RowButton(
            text = stringResource(LR.string.done),
            onClick = onConfirm,
        )
        Spacer(
            modifier = Modifier.height(48.dp),
        )
    }
}

@Composable
private fun SparkleImage(
    gradientColors: Pair<Color, Color>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        SparkleIcon(
            painter = painterResource(IR.drawable.ic_sparkle_1),
            width = maxWidth * 0.425f,
            height = maxHeight * 0.457f,
            offsetX = maxWidth * 0.42f,
            offsetY = maxHeight * 0.1f,
            gradientColors = gradientColors,
            alpha = 1f,
        )
        SparkleIcon(
            painter = painterResource(IR.drawable.ic_sparkle_2),
            width = maxWidth * 0.242f,
            height = maxHeight * 0.259f,
            offsetX = maxWidth * 0.15f,
            offsetY = maxHeight * 0.35f,
            gradientColors = gradientColors,
            alpha = 0.8f,
        )
        SparkleIcon(
            painter = painterResource(IR.drawable.ic_sparkle_3),
            width = maxWidth * 0.29f,
            height = maxHeight * 0.32f,
            offsetX = maxWidth * 0.32f,
            offsetY = maxHeight * 0.57f,
            gradientColors = gradientColors,
            alpha = 0.6f,
        )
    }
}

@Composable
private fun SparkleIcon(
    painter: Painter,
    width: Dp,
    height: Dp,
    offsetX: Dp,
    offsetY: Dp,
    gradientColors: Pair<Color, Color>,
    alpha: Float,
) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                this.alpha = alpha
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

private val ThemeType.sparkleColors
    get() = when (this) {
        ThemeType.LIGHT, ThemeType.DARK, ThemeType.EXTRA_DARK, ThemeType.ELECTRIC -> blueSparkle
        ThemeType.CLASSIC_LIGHT, ThemeType.ROSE -> redSparkle
        ThemeType.INDIGO -> indigoSparkle
        ThemeType.DARK_CONTRAST -> graySparkle
        ThemeType.LIGHT_CONTRAST -> blackSparkle
        ThemeType.RADIOACTIVE -> greenSparkle
    }

private val blackSparkle = Color.Black to Color(0xFF6B7273)
private val blueSparkle = Color(0xFF03A9F4) to Color(0xFF50D0F1)
private val redSparkle = Color(0xFFF43769) to Color(0xFFFB5246)
private val indigoSparkle = Color(0xFF5C8BCC) to Color(0xFF95B0E6)
private val greenSparkle = Color(0xFF78D549) to Color(0xFF9BE45E)
private val graySparkle = Color(0xFFCCD6D9) to Color(0xFFE5F7FF)

@Preview(device = Devices.PortraitRegular)
@Composable
private fun WinbackOfferPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        OfferClaimedPage(
            theme = theme,
            onConfirm = {},
        )
    }
}
