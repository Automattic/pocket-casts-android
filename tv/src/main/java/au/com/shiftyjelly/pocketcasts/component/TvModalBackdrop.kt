package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Stable
class TvModalBackdropState {
    private val activeKeys = mutableStateSetOf<Any>()

    val isActive: Boolean get() = activeKeys.isNotEmpty()

    fun setActive(key: Any, active: Boolean) {
        if (active) {
            activeKeys.add(key)
        } else {
            activeKeys.remove(key)
        }
    }
}

val LocalTvModalBackdrop = compositionLocalOf { TvModalBackdropState() }

@Composable
fun TvModalBackdrop(
    state: TvModalBackdropState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (state.isActive) 1f else 0f,
        animationSpec = tween(TvModalAnimationDurationMillis),
        label = "TvModalBackdrop",
    )
    Box(modifier.fillMaxSize()) {
        Box(
            modifier = if (progress > 0f) Modifier.blur(ModalBlurRadius * progress) else Modifier,
        ) {
            content()
        }
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = ModalScrimAlpha * progress)),
            )
        }
    }
}

internal val TvModalAnimationDurationMillis = 200
private val ModalBlurRadius = 30.dp
private val ModalScrimAlpha = 0.4f
