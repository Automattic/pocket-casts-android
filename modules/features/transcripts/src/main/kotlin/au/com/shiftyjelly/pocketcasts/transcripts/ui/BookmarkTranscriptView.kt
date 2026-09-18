package au.com.shiftyjelly.pocketcasts.transcripts.ui

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.transcript.BookmarkTranscript
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TextSpan
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR

/**
 * Renders a [BookmarkTranscript] with the bookmarked [passage] highlighted in the primary text
 * colour and the surrounding transcript dimmed, scrolling the passage into view. Read-only in the
 * bookmark details; when [editable] a tap selects the sentence it lands in and a drag extends the
 * passage across sentences, reporting the new span through [onPassageChange].
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

    val text = remember(transcript, passage, theme, editable) {
        buildAnnotatedString {
            append(transcript.displayText)
            addStyle(SpanStyle(color = theme.secondaryText), 0, transcript.displayText.length)
            transcript.speakerSpans.forEach { span ->
                addStyle(SpeakerSpanStyle, span.start, span.end)
            }
            passage?.let {
                val style = if (editable) {
                    SpanStyle(color = theme.primaryText, background = theme.highlightText.copy(alpha = 0.24f))
                } else {
                    SpanStyle(color = theme.primaryText)
                }
                addStyle(style, it.start, it.end)
            }
        }
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
                    .padding(ContentPadding)
                    .then(
                        if (editable) {
                            Modifier
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
                        tint = theme.primaryText,
                        modifier = Modifier
                            .size(GlyphSize)
                            .offset {
                                IntOffset(
                                    x = ((Gutter - GlyphSize) / 2).roundToPx(),
                                    y = (ContentPadding.calculateTopPadding().toPx() + glyphBox.top + (glyphBox.height - GlyphSize.toPx()) / 2f).roundToInt(),
                                )
                            },
                    )
                }
            }
        }
    }

    LaunchedEffect(layout, viewportHeight, passage) {
        val result = layout ?: return@LaunchedEffect
        if (!scrollToPassage || passage == null || viewportHeight == 0 || passage == scrolledPassage) return@LaunchedEffect
        if (editable && hasScrolled) return@LaunchedEffect
        val box = result.getBoundingBox(passage.start.coerceIn(0, transcript.displayText.length.coerceAtLeast(1) - 1))
        val topPadding = with(density) { ContentPadding.calculateTopPadding().toPx() }
        val target = (box.top + topPadding - viewportHeight * anchorFraction + box.height / 2).roundToInt()
        scrollState.scrollTo(target.coerceIn(0, scrollState.maxValue))
        scrolledPassage = passage
        skipFade = SystemClock.elapsedRealtime() - startTimeMs < FadeInThresholdMs
        hasScrolled = true
    }
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
private val GlyphSize = 16.dp

private val ContentPadding = PaddingValues(start = Gutter, end = Gutter, top = 48.dp, bottom = 64.dp)

private val TopFade = 48.dp
private val BottomFade = 64.dp

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
