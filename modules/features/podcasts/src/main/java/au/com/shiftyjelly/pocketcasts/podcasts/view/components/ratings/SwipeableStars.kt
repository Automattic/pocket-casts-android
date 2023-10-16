package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.theme
import kotlin.math.abs
import kotlin.math.max

private const val numStars = 5

@Composable
fun SwipeableStars(
    onStarsChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {

    var stopPointType by remember { mutableStateOf(StopPointType.FullAndHalfStars) }
    var changeType by remember { mutableStateOf(ChangeType.Animated) }
    var touchX by remember { mutableStateOf(0f) }
    var iconPositions by remember { mutableStateOf(listOf<Position>()) }

    val stopPoints: List<Double> = stopPointsFromIconPositions(iconPositions)
    val desiredStopPoint = getDesiredStopPoint(
        touchX = touchX,
        stopPoints = stopPoints,
        stopPointType = stopPointType
    )
    onStarsChanged(getStarsDouble(stopPoints, desiredStopPoint))

    val sliderPosition = remember {
        Animatable(initialValue = 0f)
    }
    LaunchedEffect(touchX, desiredStopPoint) {
        when (changeType) {
            ChangeType.Immediate -> sliderPosition.snapTo(touchX)
            ChangeType.Animated -> sliderPosition.animateTo(
                targetValue = desiredStopPoint,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    Box(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        stopPointType = StopPointType.None
                        changeType = ChangeType.Immediate
                        touchX += dragAmount
                    },
                    onDragEnd = {
                        stopPointType = StopPointType.FullAndHalfStars
                        changeType = ChangeType.Animated
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    // Check onPress so we can jump to the touch point as soon as a drag
                    // starts. We're using onPress instead of onDragStart because it seems better
                    // to update touchX immediately when the screen is touched instead of
                    // waiting for the drag to start. But set this as a dragging touch state
                    // so that the position stays at the touch point instead of jumping to a
                    // stop point.
                    onPress = {
                        stopPointType = StopPointType.None
                        changeType = ChangeType.Animated
                        touchX = it.x
                    },
                    onTap = {
                        stopPointType = StopPointType.FullStars
                        changeType = ChangeType.Animated
                        touchX = it.x
                    }
                )
            }
    ) {
        Stars(filled = false)

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .drawWithLayer {
                    // Destination
                    drawContent()

                    // Source
                    drawRect(
                        topLeft = Offset(sliderPosition.value, 0f),
                        color = Color.Transparent,
                        blendMode = BlendMode.SrcOut,
                    )
                }
        ) {
            val context = LocalContext.current
            // Only check this once, this means that switching TalkBack on/off while on this screen
            // will not update the screen and the user will have to exit and come back. This is
            // something that could be improved.
            val isTalkbackEnabled = remember { isTalkbackEnabled(context) }

            Stars(
                filled = true,
                modifier = { index ->
                    var right by remember { mutableStateOf(0f) }
                    Modifier
                        // We could have applied this onGloballyPositioned modifier to the empty stars with the same effect
                        .onGloballyPositioned {
                            val left = it.positionInParent().x
                            right = left + it.size.width
                            iconPositions += Position(
                                left = left,
                                right = right,
                            )
                        }
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .then(
                            if (isTalkbackEnabled) {
                                Modifier
                                    .clickable {
                                        touchX = right // select the full star
                                    }
                                    .semantics {
                                        contentDescription = "${index + 1} Stars"
                                        role = Role.Button
                                    }
                            } else Modifier
                        )
                }
            )
        }
    }
}

// Takes the left and right coordinates for the icons and finds the
// points that are between them and in the middle of them since those
// are the positions we want to fill to in order to get unfilled,
// half-filled, and fully-filled icons.
@Composable
private fun stopPointsFromIconPositions(positions: List<Position>) =
    remember(positions) {
        if (positions.isEmpty()) {
            emptyList()
        } else {
            val intermediate = positions
                .flatMap { listOf(it.left, it.right) }
                .windowed(size = 2, step = 1)
                .map { it.average() }

            buildList {
                add(positions.first().left.toDouble())
                addAll(intermediate)
                add(positions.last().right.toDouble())
            }
        }
    }

@Composable
private fun getDesiredStopPoint(
    touchX: Float,
    stopPoints: List<Double>,
    stopPointType: StopPointType
) = remember(stopPoints, touchX, stopPointType) {
    when (stopPointType) {

        StopPointType.None -> touchX // ignore stop points

        StopPointType.FullAndHalfStars ->
            stopPoints
                .minByOrNull { abs(it - touchX) }
                ?.toFloat()
                ?: 0f

        StopPointType.FullStars -> {

            // These stop points are used to determine which star to fill based on the
            // the touch point. For that reason, these stop points are in the middle of the stars.
            val touchStopPoints = buildList {
                val stopPointsInTheMiddleOfStars = stopPoints
                    .filterIndexed { i, _ -> i % 2 != 0 }

                // Add the 0.0 stop point so the user can tap the first quarter
                // of the first star to select 0 stars. Anything to further to the
                // right will be closer to the stop point in the middle of the first
                // star, which gets translated to the first full star.
                add(0.0)
                addAll(stopPointsInTheMiddleOfStars)
            }
            val nearestTouchStopPoint = touchStopPoints
                .minByOrNull {
                    // Assumes all of the stop points are the same distance apart

                    abs(it - touchX)
                }
                ?.toFloat()
                ?: 0f

            // These stop points are in between the stars, so filling to one of them will
            // result in every star being either entirely filled or entirely unfilled
            val betweenStarStopPoints = stopPoints.filterIndexed { i, _ -> i % 2 == 0 }

            // Use the relevant touch stop point to determine which between star stop point to fill to
            val nearestFullStarStopPoint = betweenStarStopPoints
                .minByOrNull {
                    // We've filtered the touch stop points to only include the stop points that
                    // are in the middle of each star for detecting touch, so we need to add
                    // a half-icon width to them to make them match up with the between star stop
                    // points.
                    val halfIconWidth = stopPoints[1] - stopPoints[0]
                    val translatedTouchStopPoint = nearestTouchStopPoint + halfIconWidth
                    abs(it - translatedTouchStopPoint)
                }
                ?.toFloat()
                ?: 0f
            nearestFullStarStopPoint
        }
    }
}

@Composable
fun Stars(
    filled: Boolean,
    modifier: @Composable (index: Int) -> Modifier = { Modifier },
) {
    Row {
        for (index in 0 until numStars) {
            Icon(
                imageVector = if (filled) {
                    Icons.Filled.Star
                } else {
                    Icons.Filled.StarBorder
                },
                tint = MaterialTheme.theme.colors.primaryIcon01,
                contentDescription = null,
                modifier = modifier(index)
                    .fillMaxHeight()
                    .aspectRatio(1f)
            )
        }
    }
}

// Returns the numbers of stars as a double: 0.0, 0.5, 1.0, ..., 5.0
@Composable
private fun getStarsDouble(stopPoints: List<Double>, positionX: Float): Double {
    val nearestStopPoint = stopPoints.minByOrNull { abs(it - positionX) }
    val index = stopPoints.indexOf(nearestStopPoint)
    val starsPerStopPoint = 0.5
    return max(0, index) * starsPerStopPoint
}

// From https://stackoverflow.com/a/73590696/1910286
private fun Modifier.drawWithLayer(block: ContentDrawScope.() -> Unit) = this.then(
    Modifier.drawWithContent {
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            block()
            restoreToCount(checkPoint)
        }
    }
)

private data class Position(
    val left: Float,
    val right: Float,
)

// A stop point is a point at which the slider will come to stop
private enum class StopPointType {
    None, // No stop points
    FullStars, // stop points for full stars only
    FullAndHalfStars, // stop points for full and half stars
}

private enum class ChangeType {
    Immediate,
    Animated,
}

private fun isTalkbackEnabled(context: Context): Boolean {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager?
    val isEnabled = accessibilityManager?.isEnabled ?: false
    val isTouchExplorationEnabled = accessibilityManager?.isTouchExplorationEnabled ?: false
    return isEnabled && isTouchExplorationEnabled
}

@Preview
@Composable
private fun SwipeableStarsPreview() {
    SwipeableStars(
        onStarsChanged = {},
        modifier = Modifier.size(
            height = 30.dp,
            width = 150.dp,
        )
    )
}
