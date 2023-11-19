package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.endofyear.utils.rainbowBrush
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

private const val AnimDurationInMs = 800
private const val AnimAlphaDelayInMs = 500

@Composable
fun GradientPillar(
    pillarStyle: PillarStyle,
    paused: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (Float) -> Unit,
) {
    val animationSpec = tween<Float>(
        durationMillis = AnimDurationInMs,
        delayMillis = 0,
    )
    val animationAlphaSpec = tween<Float>(
        durationMillis = AnimDurationInMs,
        delayMillis = AnimAlphaDelayInMs,
    )
    val animOffsetY = remember { Animatable(1f) }
    val animAlpha = remember { Animatable(0f) }
    LaunchedEffect(paused) {
        try {
            if (paused) {
                /* Stop animations when story is paused */
                animOffsetY.stop()
                animAlpha.stop()
            }
            /* Launch concurrent offset animations */
            launch {
                if (!paused) {
                    animOffsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = animationSpec
                    )
                }
            }
            launch {
                if (!paused) {
                    animAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = animationAlphaSpec
                    )
                }
            }
        } catch (e: CancellationException) {
            Timber.e(e)
        }
    }

    Box(
        contentAlignment = Alignment.TopStart,
        modifier = modifier
            .pillarGradient(pillarStyle, animOffsetY.value)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        content(animAlpha.value)
    }
}

private fun Modifier.pillarGradient(
    pillarStyle: PillarStyle,
    heightFraction: Float,
) = graphicsLayer { alpha = 0.99f }
    .drawWithCache {
        val size = Size(size.width, size.height)
        val brush = createBrush(pillarStyle, size)
        val overlayBrush = overlayBrush(size)

        onDrawWithContent {
            drawRect(
                brush = brush,
                blendMode = BlendMode.SrcOver,
                topLeft = Offset(0.0f, heightFraction * size.height),
            )
            drawRect(
                brush = overlayBrush,
                blendMode = BlendMode.SrcOver,
                topLeft = Offset(0.0f, heightFraction * size.height),
            )
            drawContent()
        }
    }

private fun createBrush(pillarStyle: PillarStyle, size: Size) = when (pillarStyle) {
    PillarStyle.Grey -> Brush.linearGradient(
        0.00f to Color(red = 0.31f, green = 0.31f, blue = 0.31f),
        1.00f to Color.Black,
        start = Offset(0.5f * size.width, 0f * size.height),
        end = Offset(0.5f * size.width, size.height)
    )

    PillarStyle.Rainbow -> rainbowBrush(
        start = Offset(0.8f * size.width, 1.27f * size.height),
        end = Offset(0.76f * size.width, -0.44f * size.height)
    )
}

private fun overlayBrush(size: Size) = Brush.linearGradient(
    0.00f to Color.Black.copy(alpha = 0.1f),
    1f to Color.Black,
    start = Offset(0.5f * size.width, 0.5f * size.height),
    end = Offset(0.5f * size.width, 0.89f * size.height),
)

enum class PillarStyle {
    Grey,
    Rainbow,
}

@ShowkaseComposable(name = "GradientPillar", group = "Images", styleName = "Rainbow", defaultStyle = true)
@Preview(name = "Rainbow")
@Composable
fun GradientPillarRainbowPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        GradientPillar(
            pillarStyle = PillarStyle.Rainbow,
            content = {},
            paused = true,
            modifier = Modifier
                .size(50.dp, 200.dp)
        )
    }
}

@ShowkaseComposable(name = "GradientPillar", group = "Images", styleName = "Grey")
@Preview(name = "Grey")
@Composable
fun GradientPillarGreyPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        GradientPillar(
            pillarStyle = PillarStyle.Grey,
            content = {},
            paused = true,
            modifier = Modifier
                .size(50.dp, 200.dp)
        )
    }
}
