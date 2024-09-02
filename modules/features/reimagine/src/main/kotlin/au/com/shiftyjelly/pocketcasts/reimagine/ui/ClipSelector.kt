package au.com.shiftyjelly.pocketcasts.reimagine.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.reimagine.clip.ShareClipPageListener
import au.com.shiftyjelly.pocketcasts.sharing.Clip
import au.com.shiftyjelly.pocketcasts.utils.extensions.ceilDiv
import au.com.shiftyjelly.pocketcasts.utils.toHhMmSs
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ClipSelector(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    shareColors: ShareColors,
    useKeyboardInput: Boolean,
    listener: ShareClipPageListener,
    modifier: Modifier = Modifier,
    state: ClipSelectorState = rememberClipSelectorState(firstVisibleItemIndex = 0),
) {
    if (useKeyboardInput) {
        KeyboardClipSelector(
            episodeDuration = episodeDuration,
            clipRange = clipRange,
            isPlaying = isPlaying,
            shareColors = shareColors,
            listener = listener,
        )
    } else {
        TouchClipSelector(
            episodeDuration = episodeDuration,
            clipRange = clipRange,
            playbackProgress = playbackProgress,
            isPlaying = isPlaying,
            shareColors = shareColors,
            listener = listener,
            modifier = modifier,
            state = state,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TouchClipSelector(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    shareColors: ShareColors,
    listener: ShareClipPageListener,
    modifier: Modifier = Modifier,
    state: ClipSelectorState,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    LaunchedEffect(state.scale) {
        state.refreshItemWidth(density)
        state.scaleBoxOffsets(clipRange)
    }
    Column(
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(shareColors.container, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
        ) {
            Image(
                painter = painterResource(if (isPlaying) IR.drawable.ic_widget_pause else IR.drawable.ic_widget_play),
                contentDescription = stringResource(if (isPlaying) LR.string.pause else LR.string.play),
                colorFilter = ColorFilter.tint(shareColors.onContainerPrimary),
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .clickable(
                        interactionSource = remember(::MutableInteractionSource),
                        indication = rememberRipple(color = shareColors.accent),
                        onClickLabel = stringResource(if (isPlaying) LR.string.pause else LR.string.play),
                        role = Role.Button,
                        onClick = if (isPlaying) listener::onClickPause else listener::onClickPlay,
                    )
                    .padding(16.dp),
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            ClipBox(
                episodeDuration = episodeDuration,
                clipRange = clipRange,
                playbackProgress = playbackProgress,
                shareColors = shareColors,
                listener = listener,
                state = state,
            )
        }
        Spacer(
            modifier = Modifier.height(12.dp),
        )
        Row {
            TextH70(
                text = stringResource(LR.string.share_clip_start_position, clipRange.start.toHhMmSs()),
                color = shareColors.onBackgroundSecondary,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { coroutineScope.launch { state.scrollTo(clipRange.start) } },
                ),
            )
            Spacer(
                modifier = Modifier.weight(1f),
            )
            TextH70(
                text = stringResource(LR.string.share_clip_duration, clipRange.duration.toHhMmSs()),
                color = shareColors.onBackgroundSecondary,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { coroutineScope.launch { state.scrollTo(clipRange.end) } },
                ),
            )
        }
    }
}

@Composable
private fun ClipBox(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    shareColors: ShareColors,
    state: ClipSelectorState,
    listener: ShareClipPageListener,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        ClipTimeline(
            episodeDuration = episodeDuration,
            clipRange = clipRange,
            shareColors = shareColors,
            state = state,
            listener = listener,
        )
        ClipWindow(
            episodeDuration = episodeDuration,
            clipRange = clipRange,
            playbackProgress = playbackProgress,
            shareColors = shareColors,
            state = state,
            listener = listener,
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.ClipTimeline(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    shareColors: ShareColors,
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
            .background(shareColors.background)
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
        val tickCount = episodeDuration.inWholeSeconds.toInt().ceilDiv(state.secondsPerTick)
        items(
            count = tickCount,
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
                    .background(shareColors.onBackgroundSecondary),
            )
            if (index == tickCount - 1) {
                Spacer(modifier = Modifier.width(32.dp))
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.ClipWindow(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    shareColors: ShareColors,
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
                    .background(shareColors.onBackgroundPrimary),
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
                    .background(shareColors.accent),
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(this@ClipWindow.maxHeight / 2)
                        .clip(RoundedCornerShape(1.dp))
                        .background(shareColors.onAccent.copy(alpha = 0.6f)),
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
                    .background(shareColors.accent),
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(this@ClipWindow.maxHeight / 2)
                        .clip(RoundedCornerShape(1.dp))
                        .background(shareColors.onAccent.copy(alpha = 0.6f)),
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
            val maxWidthPx = with(LocalDensity.current) { this@ClipWindow.maxWidth.toPx() }
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
                    .background(shareColors.accent),
            )
            Spacer(
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(frameOffsetPx, 0) }
                    .requiredWidth(frameWidth)
                    .height(6.dp)
                    .background(shareColors.accent),
            )
        }
    }
}

@Composable
private fun KeyboardClipSelector(
    episodeDuration: Duration,
    clipRange: Clip.Range,
    isPlaying: Boolean,
    shareColors: ShareColors,
    listener: ShareClipPageListener,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.wrapContentSize(),
    ) {
        Box(
            modifier = Modifier
                .background(shareColors.container, RoundedCornerShape(8.dp))
                .width(72.dp)
                .height(72.dp),
        ) {
            Image(
                painter = painterResource(if (isPlaying) IR.drawable.ic_widget_pause else IR.drawable.ic_widget_play),
                contentDescription = stringResource(if (isPlaying) LR.string.pause else LR.string.play),
                colorFilter = ColorFilter.tint(shareColors.onContainerPrimary),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember(::MutableInteractionSource),
                        indication = rememberRipple(color = shareColors.accent),
                        onClickLabel = stringResource(if (isPlaying) LR.string.pause else LR.string.play),
                        role = Role.Button,
                        onClick = if (isPlaying) listener::onClickPause else listener::onClickPlay,
                    )
                    .padding(16.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(shareColors.container, RoundedCornerShape(8.dp))
                .weight(1f)
                .heightIn(min = 72.dp)
                .padding(12.dp),
        ) {
            TextH50(
                text = stringResource(LR.string.share_start_position),
                color = shareColors.onContainerSecondary,
            )
            Spacer(
                modifier = Modifier.height(2.dp),
            )
            HhMmSsTextInput(
                value = clipRange.start,
                episodeDuration = episodeDuration,
                shareColors = shareColors,
                hoursDescription = stringResource(LR.string.share_start_hours),
                minutesDescription = stringResource(LR.string.share_start_minutes),
                secondsDescription = stringResource(LR.string.share_start_seconds),
                onValueChanged = listener::onUpdateClipStart,
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(shareColors.container, RoundedCornerShape(8.dp))
                .weight(1f)
                .heightIn(min = 72.dp)
                .padding(12.dp),
        ) {
            TextH50(
                text = stringResource(LR.string.share_end_position),
                color = shareColors.onContainerSecondary,
            )
            Spacer(
                modifier = Modifier.height(2.dp),
            )
            HhMmSsTextInput(
                value = clipRange.end,
                episodeDuration = episodeDuration,
                shareColors = shareColors,
                hoursDescription = stringResource(LR.string.share_end_hours),
                minutesDescription = stringResource(LR.string.share_end_minutes),
                secondsDescription = stringResource(LR.string.share_end_seconds),
                onValueChanged = listener::onUpdateClipEnd,
            )
        }
    }
}

@Composable
private fun HhMmSsTextInput(
    value: Duration,
    episodeDuration: Duration,
    shareColors: ShareColors,
    hoursDescription: String,
    minutesDescription: String,
    secondsDescription: String,
    onValueChanged: (Duration) -> Unit,
) {
    var hours by remember { mutableStateOf(value.inWholeHours.hours) }
    var minutes by remember { mutableStateOf((value.inWholeMinutes % 60).minutes) }
    var seconds by remember { mutableStateOf((value.inWholeSeconds % 60).seconds) }

    val maxHours = episodeDuration.inWholeHours
    val maxMinutes = if (maxHours == 0L) episodeDuration.inWholeMinutes else 59
    val maxSeconds = if (maxMinutes == 0L) episodeDuration.inWholeSeconds else 59

    Row {
        if (maxHours > 0) {
            TimeTextField(
                value = value,
                formatter = DurationFormatter(
                    maxComponentValue = maxHours,
                    durationToComponenet = { it.inWholeHours },
                    componentToDuration = { it.hours },
                ),
                showSeparator = true,
                shareColors = shareColors,
                contentDescription = hoursDescription,
                onValueChanged = {
                    hours = it
                    onValueChanged(hours + minutes + seconds)
                },
            )
        }
        if (maxMinutes > 0) {
            TimeTextField(
                value = value,
                formatter = DurationFormatter(
                    maxComponentValue = maxMinutes,
                    durationToComponenet = { it.inWholeMinutes % 60 },
                    componentToDuration = { it.minutes },
                ),
                showSeparator = true,
                shareColors = shareColors,
                contentDescription = minutesDescription,
                onValueChanged = {
                    minutes = it
                    onValueChanged(hours + minutes + seconds)
                },
            )
        }
        if (maxSeconds > 0) {
            TimeTextField(
                value = value,
                formatter = DurationFormatter(
                    maxComponentValue = maxSeconds,
                    durationToComponenet = { it.inWholeSeconds % 60 },
                    componentToDuration = { it.seconds },
                ),
                showSeparator = false,
                shareColors = shareColors,
                contentDescription = secondsDescription,
                onValueChanged = {
                    seconds = it
                    onValueChanged(hours + minutes + seconds)
                },
            )
        }
    }
}

@Composable
private fun TimeTextField(
    value: Duration,
    formatter: DurationFormatter,
    showSeparator: Boolean,
    shareColors: ShareColors,
    contentDescription: String,
    onValueChanged: (Duration) -> Unit,
) {
    var displyedValue by remember { mutableStateOf(TextFieldValue(formatter.durationToComponenet(value).toString())) }
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = displyedValue,
        onValueChange = { textFieldValue ->
            val newValue = textFieldValue.text.replace("""[^0-9]+""".toRegex(), "").trimStart('0')
            val longValue = newValue.toLongOrNull()?.takeIf { it <= formatter.maxComponentValue }
            if (newValue.isEmpty() || longValue != null) {
                displyedValue = textFieldValue.copy(text = newValue)
                onValueChanged(longValue?.let { formatter.componentToDuration(it) } ?: Duration.ZERO)
            }
        },
        maxLines = 1,
        textStyle = TextStyle(
            color = shareColors.onContainerPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.W500,
        ),
        cursorBrush = SolidColor(shareColors.onContainerPrimary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
        ),
        visualTransformation = HHMmSsVisualTransformation(showSeparator),
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .onFocusEvent { event ->
                if (event.isFocused) {
                    displyedValue = displyedValue.copy(selection = TextRange(Int.MAX_VALUE))
                }
            }
            .semantics { this.contentDescription = contentDescription },
    )
}

private class DurationFormatter(
    val maxComponentValue: Long,
    val durationToComponenet: (Duration) -> Long,
    val componentToDuration: (Long) -> Duration,
)

private class HHMmSsVisualTransformation(
    private val showSeparator: Boolean,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val formattedText = buildString {
            append(originalText.padStart(2, ':'))
            if (showSeparator) {
                append(':')
            }
        }
        val displayedText = buildString {
            append(originalText.padStart(2, '0'))
            if (showSeparator) {
                append(':')
            }
        }

        return TransformedText(
            AnnotatedString(displayedText),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return originalText.length.coerceAtLeast(2) - originalText.length + offset
                }

                override fun transformedToOriginal(offset: Int): Int {
                    val precedingSeparatorCount = formattedText
                        .mapIndexedNotNull { index, c -> index.takeIf { c == ':' } }
                        .count { separatorIndex -> separatorIndex < offset }
                    return offset - precedingSeparatorCount
                }
            },
        )
    }
}

