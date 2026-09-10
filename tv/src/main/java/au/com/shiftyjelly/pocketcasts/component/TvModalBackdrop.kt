package au.com.shiftyjelly.pocketcasts.component

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
    val blurRadiusPx = with(LocalDensity.current) { ModalBlurRadius.toPx() }
    Box(
        modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (progress > 0f) {
                    drawRect(Color.Black, alpha = ModalScrimAlpha * progress)
                }
            }
            .graphicsLayer {
                renderEffect = if (progress > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val radius = blurRadiusPx * progress
                    BlurEffect(radius, radius, TileMode.Clamp)
                } else {
                    null
                }
            },
    ) {
        content()
    }
}

internal val TvModalAnimationDurationMillis = 200
private val ModalBlurRadius = 30.dp
private val ModalScrimAlpha = 0.4f
