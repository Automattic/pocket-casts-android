package au.com.shiftyjelly.pocketcasts.transcripts.ui

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.transcript.BookmarkTranscript
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TextSpan
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * Renders a [BookmarkTranscript] scrolled to the bookmarked [passage]. Read-only in the bookmark
 * details, where the passage is drawn in the primary text colour and the rest of the transcript is
 * dimmed. When [editable] the whole transcript is drawn in [editableTextColor] and the passage is
 * marked by an [editableHighlightColor] background; a tap selects the sentence it lands in, a drag
 * extends the passage across sentences, and handles at either end move its start or end a word at a
 * time, reporting the new span through [onPassageChange].
 */
@Composable
fun BookmarkTranscriptView(
    transcript: BookmarkTranscript,
    passage: TextSpan?,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    scrollToPassage: Boolean = true,
    anchorFraction: Float = 0.5f,
    referenceOffset: Int? = null,
    editableTextColor: Color = Color.Unspecified,
    editableHighlightColor: Color = Color.Unspecified,
    handleColor: Color = Color.Unspecified,
    onPassageChange: (TextSpan) -> Unit = {},
) {
    val theme = rememberTranscriptTheme()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var hasScrolled by remember { mutableStateOf(false) }
    var scrolledPassage by remember { mutableStateOf<TextSpan?>(null) }
    var skipFade by remember { mutableStateOf(false) }
    val startTimeMs = remember { SystemClock.elapsedRealtime() }
    val contentAlpha by animateFloatAsState(
        targetValue = if (editable || !scrollToPassage || passage == null || hasScrolled) 1f else 0f,
        animationSpec = if (skipFade) snap() else tween(),
        label = "transcriptFade",
    )
    val currentLayout by rememberUpdatedState(layout)
    val currentPassageChange by rememberUpdatedState(onPassageChange)

    val editableColor = editableTextColor.takeOrElse { theme.primaryText }
    val highlightColor = editableHighlightColor.takeOrElse { theme.highlightText }
    val text = remember(transcript, passage, theme, editable, editableColor, highlightColor) {
        buildAnnotatedString {
            append(transcript.displayText)
            val baseColor = if (editable) editableColor else theme.secondaryText
            addStyle(SpanStyle(color = baseColor), 0, transcript.displayText.length)
            transcript.speakerSpans.forEach { span ->
                addStyle(SpeakerSpanStyle, span.start, span.end)
            }
            passage?.let {
                val style = if (editable) {
                    SpanStyle(background = highlightColor.copy(alpha = 0.24f))
                } else {
                    SpanStyle(color = theme.primaryText)
                }
                addStyle(style, it.start, it.end)
            }
        }
    }

    val selectionDescription = if (passage != null) {
        stringResource(LR.string.bookmark_edit_transcript_selection, transcript.displaySubstring(passage))
    } else {
        stringResource(LR.string.bookmark_edit_transcript_subtitle)
    }

    Column(
        modifier = modifier
            .onSizeChanged { viewportHeight = it.height }
            .alpha(contentAlpha)
            .fadingEdges(top = TopFade, bottom = if (editable) 0.dp else BottomFade)
            .verticalScroll(scrollState),
    ) {
        val renderText: @Composable () -> Unit = {
            Text(
                text = text,
                style = SimpleTextStyle,
                onTextLayout = { layout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (editable) EditContentPadding else ContentPadding)
                    .then(
                        if (editable) {
                            Modifier
                                .semantics {
                                    stateDescription = selectionDescription
                                    liveRegion = LiveRegionMode.Polite
                                }
                                .pointerInput(transcript) {
                                    detectTapGestures { position ->
                                        val result = currentLayout ?: return@detectTapGestures
                                        val offset = result.getOffsetForPosition(position)
                                        if (!transcript.isSpeakerOffset(offset)) {
                                            currentPassageChange(transcript.sentenceDisplaySpan(offset))
                                        }
                                    }
                                }
                                .pointerInput(transcript) {
                                    var anchor: TextSpan? = null
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { position ->
                                            val result = currentLayout ?: return@detectDragGesturesAfterLongPress
                                            val offset = result.getOffsetForPosition(position)
                                            anchor = if (transcript.isSpeakerOffset(offset)) null else transcript.sentenceDisplaySpan(offset)
                                            anchor?.let(currentPassageChange)
                                        },
                                        onDrag = { change, _ ->
                                            val result = currentLayout ?: return@detectDragGesturesAfterLongPress
                                            val start = anchor ?: return@detectDragGesturesAfterLongPress
                                            val focus = transcript.sentenceDisplaySpan(result.getOffsetForPosition(change.position))
                                            currentPassageChange(TextSpan(min(start.start, focus.start), max(start.end, focus.end)))
                                        },
                                    )
                                }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            if (editable) {
                renderText()
            } else {
                SelectionContainer(content = renderText)
            }
            if (referenceOffset != null) {
                val glyphOffset = referenceOffset
                    .coerceIn(0, transcript.displayText.length.coerceAtLeast(1) - 1)
                val glyphBox = layout?.getBoundingBox(glyphOffset)
                if (glyphBox != null) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_bookmark_fill),
                        contentDescription = null,
                        tint = if (editable) editableColor else theme.primaryText,
                        modifier = Modifier
                            .size(GlyphBox)
                            .offset {
                                IntOffset(
                                    x = ((Gutter - GlyphBox) / 2).roundToPx(),
                                    y = (TopFade.toPx() + glyphBox.top + (glyphBox.height - GlyphBox.toPx()) / 2f).roundToInt(),
                                )
                            },
                    )
                }
            }
            val handleLayout = layout
            if (editable && passage != null && !passage.isEmpty && handleLayout != null) {
                val color = handleColor.takeOrElse { theme.highlightText }
                PassageHandle(
                    edge = HandleEdge.Start,
                    layout = handleLayout,
                    passage = passage,
                    color = color,
                    onMove = { index -> currentPassageChange(transcript.movePassageStart(passage, index)) },
                )
                PassageHandle(
                    edge = HandleEdge.End,
                    layout = handleLayout,
                    passage = passage,
                    color = color,
                    onMove = { index -> currentPassageChange(transcript.movePassageEnd(passage, index)) },
                )
            }
        }
    }

    LaunchedEffect(layout, viewportHeight, passage) {
        val result = layout ?: return@LaunchedEffect
        if (!scrollToPassage || passage == null || viewportHeight == 0 || passage == scrolledPassage) return@LaunchedEffect
        if (editable && hasScrolled) return@LaunchedEffect
        val box = result.getBoundingBox(passage.start.coerceIn(0, transcript.displayText.length.coerceAtLeast(1) - 1))
        val topPadding = with(density) { TopFade.toPx() }
        val target = (box.top + topPadding - viewportHeight * anchorFraction + box.height / 2).roundToInt()
        scrollState.scrollTo(target.coerceIn(0, scrollState.maxValue))
        scrolledPassage = passage
        skipFade = SystemClock.elapsedRealtime() - startTimeMs < FadeInThresholdMs
        hasScrolled = true
    }
}

