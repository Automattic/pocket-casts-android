package au.com.shiftyjelly.pocketcasts.referrals

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.extensions.rainbowBrush
import au.com.shiftyjelly.pocketcasts.utils.DeviceOrientationDetector
import au.com.shiftyjelly.pocketcasts.utils.OrientationData
import kotlin.coroutines.coroutineContext
import kotlin.math.atan
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun ReferralCardAnimatedBackgroundView(
    modifier: Modifier = Modifier,
) = ReferralCardAnimatedBackgroundView(
    initialOrientation = OrientationData(roll = 0f, pitch = 0f),
    modifier = modifier,
)

@Composable
private fun ReferralCardAnimatedBackgroundView(
    initialOrientation: OrientationData,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .blur(maxWidth / 10)
                .background(Color.Black)
                .fillMaxSize(),
        ) {
            val density = LocalDensity.current
            val circleSize = maxHeight * 1.3f
            val circleSizePx = density.run { circleSize.toPx() }
            val maxWidthPx = density.run { maxWidth.roundToPx() }
            val maxHeightPx = density.run { maxHeight.roundToPx() }

            val animationData = updateAnimationData(initialOrientation, maxWidthPx, maxHeightPx)

            GradientCircle(
                circleSize = circleSize,
                backgroundBrush = rainbowBrush(
                    start = Offset(0.12f * circleSizePx, 0f * circleSizePx),
                    end = Offset(0.89f * circleSizePx, 0.95f * circleSizePx),
                ),
                compatImageId = IR.drawable.referrals_blob_top_left,
                modifier = Modifier
                    .offset(x = -maxWidth / 1.8f, y = -maxHeight / 4f)
                    .offset { animationData.topLeftOffset },
            )

            GradientCircle(
                circleSize = circleSize,
                backgroundBrush = rainbowBrush(
                    start = Offset(0.29f * circleSizePx, 0.19f * circleSizePx),
                    end = Offset(0.87f * circleSizePx, 1.18f * circleSizePx),
                ),
                compatImageId = IR.drawable.referrals_blob_bottom_right,
                modifier = Modifier
                    .offset(x = maxWidth / 1.7f, y = maxHeight / 3.5f)
                    .offset { animationData.bottomRightOffset },
            )
        }
    }
}

@Composable
private fun GradientCircle(
    circleSize: Dp,
    backgroundBrush: Brush,
    @DrawableRes compatImageId: Int,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Box(
            modifier = modifier
                .requiredSize(circleSize)
                .background(
                    brush = backgroundBrush,
                    shape = CircleShape,
                ),
        )
    } else {
        Image(
            painterResource(compatImageId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.requiredSize(circleSize * 1.4f),
        )
    }
}

@Composable
private fun updateAnimationData(
    initialOrientation: OrientationData,
    maxWidthPx: Int,
    maxHeightPx: Int,
): AnimationData {
    val context = LocalContext.current
    val (orientationFlow, animationSpec) = remember {
        val sensorFlow = sensorOrientationFlow(context)
        if (sensorFlow != null) {
            sensorFlow to sensorAnimationSpec
        } else {
            computedOrientationFlow to computedAnimationSpec
        }
    }
    return orientationAnimationData(
        initialOrientation,
        orientationFlow,
        animationSpec,
        maxWidthPx,
        maxHeightPx,
    )
}

private class AnimationData(
    topLeftOffset: State<IntOffset>,
    bottomRightOffset: State<IntOffset>,
) {
    val topLeftOffset by topLeftOffset
    val bottomRightOffset by bottomRightOffset
}

