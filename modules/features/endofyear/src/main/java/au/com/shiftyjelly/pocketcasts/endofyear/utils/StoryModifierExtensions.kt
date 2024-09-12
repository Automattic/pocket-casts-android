package au.com.shiftyjelly.pocketcasts.endofyear.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import au.com.shiftyjelly.pocketcasts.compose.extensions.gradientBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.rainbowBrush
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

fun Modifier.podcastDynamicBackground(podcast: Podcast) =
    gradientBackground(Color(podcast.getTintColor(false)))

fun Modifier.textGradient() =
    graphicsLayer { alpha = 0.99f }
        .drawWithCache {
            val brush = rainbowBrush(
                start = Offset(-0.3f * size.width, -0.27f * size.height),
                end = Offset(1.5f * size.width, 1.19f * size.height),
            )
            onDrawWithContent {
                drawContent()
                drawRect(
                    brush = brush,
                    blendMode = BlendMode.SrcAtop,
                )
            }
        }