@ShowkaseComposable(name = "Clip selector", group = "Sharing")
@Preview(name = "Paused", device = Devices.PortraitRegular)
@Composable
fun ClipSelectorPausedPreview() = ClipSelectorPreview()

@Preview(name = "Playing", device = Devices.PortraitRegular)
@Composable
private fun ClipSelectorPlayingPreview() = ClipSelectorPreview(isPlaying = true)

@Preview(name = "Zoomed in", device = Devices.PortraitRegular)
@Composable
private fun ClipSelectorZoomedPreview() = ClipSelectorPreview(
    clipEnd = 10.seconds,
    scale = 5f,
)

@Preview(name = "Scrolled", device = Devices.PortraitRegular)
@Composable
private fun ClipSelectorScrolledPreview() = ClipSelectorPreview(
    clipStart = 35.seconds,
    clipEnd = 55.seconds,
    firstVisibleItemIndex = 25,
)

@Preview(name = "No start handle", device = Devices.PortraitRegular)
@Composable
private fun ClipSelectorNoStartHandlePreview() = ClipSelectorPreview(
    firstVisibleItemIndex = 5,
)

@Preview(name = "No end handle", device = Devices.PortraitRegular)
@Composable
private fun ClipSelectorNoEndHandlePreview() = ClipSelectorPreview(
    clipStart = 35.seconds,
    clipEnd = 75.seconds,
)

