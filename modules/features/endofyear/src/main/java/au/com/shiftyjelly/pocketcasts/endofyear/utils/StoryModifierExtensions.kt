package au.com.shiftyjelly.pocketcasts.endofyear.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

private const val Black60 = 0x99000000

fun Modifier.podcastDynamicBackground(podcast: Podcast) =
    dynamicBackground(Color(podcast.getTintColor(false)))

fun Modifier.dynamicBackground(
    baseColor: Color,
    colorStops: List<Color> = listOf(Color.Black, Color(Black60)),
    direction: FadeDirection = FadeDirection.TopToBottom,
) =
    graphicsLayer {
        /*
        https://rb.gy/iju6fn
        This is required to render to an offscreen buffer
        The Clear blend mode will not work without it */
        alpha = 0.99f
    }.drawWithContent {
        drawRect(color = baseColor)
        drawRect(
            brush = Brush.verticalGradient(
                colorStops,
                startY = if (direction == FadeDirection.BottomToTop) Float.POSITIVE_INFINITY else 0f,
                endY = if (direction == FadeDirection.BottomToTop) 0f else Float.POSITIVE_INFINITY,
            ),
            blendMode = BlendMode.DstIn
        )
        drawContent()
    }

fun Modifier.textGradient() =
    graphicsLayer { alpha = 0.99f }
        .drawWithCache {
            val brush = Brush.linearGradient(
                0.00f to Color(red = 0.25f, green = 0.11f, blue = 0.92f),
                0.24f to Color(red = 0.68f, green = 0.89f, blue = 0.86f),
                0.50f to Color(red = 0.87f, green = 0.91f, blue = 0.53f),
                0.74f to Color(red = 0.91f, green = 0.35f, blue = 0.26f),
                1.00f to Color(red = 0.1f, green = 0.1f, blue = 0.1f),
                start = Offset(-0.3f * size.width, -0.27f * size.height),
                end = Offset(1.5f * size.width, 1.19f * size.height),
            )
            onDrawWithContent {
                drawContent()
                drawRect(
                    brush = brush,
                    blendMode = BlendMode.SrcAtop
                )
            }
        }

enum class FadeDirection {
    TopToBottom,
    BottomToTop
}
