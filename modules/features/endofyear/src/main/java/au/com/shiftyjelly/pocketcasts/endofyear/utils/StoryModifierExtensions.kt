package au.com.shiftyjelly.pocketcasts.endofyear.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

fun Modifier.podcastDynamicBackground(podcast: Podcast) =
    graphicsLayer {
        /*
        https://rb.gy/iju6fn
        This is required to render to an offscreen buffer
        The Clear blend mode will not work without it */
        alpha = 0.99f
    }.drawWithContent {
        val colors = listOf(
            Color.Black,
            Color(0x80000000), // 50% Black
        )
        drawRect(color = Color(podcast.getTintColor(false)))
        drawRect(
            brush = Brush.verticalGradient(
                colors,
                startY = Float.POSITIVE_INFINITY,
                endY = 0f,
            ),
            blendMode = BlendMode.DstIn
        )
        drawContent()
    }
