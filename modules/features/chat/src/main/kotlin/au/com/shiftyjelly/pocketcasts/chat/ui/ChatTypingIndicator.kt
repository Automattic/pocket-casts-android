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
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatTypingIndicator(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val cycleDuration = 1200
    val transition = rememberInfiniteTransition(label = "typing")

    fun dotKeyframes(delayMs: Int) = infiniteRepeatable<Float>(
        animation = keyframes {
            durationMillis = cycleDuration
            0.3f at 0
            0.3f at delayMs
            1f at delayMs + 300
            0.3f at delayMs + 600
            0.3f at cycleDuration
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

    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(theme.aiBubble, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(dot1Alpha)
                .background(theme.aiBubbleText, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(dot2Alpha)
                .background(theme.aiBubbleText, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(dot3Alpha)
                .background(theme.aiBubbleText, CircleShape),
        )
    }
}