@Preview(name = "Playback in middle", device = Devices.PortraitRegular)
@Composable
private fun ClipSelectorInProgressPreview() = ClipSelectorPreview(
    progressPlayback = 10.seconds,
)

@Preview(name = "Playback at end", device = Devices.PortraitRegular)
@Composable
private fun ClipSelectorPlayedPreview() = ClipSelectorPreview(
    progressPlayback = 15.seconds,
)

@Preview(name = "Keyboard", device = Devices.PortraitRegular)
@Composable
private fun ClipSelectorKeyboardPreview() = ClipSelectorPreview(
    useKeyboardInput = true,
)

@Composable
private fun ClipSelectorPreview(
    clipStart: Duration = 0.seconds,
    clipEnd: Duration = 15.seconds,
    progressPlayback: Duration = 0.seconds,
    isPlaying: Boolean = false,
    firstVisibleItemIndex: Int = 0,
    scale: Float = 1f,
    useKeyboardInput: Boolean = false,
) {
    val shareColors = ShareColors(Color(0xFFEC0404))
    Box(
        modifier = Modifier.background(shareColors.background),
    ) {
        ClipSelector(
            episodeDuration = 5.minutes,
            clipRange = Clip.Range(clipStart, clipEnd),
            playbackProgress = progressPlayback,
            isPlaying = isPlaying,
            shareColors = shareColors,
            listener = ShareClipPageListener.Preview,
            useKeyboardInput = useKeyboardInput,
            state = rememberClipSelectorState(
                firstVisibleItemIndex = firstVisibleItemIndex,
                scale = scale,
            ),
        )
    }
}
