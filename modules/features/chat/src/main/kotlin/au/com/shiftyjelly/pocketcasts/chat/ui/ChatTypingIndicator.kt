package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

@Composable
internal fun ChatTypingIndicator(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    transition: InfiniteTransition = rememberInfiniteTransition(label = "typing"),
) {
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        repeat(DOT_COUNT) { index ->
            val alpha = transition.dotAlpha(peakMs = index * DOT_STAGGER_MS)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { this.alpha = if (isReduceMotionEnabled) REDUCED_MOTION_DOT_ALPHA else alpha.value }
                    .background(theme.primaryText, CircleShape),
            )
        }
    }
}

@Composable
private fun InfiniteTransition.dotAlpha(peakMs: Int): State<Float> = animateFloat(
    initialValue = MIN_DOT_ALPHA,
    targetValue = MIN_DOT_ALPHA,
    animationSpec = infiniteRepeatable(
        animation = keyframes {
            durationMillis = CYCLE_DURATION_MS
            for (timeMs in 0..CYCLE_DURATION_MS step DOT_STAGGER_MS) {
                dotAlphaAt(timeMs, peakMs) at timeMs using LinearEasing
            }
        },
    ),
    label = "dot$peakMs",
)

private fun dotAlphaAt(timeMs: Int, peakMs: Int): Float {
    val distance = abs(timeMs - peakMs)
    return when (min(distance, CYCLE_DURATION_MS - distance)) {
        0 -> 1f
        DOT_STAGGER_MS -> 0.6f
        else -> MIN_DOT_ALPHA
    }
}

private const val DOT_COUNT = 3
private const val DOT_STAGGER_MS = 200
private const val CYCLE_DURATION_MS = 1200
private const val MIN_DOT_ALPHA = 0.3f
private const val REDUCED_MOTION_DOT_ALPHA = 0.6f