private enum class HandleEdge { Start, End }

@Composable
private fun PassageHandle(
    edge: HandleEdge,
    layout: TextLayoutResult,
    passage: TextSpan,
    color: Color,
    onMove: (Int) -> Unit,
) {
    val box = when (edge) {
        HandleEdge.Start -> layout.getBoundingBox(passage.start)
        HandleEdge.End -> layout.getBoundingBox(passage.end - 1)
    }
    val density = LocalDensity.current
    val knobPx = with(density) { HandleKnob.toPx() }
    val touchPx = with(density) { HandleTouchWidth.toPx() }
    val origin = Offset(
        x = (if (edge == HandleEdge.Start) box.left else box.right) - touchPx / 2,
        y = if (edge == HandleEdge.Start) box.top - knobPx else box.top,
    )
    val currentOrigin by rememberUpdatedState(origin)
    val currentLayout by rememberUpdatedState(layout)
    val currentOnMove by rememberUpdatedState(onMove)
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (Gutter.toPx() + origin.x).roundToInt(),
                    y = (TopFade.toPx() + origin.y).roundToInt(),
                )
            }
            .size(width = HandleTouchWidth, height = with(density) { (box.height + knobPx).toDp() })
            .systemGestureExclusion()
            .pointerInput(edge) {
                detectDragGestures { change, _ ->
                    change.consume()
                    currentOnMove(currentLayout.getOffsetForPosition(currentOrigin + change.position))
                }
            }
            .drawBehind {
                val stemX = size.width / 2
                val stemTop = if (edge == HandleEdge.Start) knobPx / 2 else 0f
                val stemBottom = if (edge == HandleEdge.Start) size.height else box.height
                drawLine(color, Offset(stemX, stemTop), Offset(stemX, stemBottom), strokeWidth = HandleStem.toPx())
                val knobY = if (edge == HandleEdge.Start) knobPx / 2 else size.height - knobPx / 2
                drawCircle(color, radius = knobPx / 2, center = Offset(stemX, knobY))
            },
    )
}

