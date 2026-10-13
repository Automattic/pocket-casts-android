package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatTypingIndicator(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    showBubble: Boolean = true,
    dotColor: Color = theme.aiBubbleText,
) {
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    val transition = rememberInfiniteTransition(label = "typing")

    fun dotKeyframes(delayMs: Int) = infiniteRepeatable<Float>(
        animation = keyframes {
            durationMillis = CYCLE_DURATION_MS
            0.3f at 0
            0.3f at delayMs
            1f at delayMs + 300
            0.3f at delayMs + 600
            0.3f at CYCLE_DURATION_MS
        },
    )

    val dot1Alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.3f,
        animationSpec = dotKeyframes(0),
        label = "dot1",
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.3f,
        animationSpec = dotKeyframes(200),
        label = "dot2",
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.3f,
        animationSpec = dotKeyframes(400),
        label = "dot3",
    )
    val answeringDescription = stringResource(LR.string.chat_answering)

    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics {
                contentDescription = answeringDescription
                liveRegion = LiveRegionMode.Polite
            }
            .then(
                if (showBubble) {
                    Modifier
                        .background(theme.aiBubble, shape)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (isReduceMotionEnabled) REDUCED_MOTION_DOT_ALPHA else dot1Alpha)
                .background(dotColor, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (isReduceMotionEnabled) REDUCED_MOTION_DOT_ALPHA else dot2Alpha)
                .background(dotColor, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (isReduceMotionEnabled) REDUCED_MOTION_DOT_ALPHA else dot3Alpha)
                .background(dotColor, CircleShape),
        )
    }
}

private const val CYCLE_DURATION_MS = 1200
private const val REDUCED_MOTION_DOT_ALPHA = 0.6f
