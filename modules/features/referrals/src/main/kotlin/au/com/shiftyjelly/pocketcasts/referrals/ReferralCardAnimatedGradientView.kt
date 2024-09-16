package au.com.shiftyjelly.pocketcasts.referrals

import android.os.Build
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import au.com.shiftyjelly.pocketcasts.compose.extensions.rainbowBrush
import au.com.shiftyjelly.pocketcasts.referrals.Position.BottomLeading
import au.com.shiftyjelly.pocketcasts.referrals.Position.BottomTrailing
import au.com.shiftyjelly.pocketcasts.referrals.Position.TopLeading
import au.com.shiftyjelly.pocketcasts.referrals.Position.TopTrailing
import au.com.shiftyjelly.pocketcasts.images.R as IR

private val positionVectors = listOf(
    BottomLeading.vector,
    TopLeading.vector,
    TopTrailing.vector,
    BottomTrailing.vector,
)
private val animationDuration = 5000L * positionVectors.size

@Composable
fun ReferralCardAnimatedBackgroundView(
    modifier: Modifier = Modifier,
) {
    val infinityTransition = rememberInfiniteTransition(label = "infinite transition")
    val animatePosition by infinityTransition.animateValue(
        initialValue = positionVectors[0],
        targetValue = positionVectors[1],
        typeConverter = TwoWayConverter(
            convertToVector = { vector -> vector },
            convertFromVector = { vector ->
                AnimationVector2D(vector.v1, vector.v2)
            },
        ),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = animationDuration.toInt()
                positionVectors.forEachIndexed { index, vector ->
                    val timeStamp = index * (animationDuration.toInt() / positionVectors.size)
                    vector.next() at timeStamp using LinearOutSlowInEasing
                }
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "referral-card-background-animation",
    )

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
            val maxWidthPx = density.run { this@BoxWithConstraints.maxWidth.toPx() }
            val maxHeightPx = density.run { this@BoxWithConstraints.maxHeight.toPx() }

            val offsetX = (animatePosition.v1 * maxWidthPx) / 2f
            val offsetY = (animatePosition.v2 * maxHeightPx) / 2f

            GradientCircle(
                circleSize = circleSize,
                backgroundBrush = rainbowBrush(
                    start = Offset(0.12f * circleSizePx, 0f * circleSizePx),
                    end = Offset(0.89f * circleSizePx, 0.95f * circleSizePx),
                ),
                offset = IntOffset(offsetX.toInt(), offsetY.toInt()),
            )

            GradientCircle(
                circleSize = circleSize,
                backgroundBrush = rainbowBrush(
                    start = Offset(0.29f * circleSizePx, 0.19f * circleSizePx),
                    end = Offset(0.87f * circleSizePx, 1.18f * circleSizePx),
                ),
                offset = IntOffset(-offsetX.toInt(), -offsetY.toInt()),
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

private sealed class Position(val vector: AnimationVector2D) {
    data object TopLeading : Position(AnimationVector2D(-1f, -1f))
    data object BottomLeading : Position(AnimationVector2D(1f, -1f))
    data object BottomTrailing : Position(AnimationVector2D(1f, 1f))
    data object TopTrailing : Position(AnimationVector2D(-1f, 1f))
}

private fun AnimationVector2D.next() = when (this) {
    BottomLeading.vector -> TopLeading.vector
    TopLeading.vector -> TopTrailing.vector
    TopTrailing.vector -> BottomTrailing.vector
    BottomTrailing.vector -> BottomLeading.vector
    else -> BottomLeading.vector
}
