package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable

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
                        blendMode = BlendMode.SrcAtop
                    )
                }
            }
    )
}

@ShowkaseComposable(name = "GradientIcon", group = "Images", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun GradientIconLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        GradientIconPreview()
    }
}

@ShowkaseComposable(name = "GradientIcon", group = "Images", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun GradientIconDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        GradientIconPreview()
    }
}

@Composable
private fun GradientIconPreview() {
    GradientIcon(
        painter = painterResource(R.drawable.ic_podcasts),
        colors = listOf(Color.Red, Color.Yellow)
    )
}
