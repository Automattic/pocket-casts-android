package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.theme

@Composable
fun GradientIcon(icon: GradientIconData?) {

    if (icon != null) {

        val atLeastTwoColors = when (icon.colors.size) {
            0 -> {
                val defaultColor = MaterialTheme.theme.colors.primaryInteractive01
                listOf(defaultColor, defaultColor)
            }
            1 -> {
                val onlyColor = icon.colors.first()
                listOf(onlyColor, onlyColor)
            }
            else -> icon.colors
        }

        Image(
            painter = painterResource(icon.res),
            contentDescription = null,
            modifier = Modifier
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
}

class GradientIconData(
    @DrawableRes val res: Int,
    val colors: List<Color> = emptyList()
)
