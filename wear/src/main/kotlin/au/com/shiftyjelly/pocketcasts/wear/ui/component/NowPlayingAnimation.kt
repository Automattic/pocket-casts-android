package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme

/**
 * Simple "now playing" animation using Compose animations instead of Lottie.
 * This is a lightweight alternative that shows 3 animated bars to indicate playback.
 *
 * This replaces the Lottie animation (nowplaying.json) and helps reduce APK size
 * by removing the Lottie dependency (~3-5MB).
 */
@Composable
fun NowPlayingAnimation(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nowPlaying")

    // Animate three bars with different delays for a wave effect
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar1",
    )

    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar2",
    )

    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar3",
    )

    Canvas(modifier = modifier.size(24.dp)) {
        val barWidth = size.width / 5f
        val spacing = barWidth / 2f
        val maxHeight = size.height
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        // Draw three animated bars
        listOf(bar1Height, bar2Height, bar3Height).forEachIndexed { index, height ->
            val barHeight = maxHeight * height
            val x = index * (barWidth + spacing)
            val y = maxHeight - barHeight

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius,
            )
        }
    }
}
