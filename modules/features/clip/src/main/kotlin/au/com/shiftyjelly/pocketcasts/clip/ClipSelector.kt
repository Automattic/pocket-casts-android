package au.com.shiftyjelly.pocketcasts.clip

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.utils.extensions.ceilDiv
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ClipSelector(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    isPlaying: Boolean,
    clipColors: ClipColors,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onClipStartUpdate: (Duration) -> Unit,
    onClipEndUpdate: (Duration) -> Unit,
    modifier: Modifier = Modifier,
    state: ClipSelectorState = rememberClipSelectorState(),
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0x476B6B6B), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
    ) {
        Image(
            painter = painterResource(if (isPlaying) IR.drawable.ic_widget_pause else IR.drawable.ic_widget_play),
            contentDescription = stringResource(if (isPlaying) LR.string.pause else LR.string.play),
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .clickable(
                    interactionSource = remember(::MutableInteractionSource),
                    indication = rememberRipple(color = clipColors.baseColor),
                    onClickLabel = stringResource(if (isPlaying) LR.string.pause else LR.string.play),
                    role = Role.Button,
                    onClick = if (isPlaying) onPauseClick else onPlayClick,
                )
                .padding(16.dp),
        )
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        ClipSelector(
            episodeDuration = episodeDuration,
            clipRange = clipRange,
            clipColors = clipColors,
            onClipStartUpdate = onClipStartUpdate,
            onClipEndUpdate = onClipEndUpdate,
            state = state,
        )
    }
}

