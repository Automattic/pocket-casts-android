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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlin.time.DurationUnit
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ClipSelector(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    clipColors: ClipColors,
    listener: ShareClipPageListener,
    modifier: Modifier = Modifier,
    state: ClipSelectorState = rememberClipSelectorState(firstVisibleItemIndex = 0),
) {
    val density = LocalDensity.current
    LaunchedEffect(state.scale) {
        state.refreshItemWidth(density)
        state.scaleBoxOffsets(clipRange)
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(clipColors.timeline, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
    ) {
        Image(
            painter = painterResource(if (isPlaying) IR.drawable.ic_widget_pause else IR.drawable.ic_widget_play),
            contentDescription = stringResource(if (isPlaying) LR.string.pause else LR.string.play),
            colorFilter = ColorFilter.tint(clipColors.playPauseButton),
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .clickable(
                    interactionSource = remember(::MutableInteractionSource),
                    indication = rememberRipple(color = clipColors.base),
                    onClickLabel = stringResource(if (isPlaying) LR.string.pause else LR.string.play),
                    role = Role.Button,
                    onClick = if (isPlaying) listener::onClickPause else listener::onClickPlay,
                )
                .padding(16.dp),
        )
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        ClipSelector(
            episodeDuration = episodeDuration,
            clipRange = clipRange,
            playbackProgress = playbackProgress,
            clipColors = clipColors,
            listener = listener,
            state = state,
        )
    }
}

@Composable
private fun ClipSelector(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    clipColors: ClipColors,
    state: ClipSelectorState,
    listener: ShareClipPageListener,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        ClipTimeline(
            episodeDuration = episodeDuration,
            clipRange = clipRange,
            clipColors = clipColors,
            state = state,
            listener = listener,
        )
        ClipBox(
            episodeDuration = episodeDuration,
            clipRange = clipRange,
            playbackProgress = playbackProgress,
            clipColors = clipColors,
            state = state,
            listener = listener,
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.ClipTimeline(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    clipColors: ClipColors,
    state: ClipSelectorState,
    listener: ShareClipPageListener,
) {
    val largeTickHeight = maxHeight / 3
    val mediumTickHeight = maxHeight / 6
    val smallTickHeight = maxHeight / 12

    // Some episode are very short (couple of minutes or even dozen of seconds).
    // For those episodes we want to disable scaling timeline beyond a point
    // where ticks are too dense and an episode fits more than one in a timeline.
    val maxSecondsPerTick by remember(episodeDuration, maxWidth) {
        mutableIntStateOf(state.calculateMaxSecondsPerTick(episodeDuration, maxWidth))
    }

    val transformation = rememberTransformableState { zoom, _, _ ->
        state.updateTimelineScale(zoom, maxSecondsPerTick, listener::onUpdateTimeline)
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(state.spaceWidthDp),
        verticalAlignment = Alignment.CenterVertically,
        state = state.listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 6.dp)
            .background(clipColors.background)
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
                                listener.onUpdateClipStart(newClipStart)
                            }
                        } else {
                            val newClipEnd = state.pixelsToDuration(offsetX)
                            if (newClipEnd <= episodeDuration && (newClipEnd - clipRange.start) >= 1.seconds) {
                                state.endOffset = offsetX
                                listener.onUpdateClipEnd(newClipEnd)
                            }
                        }
                    },
                )
            },
    ) {
        items(
            count = episodeDuration.inWholeSeconds.toInt().ceilDiv(state.secondsPerTick) + 1,
            key = { index -> index },
        ) { index ->
            val heightIndex = when (index % 10) {
                0 -> largeTickHeight
                5 -> mediumTickHeight
                else -> smallTickHeight
            }
            Box(
                modifier = Modifier
                    .width(state.tickWidthDp)
                    .height(heightIndex)
                    .background(clipColors.timelineTick),
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.ClipBox(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    clipColors: ClipColors,
    listener: ShareClipPageListener,
    state: ClipSelectorState,
) {
    val handleWidth = 16.dp
    val handleWidthPx = with(LocalDensity.current) { handleWidth.toPx() }

    var frameOffsetPx by remember { mutableIntStateOf(-(handleWidthPx / 2).roundToInt()) }
    val scrollOffset by remember { state.scrollOffsetState }
    var progressOffsetPx by remember(playbackProgress, state.itemWidth) {
        mutableFloatStateOf(state.durationToPixels(playbackProgress, accuracy = DurationUnit.MILLISECONDS))
    }

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
        // Outer box to increase the touch area of the progress bar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = -handleWidth + 1.dp)
                .offset { IntOffset((progressOffsetPx + state.startOffset).roundToInt(), 0) }
                .width(handleWidth * 2)
                .fillMaxHeight()
                .draggable(
                    state = rememberDraggableState { delta ->
                        val newOffset = progressOffsetPx + delta
                        val newProgress = state.pixelsToDuration(newOffset)
                        if ((clipRange.start + newProgress) in clipRange) {
                            listener.onUpdateClipProgress(state.pixelsToDuration(newOffset))
                            progressOffsetPx = newOffset
                        }
                    },
                    orientation = Orientation.Horizontal,
                ),
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(clipColors.timelineProgress),
            )
        }

        val startDescription = pluralStringResource(
            id = LR.plurals.podcast_share_start_handle_description,
            count = clipRange.startInSeconds,
            clipRange.startInSeconds,
        )
        // Outer box to increase the touch area of the handle
        Box(
            modifier = Modifier
                .offset { IntOffset((state.startOffset - handleWidthPx * 2).roundToInt(), 0) }
                .width(handleWidth * 2)
                .fillMaxHeight()
                .draggable(
                    state = rememberDraggableState { delta ->
                        val newStartOffset = state.startOffset + delta
                        val newClipStart = state.pixelsToDuration(newStartOffset)
                        if (newClipStart >= Duration.ZERO && (clipRange.end - newClipStart) >= 1.seconds) {
                            listener.onUpdateClipStart(newClipStart)
                            state.startOffset = newStartOffset
                            progressOffsetPx = 0f
                        }
                    },
                    orientation = Orientation.Horizontal,
                )
                .semantics { contentDescription = startDescription },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(handleWidth)
                    .width(handleWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = handleWidth / 2, bottomStart = handleWidth / 2))
                    .background(clipColors.selector),
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(this@ClipBox.maxHeight / 2)
                        .clip(RoundedCornerShape(1.dp))
                        .background(clipColors.selectorHandle),
                )
            }
        }
        val endDescription = pluralStringResource(
            id = LR.plurals.podcast_share_end_handle_description,
            count = clipRange.endInSeconds,
            clipRange.endInSeconds,
        )
        // Outer box to increas the touch area of the handle
        Box(
            modifier = Modifier
                .offset(x = state.tickWidthDp)
                .offset { IntOffset(state.endOffset.roundToInt(), 0) }
                .width(handleWidth * 2)
                .fillMaxHeight()
                .draggable(
                    state = rememberDraggableState { delta ->
                        val newEndOffset = state.endOffset + delta
                        val newClipEnd = state.pixelsToDuration(newEndOffset)
                        if (newClipEnd <= episodeDuration && (newClipEnd - clipRange.start) >= 1.seconds) {
                            listener.onUpdateClipEnd(newClipEnd)
                            state.endOffset = newEndOffset
                            progressOffsetPx = 0f
                        }
                    },
                    orientation = Orientation.Horizontal,
                )
                .semantics { contentDescription = endDescription },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(handleWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = handleWidth / 2, bottomEnd = handleWidth / 2))
                    .background(clipColors.selector),
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(this@ClipBox.maxHeight / 2)
                        .clip(RoundedCornerShape(1.dp))
                        .background(clipColors.selectorHandle),
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
                    .background(clipColors.selector),
            )
            Spacer(
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(frameOffsetPx, 0) }
                    .requiredWidth(frameWidth)
                    .height(6.dp)
                    .background(clipColors.selector),
            )
        }
    }
}