private fun BookmarkTranscript.isSpeakerOffset(index: Int) = speakerSpans.any { index in it.start until it.end }

private val SimpleTextStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 16.sp * 1.5f,
    fontWeight = FontWeight.Medium,
    fontFamily = TranscriptTheme.RobotoSerifFontFamily,
)

private val SpeakerSpanStyle = SpanStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
)

private val Gutter = 28.dp
private val HandleKnob = 12.dp
private val HandleStem = 2.dp
private val HandleTouchWidth = 48.dp
private val GlyphBox = 24.dp

private val TopFade = 48.dp
private val BottomFade = 64.dp

private val ContentPadding = PaddingValues(start = Gutter, end = Gutter, top = TopFade, bottom = BottomFade)
private val EditContentPadding = PaddingValues(start = Gutter, end = Gutter, top = TopFade, bottom = 0.dp)

private val FadeInThresholdMs = 200L

private fun Modifier.fadingEdges(top: Dp, bottom: Dp) = if (top == 0.dp && bottom == 0.dp) {
    this
} else {
    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val scale = min(1f, size.height / (top.toPx() + bottom.toPx()))
            val topFade = top.toPx() * scale
            val bottomFade = bottom.toPx() * scale
            if (topFade > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = topFade,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (bottomFade > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = size.height - bottomFade,
                        endY = size.height,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
}

@Preview
@Composable
private fun BookmarkTranscriptViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    val transcript = remember { BookmarkTranscript.from(Transcript.TextPreview) }
    val passage = remember { transcript.sentenceDisplaySpan(index = 40) }
    AppThemeWithBackground(themeType) {
        BookmarkTranscriptView(
            transcript = transcript,
            passage = passage,
            scrollToPassage = false,
            referenceOffset = passage.start,
            modifier = Modifier.background(rememberTranscriptTheme().background),
        )
    }
}

@Preview
@Composable
private fun BookmarkTranscriptViewEditablePreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    val transcript = remember { BookmarkTranscript.from(Transcript.TextPreview) }
    val passage = remember { transcript.sentenceDisplaySpan(index = 40) }
    AppTheme(ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            val playerColors = requireNotNull(MaterialTheme.theme.rememberPlayerColors())
            BookmarkTranscriptView(
                transcript = transcript,
                passage = passage,
                editable = true,
                scrollToPassage = false,
                referenceOffset = passage.start,
                editableTextColor = playerColors.contrast01,
                editableHighlightColor = playerColors.highlight01,
                modifier = Modifier.background(playerColors.background01),
            )
        }
    }
}
