package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.tvFocusedCardDepth(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(12.dp),
): Modifier = composed {
    val elevation by animateDpAsState(
        targetValue = if (isFocused) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "TvCardDepthElevation",
    )
    shadow(elevation = elevation, shape = shape, clip = false)
}
