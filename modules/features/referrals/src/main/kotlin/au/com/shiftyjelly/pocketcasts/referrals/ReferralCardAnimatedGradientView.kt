package au.com.shiftyjelly.pocketcasts.referrals

import android.os.Build
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.extensions.rainbowBrush
import au.com.shiftyjelly.pocketcasts.utils.DeviceOrientationDetector
import au.com.shiftyjelly.pocketcasts.utils.OrientationData
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.sample
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun ReferralCardAnimatedBackgroundView(
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .blur(maxWidth / 3)
                .background(Color.Black)
                .clipToBounds(),
        ) {
            val density = LocalDensity.current
            val circleSize = this@BoxWithConstraints.maxHeight
            val circleSizePx = density.run { circleSize.toPx() }
            val maxWidthPx = density.run { this@BoxWithConstraints.maxWidth.roundToPx() }
            val maxHeightPx = density.run { this@BoxWithConstraints.maxHeight.roundToPx() }

            val animationData = updateAnimationData(maxWidthPx, maxHeightPx)

            GradientCircle(
                circleSize = circleSize,
                backgroundBrush = rainbowBrush(
                    start = Offset(0.12f * circleSizePx, 0f * circleSizePx),
                    end = Offset(0.89f * circleSizePx, 0.95f * circleSizePx),
                ),
                offset = animationData.offset,
            )

            GradientCircle(
                circleSize = circleSize,
                backgroundBrush = rainbowBrush(
                    start = Offset(0.29f * circleSizePx, 0.19f * circleSizePx),
                    end = Offset(0.87f * circleSizePx, 1.18f * circleSizePx),
                ),
                offset = IntOffset(-animationData.offset.x, -animationData.offset.y),
                rotation = 45f,
            )
        }
    }
}

@Composable
private fun GradientCircle(
    circleSize: Dp,
    offset: IntOffset,
    backgroundBrush: Brush,
    rotation: Float = 0f,
) {
    val modifier = Modifier
        .offset { offset }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Box(
            modifier = modifier
                .size(circleSize)
                .background(
                    brush = backgroundBrush,
                    shape = CircleShape,
                ),
        )
    } else {
        Image(
            painterResource(IR.drawable.blurred_rainbow_circle),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .rotate(rotation)
                .size(circleSize * 1.25f),
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun updateAnimationData(
    maxWidthPx: Int,
    maxHeightPx: Int,
): AnimationData {
    val context = LocalContext.current
    val sensorOrientationFlow = remember { DeviceOrientationDetector.create(context)?.orientationData()?.sample(100.milliseconds) }

    return if (sensorOrientationFlow == null) {
        infinityAnimationData(maxWidthPx, maxHeightPx)
    } else {
        sensorAnimationData(sensorOrientationFlow, maxWidthPx, maxHeightPx)
    }
}

private class AnimationData(
    offset: State<IntOffset>,
) {
    val offset by offset
}

@Composable
private fun infinityAnimationData(
    maxWidthPx: Int,
    maxHeightPx: Int,
): AnimationData {
    val infinityTransition = rememberInfiniteTransition(label = "infinite transition")
    val animatedOffset = infinityTransition.animateValue(
        initialValue = Position.BottomLeading.vector.toInOffset(maxWidthPx, maxHeightPx),
        targetValue = Position.TopLeading.vector.toInOffset(maxWidthPx, maxHeightPx),
        typeConverter = TwoWayConverter(
            convertToVector = { offset -> offset.toVector(maxWidthPx, maxHeightPx) },
            convertFromVector = { vector -> vector.toInOffset(maxWidthPx, maxHeightPx) },
        ),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = animationDuration.toInt()
                Position.entries.forEachIndexed { index, position ->
                    val timeStamp = index * (animationDuration.toInt() / Position.entries.size)
                    position.next().vector.toInOffset(maxWidthPx, maxHeightPx) at timeStamp using LinearOutSlowInEasing
                }
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "referral-card-background-animation",
    )
    return remember(infinityTransition) { AnimationData(animatedOffset) }
}

private enum class Position(val vector: AnimationVector2D) {
    BottomLeading(AnimationVector2D(1f, -1f)),
    TopLeading(AnimationVector2D(-1f, -1f)),
    TopTrailing(AnimationVector2D(-1f, 1f)),
    BottomTrailing(AnimationVector2D(1f, 1f)),
    ;

    fun next() = when (this) {
        BottomLeading -> TopLeading
        TopLeading -> TopTrailing
        TopTrailing -> BottomTrailing
        BottomTrailing -> BottomLeading
    }
}

private val animationDuration = 5000L * Position.entries.size

private fun IntOffset.toVector(maxWidthPx: Int, maxHeightPx: Int) = AnimationVector2D(
    v1 = 2f * x / maxWidthPx,
    v2 = 2f * y / maxHeightPx,
)

private fun AnimationVector2D.toInOffset(maxWidthPx: Int, maxHeightPx: Int) = IntOffset(
    x = (v1 * maxWidthPx / 2f).roundToInt(),
    y = (v2 * maxHeightPx / 2f).roundToInt(),
)

@Composable
private fun sensorAnimationData(
    sensorOrientationFlow: Flow<OrientationData>,
    maxWidthPx: Int,
    maxHeightPx: Int,
): AnimationData {
    val orientationData by sensorOrientationFlow.collectAsStateWithLifecycle(initialValue = null, minActiveState = Lifecycle.State.RESUMED)
    val offset = animateIntOffsetAsState(
        targetValue = IntOffset(
            x = lerp(
                start = 0,
                stop = maxWidthPx / 2,
                fraction = orientationData?.rollOffsetFraction() ?: -1f,
            ),
            y = lerp(
                start = 0,
                stop = maxHeightPx / 2,
                fraction = orientationData?.pitchOffsetFraction() ?: -1f,
            ),
        ),
        animationSpec = sensorAnimationSpec,
        label = "referral-card-background-animation",
    )
    return remember(sensorOrientationFlow) { AnimationData(offset) }
}

private val sensorAnimationSpec = spring(
    stiffness = Spring.StiffnessVeryLow,
    visibilityThreshold = IntOffset(1, 1),
)

private fun OrientationData.rollOffsetFraction(): Float {
    val fraction = (roll / (2 * Math.PI)).toFloat().absoluteValue
    return when (fraction) {
        in 0f..0.25f -> cubic(a = 303.7037, b = -157.4603, c = 28.3519, d = -1.0, value = fraction.toDouble()).toFloat()
        else -> 1f
    }
}

private fun OrientationData.pitchOffsetFraction(): Float {
    val fraction = (pitch / Math.PI).toFloat().absoluteValue
    return when (fraction) {
        in 0f..0.25f -> cubic(a = 303.7037, b = -157.4603, c = 28.3519, d = -1.0, value = fraction.toDouble()).toFloat()
        else -> 1f
    }
}

private fun cubic(a: Double, b: Double, c: Double, d: Double, value: Double): Double {
    return a * value * value * value + b * value * value + c * value + d
}
