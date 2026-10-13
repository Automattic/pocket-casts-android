package au.com.shiftyjelly.pocketcasts.chat.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberIsReduceMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
internal fun ChatAppear(
    animate: Boolean,
    modifier: Modifier = Modifier,
    rise: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    val progress = remember { Animatable(if (animate && !isReduceMotionEnabled) 0f else 1f) }
    LaunchedEffect(progress) {
        progress.animateTo(1f, tween(durationMillis = APPEAR_DURATION_MS, easing = EaseOut))
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * rise.toPx()
            compositingStrategy = CompositingStrategy.ModulateAlpha
        },
    ) {
        content()
    }
}

private const val APPEAR_DURATION_MS = 200