@ShowkaseComposable(name = "ClipSelectorPaused", group = "Clip")
@Preview(name = "Paused", device = PreviewDevicePortrait)
@Composable
fun ClipSelectorPausedPreview() = ClipSelectorPreview()

@ShowkaseComposable(name = "ClipSelectorPlaying", group = "Clip")
@Preview(name = "Playing", device = PreviewDevicePortrait)
@Composable
fun ClipSelectorPlayingPreview() = ClipSelectorPreview(isPlaying = true)

@Preview(name = "Zoomed in", device = PreviewDevicePortrait)
@Composable
private fun ClipSelectorZoomedPreview() = ClipSelectorPreview(
    clipEnd = 10.seconds,
    scale = 5f,
)

@Preview(name = "Scrolled", device = PreviewDevicePortrait)
@Composable
private fun ClipSelectorScrolledPreview() = ClipSelectorPreview(
    clipStart = 35.seconds,
    clipEnd = 55.seconds,
    firstVisibleItemIndex = 25,
)

@Preview(name = "No start handle", device = PreviewDevicePortrait)
@Composable
private fun ClipSelectorNoStartHandlePreview() = ClipSelectorPreview(
    firstVisibleItemIndex = 5,
)

@Preview(name = "No end handle", device = PreviewDevicePortrait)
@Composable
private fun ClipSelectorNoEndHandlePreview() = ClipSelectorPreview(
    clipStart = 35.seconds,
    clipEnd = 75.seconds,
)

@Preview(name = "Playback in middle", device = PreviewDevicePortrait)
@Composable
private fun ClipSelectorInProgressPreview() = ClipSelectorPreview(
    progressPlayback = 10.seconds,
)

@Preview(name = "Playback at end", device = PreviewDevicePortrait)
@Composable
private fun ClipSelectorPlayedPreview() = ClipSelectorPreview(
    progressPlayback = 15.seconds,
)

@Composable
private fun ClipSelectorPreview(
    clipStart: Duration = 0.seconds,
    clipEnd: Duration = 15.seconds,
    progressPlayback: Duration = 0.seconds,
    isPlaying: Boolean = false,
    firstVisibleItemIndex: Int = 0,
    scale: Float = 1f,
) {
    val clipColors = ClipColors(Color(0xFFEC0404))
    Box(
        modifier = Modifier.background(clipColors.background),
    ) {
        ClipSelector(
            episodeDuration = 5.minutes,
            clipRange = Clip.Range(clipStart, clipEnd),
            playbackProgress = progressPlayback,
            isPlaying = isPlaying,
            clipColors = clipColors,
            listener = ShareClipPageListener.Preview,
            state = rememberClipSelectorState(
                firstVisibleItemIndex = firstVisibleItemIndex,
                scale = scale,
            ),
        )
    }
}
