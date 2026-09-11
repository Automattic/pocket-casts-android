package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Stable
class TvModalBackdropState {
    private val activeKeys = mutableStateSetOf<Any>()

    val isActive: Boolean get() = activeKeys.isNotEmpty()

    var dialogSize: IntSize? by mutableStateOf(null)

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
    val graphicsLayer = rememberGraphicsLayer()
    val blurRadiusPx = with(LocalDensity.current) { ModalBlurRadius.toPx() }
    val cornerPx = with(LocalDensity.current) { ModalCornerRadius.toPx() }
    Box(
        modifier
            .fillMaxSize()
            .drawWithContent {
                if (progress <= 0f) {
                    drawContent()
                    return@drawWithContent
                }
                drawContent()
                drawRect(Color.Black, alpha = ModalScrimAlpha * progress)
                val dialogSize = state.dialogSize
                if (dialogSize != null && dialogSize.width > 0 && dialogSize.height > 0) {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    graphicsLayer.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Clamp)
                    graphicsLayer.alpha = progress
                    val left = (size.width - dialogSize.width) / 2f
                    val top = (size.height - dialogSize.height) / 2f
                    val panel = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left,
                                top = top,
                                right = left + dialogSize.width,
                                bottom = top + dialogSize.height,
                                cornerRadius = CornerRadius(cornerPx, cornerPx),
                            ),
                        )
                    }
                    clipPath(panel) {
                        drawLayer(graphicsLayer)
                    }
                }
            },
    ) {
        content()
    }
}

internal val TvModalAnimationDurationMillis = 200
internal val ModalCornerRadius = 21.dp
private val ModalBlurRadius = 30.dp
private val ModalScrimAlpha = 0.65f