@Composable
private fun orientationAnimationData(
    initialOrientation: OrientationData,
    sensorOrientationFlow: Flow<OrientationData>,
    animationSpec: AnimationSpec<IntOffset>,
    maxWidthPx: Int,
    maxHeightPx: Int,
): AnimationData {
    val orientationData by sensorOrientationFlow.collectAsStateWithLifecycle(
        initialValue = initialOrientation,
        minActiveState = Lifecycle.State.RESUMED,
    )
    val topLeftOffset = animateIntOffsetAsState(
        targetValue = IntOffset(
            x = lerp(
                start = 0,
                stop = maxWidthPx,
                fraction = -angleToFraction(
                    angle = orientationData.roll,
                    positiveFactor = 4f,
                    negativeFactor = 8f,
                ),
            ),
            y = lerp(
                start = 0,
                stop = maxHeightPx,
                fraction = angleToFraction(
                    angle = -orientationData.pitch,
                    positiveFactor = 12f,
                    negativeFactor = 4f,
                ),
            ),
        ),
        animationSpec = animationSpec,
        label = "top-left-offset",
    )
    val bottomRightOffset = animateIntOffsetAsState(
        targetValue = IntOffset(
            x = lerp(
                start = 0,
                stop = maxWidthPx,
                fraction = -angleToFraction(
                    angle = -orientationData.roll,
                    positiveFactor = 6f,
                    negativeFactor = 8f,
                ),
            ),
            y = lerp(
                start = 0,
                stop = maxHeightPx,
                fraction = angleToFraction(
                    angle = orientationData.pitch,
                    positiveFactor = 3f,
                    negativeFactor = 6f,
                ),
            ),
        ),
        animationSpec = animationSpec,
        label = "bottom-right-offset",
    )
    return remember(sensorOrientationFlow) { AnimationData(topLeftOffset, bottomRightOffset) }
}

private fun angleToFraction(
    angle: Float,
    positiveFactor: Float,
    negativeFactor: Float,
): Float {
    val fraction = (angle / maxValueAngle).coerceIn(-1f, 1f)
    val scaledAngle = lerp(0f, Math.PI.toFloat(), fraction)
    val factor = if (scaledAngle >= 0) positiveFactor else negativeFactor
    return (atan(scaledAngle) / (factor * Math.PI)).toFloat()
}

private val maxValueAngle = (Math.PI / 4).toFloat()

@OptIn(FlowPreview::class)
private fun sensorOrientationFlow(context: Context) = DeviceOrientationDetector.create(context)
    ?.orientationData()
    ?.sample(100.milliseconds)

private val sensorAnimationSpec = spring(
    stiffness = 20f,
    visibilityThreshold = IntOffset(1, 1),
)

private val computedOrientationFlow = flow {
    var angle = -maxValueAngle
    while (coroutineContext.isActive) {
        emit(OrientationData(angle, angle))
        angle = if (angle > 0) -maxValueAngle else maxValueAngle
        delay(4.seconds)
    }
}

private val computedAnimationSpec = spring(
    stiffness = 2f,
    visibilityThreshold = IntOffset(1, 1),
)

@Preview
@Composable
private fun CardBackgroundPreview(
    @PreviewParameter(OrientationDataProvider::class) data: OrientationData,
) {
    ReferralCardAnimatedBackgroundView(
        initialOrientation = data,
        modifier = Modifier.size(DpSize(150.dp, 150.dp * ReferralGuestPassCardDefaults.cardAspectRatio)),
    )
}

private class OrientationDataProvider : PreviewParameterProvider<OrientationData> {
    override val values = sequenceOf(
        OrientationData(
            roll = 0f,
            pitch = 0f,
        ),
        OrientationData(
            roll = Math.PI.toFloat(),
            pitch = 0f,
        ),
        OrientationData(
            roll = Math.PI.toFloat(),
            pitch = Math.PI.toFloat() / 2,
        ),
        OrientationData(
            roll = 0f,
            pitch = Math.PI.toFloat() / 2,
        ),
        OrientationData(
            roll = -Math.PI.toFloat(),
            pitch = Math.PI.toFloat() / 2,
        ),
        OrientationData(
            roll = -Math.PI.toFloat(),
            pitch = 0f,
        ),
        OrientationData(
            roll = -Math.PI.toFloat(),
            pitch = -Math.PI.toFloat() / 2,
        ),
        OrientationData(
            roll = 0f,
            pitch = -Math.PI.toFloat() / 2,
        ),
    )
}
