package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.endofyear.utils.rainbowBrush

@Composable
fun GradientPillar(
    pillarStyle: PillarStyle,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = modifier
            .pillarGradient(pillarStyle)
            .padding(start = 24.dp),
    ) {
        content()
    }
}

private fun Modifier.pillarGradient(
    pillarStyle: PillarStyle
) = graphicsLayer { alpha = 0.99f }
    .drawWithCache {
        val size = size
        val brush = createBrush(pillarStyle, size)
        val overlayBrush = overlayBrush(size)

        onDrawWithContent {
            drawRect(color = Color.Black)
            drawRect(
                brush = brush,
                blendMode = BlendMode.SrcAtop
            )
            drawRect(
                brush = overlayBrush,
                blendMode = BlendMode.SrcAtop
            )
            drawContent()
        }
    }

private fun createBrush(pillarStyle: PillarStyle, size: Size) = when (pillarStyle) {
    PillarStyle.Grey -> Brush.linearGradient(
        0.00f to Color(red = 0.31f, green = 0.31f, blue = 0.31f),
        1.00f to Color.Black,
        start = Offset(0.5f * size.width, 0f * size.height),
        end = Offset(0.5f * size.width, size.height)
    )

    PillarStyle.Rainbow -> rainbowBrush(
        start = Offset(0.8f * size.width, 1.27f * size.height),
        end = Offset(0.76f * size.width, -0.44f * size.height)
    )
}

private fun overlayBrush(size: Size) = Brush.linearGradient(
    0.00f to Color.Black.copy(alpha = 0.1f),
    1f to Color.Black,
    start = Offset(0.5f * size.width, 0.5f * size.height),
    end = Offset(0.5f * size.width, 0.89f * size.height),
)

enum class PillarStyle {
    Grey,
    Rainbow,
}
