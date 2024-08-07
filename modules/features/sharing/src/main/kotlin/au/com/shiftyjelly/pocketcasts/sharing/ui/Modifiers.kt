package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

internal fun Modifier.scrollBottomFade(
    scrollState: ScrollState,
) = then(
    Modifier
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val viewPortHeight = scrollState.viewportSize.toFloat()
            val scrollPercentage = when (val maxValue = scrollState.maxValue) {
                0, Int.MAX_VALUE -> 1f
                else -> (scrollState.value.toFloat() / maxValue)
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.02f to Color.Transparent.copy(alpha = scrollPercentage * scrollPercentage),
                        0.25f to Color.Black,
                    ),
                    startY = viewPortHeight,
                    endY = 0f,
                ),
                blendMode = BlendMode.DstIn,
            )
            drawRect(Color.Transparent, size = Size(size.width, viewPortHeight / 4))
        },
)