@Composable
private fun ClipSelector(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    clipColors: ClipColors,
    state: ClipSelectorState,
    onClipStartUpdate: (Duration) -> Unit,
    onClipEndUpdate: (Duration) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        ClipTimeline(
            episodeDuration = episodeDuration,
            clipRange = clipRange,
            clipColors = clipColors,
            state = state,
            onClipStartUpdate = onClipStartUpdate,
            onClipEndUpdate = onClipEndUpdate,
        )
        ClipBox(
            episodeDuration = episodeDuration,
            clipRange = clipRange,
            clipColors = clipColors,
            state = state,
            onClipStartUpdate = onClipStartUpdate,
            onClipEndUpdate = onClipEndUpdate,
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.ClipTimeline(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    clipColors: ClipColors,
    state: ClipSelectorState,
    onClipStartUpdate: (Duration) -> Unit,
    onClipEndUpdate: (Duration) -> Unit,
) {
    val localDensity = LocalDensity.current
    val largeTickHeight = maxHeight / 3
    val mediumTickHeight = maxHeight / 6
    val smallTickHeight = maxHeight / 12

    val tickWidth = 1.5.dp
    val spaceWidth = 4.dp * state.scale
    val itemWidth = tickWidth + spaceWidth
    state.itemWidth = with(localDensity) { itemWidth.toPx() }
    LaunchedEffect(state.scale) { state.scaleBoxOffsets(clipRange) }

    val maxTickResolution by remember {
        val secondsCount = episodeDuration.inWholeSeconds.toInt()
        var resolution = 1
        val minItemWidth = 1.5.dp + 4.dp
        while (resolution < 125) {
            val newResolution = resolution * 5
            val tickCount = (secondsCount / newResolution) + 1
            val totalWidth = minItemWidth * tickCount
            if (totalWidth <= maxWidth) {
                break
            } else {
                resolution = newResolution
            }
        }
        mutableIntStateOf(resolution.coerceAtLeast(1))
    }

    val transformation = rememberTransformableState { zoom, _, _ ->
        val newScale = state.scale * zoom
        when {
            newScale > 5f -> if (state.tickResolution != 1) {
                state.tickResolution /= 5
                state.scale = 1f
            }
            newScale < 1f -> if (state.tickResolution != maxTickResolution) {
                state.tickResolution *= 5
                state.scale = 5f
            }
            else -> state.scale = newScale
        }
    }

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        state = state.listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 6.dp)
            .background(clipColors.backgroundColor)
            .transformable(transformation)
            .pointerInput(clipRange) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        val offsetX = tapOffset.x + state.scrollOffset
                        val startDistance = (state.startOffset - offsetX).absoluteValue
                        val endDistance = (state.endOffset - offsetX).absoluteValue
                        val isStartHandle = startDistance < endDistance
                        if (isStartHandle) {
                            val newClipStart = state.pixelsToDuration(offsetX)
                            if (newClipStart >= Duration.ZERO && (clipRange.end - newClipStart) >= 1.seconds) {
                                state.startOffset = offsetX
                                onClipStartUpdate(newClipStart)
                            }
                        } else {
                            val newClipEnd = state.pixelsToDuration(offsetX)
                            if (newClipEnd <= episodeDuration && (newClipEnd - clipRange.start) >= 1.seconds) {
                                state.endOffset = offsetX
                                onClipEndUpdate(newClipEnd)
                            }
                        }
                    },
                )
            },
    ) {
        items(episodeDuration.inWholeSeconds.toInt().ceilDiv(state.tickResolution) + 1) { index ->
            val heightIndex = when (index % 10) {
                0 -> largeTickHeight
                5 -> mediumTickHeight
                else -> smallTickHeight
            }
            Box(
                modifier = Modifier
                    .padding(end = spaceWidth)
                    .width(tickWidth)
                    .height(heightIndex)
                    .background(clipColors.selectorTimelineColor),
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.ClipBox(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    clipColors: ClipColors,
    onClipStartUpdate: (Duration) -> Unit,
    onClipEndUpdate: (Duration) -> Unit,
    state: ClipSelectorState,
) {
    val handleWidth = 16.dp
    val handleWidthPx = with(LocalDensity.current) { handleWidth.toPx() }

    var frameOffsetPx by remember { mutableIntStateOf(-(handleWidthPx / 2).roundToInt()) }
    val scrollOffset by remember { state.scrollOffsetState }

    Box(
        modifier = Modifier
            .clip(
                GenericShape { size, _ ->
                    val baseRect = size.toRect()
                    val adjustedRect = baseRect.copy(left = baseRect.left - handleWidthPx)
                    addRect(adjustedRect)
                },
            )
            .offset { IntOffset(-scrollOffset.roundToInt(), 0) }
            .fillMaxSize(),
    ) {
        // Outer box to increase the touch area of the handle
        Box(
            modifier = Modifier
                .offset { IntOffset((state.startOffset - handleWidthPx * 1.5).roundToInt(), 0) }
                .width(handleWidth * 2)
                .fillMaxHeight()
                .draggable(
                    state = rememberDraggableState { delta ->
                        val newStartOffset = state.startOffset + delta
                        val newClipStart = state.pixelsToDuration(newStartOffset)
                        if (newClipStart >= Duration.ZERO && (clipRange.end - newClipStart) >= 1.seconds) {
                            state.startOffset = newStartOffset
                            onClipStartUpdate(newClipStart)
                        }
                    },
                    orientation = Orientation.Horizontal,
                ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(handleWidth / 2)
                    .width(handleWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = handleWidth / 2, bottomStart = handleWidth / 2))
                    .background(clipColors.selectorColor),
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(this@ClipBox.maxHeight / 2)
                        .clip(RoundedCornerShape(1.dp))
                        .background(clipColors.selectorHandleColor),
                )
            }
        }
        // Outer box to increas the touch area of the handle
        Box(
            modifier = Modifier
                .offset { IntOffset((state.endOffset - handleWidthPx / 2).roundToInt(), 0) }
                .width(handleWidth * 2)
                .fillMaxHeight()
                .draggable(
                    state = rememberDraggableState { delta ->
                        val newEndOffset = state.endOffset + delta
                        val newClipEnd = state.pixelsToDuration(newEndOffset)
                        if (newClipEnd <= episodeDuration && (newClipEnd - clipRange.start) >= 1.seconds) {
                            state.endOffset = newEndOffset
                            onClipEndUpdate(newClipEnd)
                        }
                    },
                    orientation = Orientation.Horizontal,
                ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(handleWidth / 2)
                    .width(handleWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = handleWidth / 2, bottomEnd = handleWidth / 2))
                    .background(clipColors.selectorColor),
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(this@ClipBox.maxHeight / 2)
                        .clip(RoundedCornerShape(1.dp))
                        .background(clipColors.selectorHandleColor),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Frame offset needs to be calculated this way becasue .requiredWidth()
            // centers content inside parent layout if the width is larger than parent.
            // We need to account for that automatic offset.
            val frameWidthPx = state.endOffset - state.startOffset + handleWidthPx
            val maxWidthPx = with(LocalDensity.current) { this@ClipBox.maxWidth.toPx() }
            frameOffsetPx = if (frameWidthPx <= maxWidthPx) {
                (state.startOffset - handleWidthPx / 2).roundToInt()
            } else {
                (state.startOffset - handleWidthPx / 2 + (frameWidthPx - maxWidthPx) / 2).roundToInt()
            }

            val frameWidth = with(LocalDensity.current) { frameWidthPx.toDp() }
            Box(
                modifier = Modifier
                    .offset { IntOffset(frameOffsetPx, 0) }
                    .requiredWidth(frameWidth)
                    .height(6.dp)
                    .background(clipColors.selectorColor),
            )
            Spacer(
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(frameOffsetPx, 0) }
                    .requiredWidth(frameWidth)
                    .height(6.dp)
                    .background(clipColors.selectorColor),
            )
        }
    }
}

@ShowkaseComposable(name = "ClipSelector", group = "Clip", styleName = "Light")
@Preview(name = "ClipSelectorLight", showBackground = true, backgroundColor = 0xFF3E6266)
@Composable
fun ClipSelectorLightPreview() = ClipSelectorPreview(Color(0xFF9BF6FF))

@ShowkaseComposable(name = "ClipSelector", group = "Clip", styleName = "Dark")
@Preview(name = "ClipSelectorDark", showBackground = true, backgroundColor = 0xFF0E1A17)
@Composable
fun ClipSelectorDarkPreview() = ClipSelectorPreview(Color(0xFF152622))

@Composable
private fun ClipSelectorPreview(
    color: Color,
) = Column {
    ClipSelector(
        episodeDuration = 10.minutes,
        clipRange = Clip.Range(0.seconds, 15.seconds),
        isPlaying = false,
        clipColors = ClipColors(color),
        onPlayClick = {},
        onPauseClick = {},
        onClipStartUpdate = {},
        onClipEndUpdate = {},
    )
    Spacer(modifier = Modifier.height(32.dp))
    ClipSelector(
        episodeDuration = 10.minutes,
        clipRange = Clip.Range(0.seconds, 15.seconds),
        isPlaying = true,
        clipColors = ClipColors(color),
        onPlayClick = {},
        onPauseClick = {},
        onClipStartUpdate = {},
        onClipEndUpdate = {},
    )
}
