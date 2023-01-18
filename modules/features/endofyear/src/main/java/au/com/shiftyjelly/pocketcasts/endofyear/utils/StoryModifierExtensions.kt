package au.com.shiftyjelly.pocketcasts.endofyear.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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

enum class FadeDirection {
    TopToBottom,
    BottomToTop
}
