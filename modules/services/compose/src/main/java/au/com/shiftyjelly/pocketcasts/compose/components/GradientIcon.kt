package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun GradientIcon(
    painter: Painter,
    modifier: Modifier = Modifier,
    colors: List<Color> = emptyList(),
) {
    val atLeastTwoColors = when (colors.size) {
        0 -> {
            val defaultColor = MaterialTheme.theme.colors.primaryInteractive01
            listOf(defaultColor, defaultColor)
        }
        1 -> {
            val onlyColor = colors.first()
            listOf(onlyColor, onlyColor)
        }
        else -> colors
    }

    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
            .size(24.dp)
            .graphicsLayer(alpha = 0.99f)
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(atLeastTwoColors),
                        blendMode = BlendMode.SrcAtop,
                    )
                }
            },
    )
}

@Composable
fun GradientIcon(
    painter: Painter,
    gradientBrush: Brush,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    blendMode: BlendMode = BlendMode.SrcAtop,
) {
    Icon(
        modifier = modifier
            .graphicsLayer(alpha = 0.99f)
            .drawWithContent {
                drawContent()
                drawRect(gradientBrush, blendMode = blendMode)
            },
        painter = painter,
        contentDescription = contentDescription,
    )
}

@Preview
@Composable
private fun GradientIconWithBrushPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        GradientIcon(
            painter = painterResource(IR.drawable.ic_plus),
            contentDescription = "",
            gradientBrush = Brush.plusGradientBrush,
        )
    }
}

@Preview(name = "Light")
@Composable
private fun GradientIconLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        GradientIconPreview()
    }
}

@Preview(name = "Dark")
@Composable
private fun GradientIconDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        GradientIconPreview()
    }
}

@Composable
private fun GradientIconPreview() {
    GradientIcon(
        painter = painterResource(R.drawable.ic_podcasts),
        colors = listOf(Color.Red, Color.Yellow),
    )
}
