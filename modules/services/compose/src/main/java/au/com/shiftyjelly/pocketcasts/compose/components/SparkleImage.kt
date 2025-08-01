package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun SparkleImage(
    modifier: Modifier = Modifier,
    gradientColors: Pair<Color, Color> = MaterialTheme.theme.type.sparkleColors,
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

@Preview
@Composable
private fun SparkleImagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        SparkleImage(
            modifier = Modifier.size(80.dp),
        )
    }
}
