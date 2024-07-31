package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTimingSubtitle
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.FadeDirection
import au.com.shiftyjelly.pocketcasts.compose.extensions.gradientBackground
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptError
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TranscriptPage(
    viewModel: TranscriptViewModel,
    theme: Theme,
    scrollState: ScrollState,
    modifier: Modifier,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    when (state.value) {
        is UiState.Empty -> Unit

        is UiState.TranscriptFound -> {
            val transcriptFoundState = state.value as UiState.TranscriptFound
            val colors = DefaultColors(theme, transcriptFoundState.podcastAndEpisode?.podcast)
            LoadingView(Modifier.background(colors.backgroundColor()))
        }

        is UiState.TranscriptLoaded -> {
            val loadedState = state.value as UiState.TranscriptLoaded
            TranscriptContent(
                state = loadedState,
                colors = DefaultColors(theme, loadedState.podcastAndEpisode?.podcast),
                scrollState = scrollState,
                modifier = modifier,
            )
        }

        is UiState.Error -> {
            val errorState = state.value as UiState.Error
            TranscriptError(
                state = errorState,
                colors = DefaultColors(theme, errorState.podcastAndEpisode?.podcast),
                scrollState = scrollState,
                modifier = modifier,
            )
        }
    }

    LaunchedEffect(state.value.transcript) {
        viewModel.parseAndLoadTranscript()
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun TranscriptContent(
    state: UiState.TranscriptLoaded,
    colors: DefaultColors,
    scrollState: ScrollState,
    modifier: Modifier,
) {
    val defaultTextStyle = SpanStyle(fontSize = 16.sp, color = colors.textColor())
    val highlightedTextStyle = SpanStyle(fontSize = 18.sp, color = Color.White)
    val configuration = LocalConfiguration.current
    val bottomPadding = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.dp else 125.dp
    var highlightedText: CharSequence? by remember { mutableStateOf(null) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val annotatedString = buildAnnotatedString {
        withStyle(style = ParagraphStyle(lineHeight = 30.sp)) {
            with(state.cuesWithTimingSubtitle) {
                /* Blank lines are appended to add content padding */
                append("\n")
                append("\n")
                (0 until eventTimeCount).forEach { index ->
                    getCues(getEventTime(index)).forEach { cue ->
                        if (shouldHighlightCueAtIndex(index, state.playbackPosition)) {
                            highlightedText = cue.text
                            withStyle(style = highlightedTextStyle) { append(cue.text) }
                        } else {
                            withStyle(style = defaultTextStyle) { append(cue.text) }
                        }
                        append(" ")
                    }
                }
                append("\n")
                append("\n")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundColor())
            .onGloballyPositioned { contentSize = it.size },
    ) {
        Text(
            annotatedString,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomPadding)
                .verticalScroll(scrollState),
            onTextLayout = { textLayoutResult = it },
        )

        GradientView(
            baseColor = colors.backgroundColor(),
            modifier = Modifier
                .align(Alignment.TopCenter),
            fadeDirection = FadeDirection.TopToBottom,
        )

        GradientView(
            baseColor = colors.backgroundColor(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding),
            fadeDirection = FadeDirection.BottomToTop,
        )
    }

    LaunchedEffect(state.playbackPosition) {
        coroutineScope.launch {
            textLayoutResult?.findHighlightedTextLineOffset(annotatedString, highlightedText)
                ?.let { lineOffset ->
                    scrollToVisibleRange(contentSize, lineOffset, scrollState)
                }
        }
    }
}

@Composable
private fun GradientView(
    baseColor: Color,
    modifier: Modifier = Modifier,
    fadeDirection: FadeDirection,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height((screenHeight * 0.1).dp)
            .gradientBackground(
                baseColor = baseColor,
                colorStops = listOf(
                    Color.Black,
                    Color.Transparent,
                ),
                direction = fadeDirection,
            ),
    )
}

private suspend fun scrollToVisibleRange(
    contentSize: IntSize,
    lineOffset: Float,
    scrollState: ScrollState,
) {
    val visibleRect = Rect(0f, 0f, contentSize.width.toFloat(), (contentSize.height * .9).toFloat())
    val lineRect = Rect(0f, (lineOffset - scrollState.value), contentSize.width.toFloat(), (lineOffset - scrollState.value) + 10)

    if (visibleRect.intersect(lineRect).isEmpty) {
        // Calculate an offset to adjust the scrolling position,
        // ensuring that the highlighted text line is brought into a visible portion of the screen.
        // If the line is above the current view, it scrolls down slightly (10% of content height).
        // If the line is below, it scrolls up slightly (80% of content height).
        val contentOffset = (if (lineOffset < scrollState.value) .1 else .8) * contentSize.height
        scrollState.animateScrollTo((lineOffset - contentOffset).toInt())
    }
}

@OptIn(UnstableApi::class)
private fun CuesWithTimingSubtitle.shouldHighlightCueAtIndex(
    index: Int,
    playbackPosition: Duration,
) = playbackPosition.inWholeMicroseconds in
    getEventTime(index)..<getEventTime((index + 1).coerceAtMost(eventTimeCount - 1))

private fun TextLayoutResult.findHighlightedTextLineOffset(
    annotatedString: AnnotatedString,
    highlightedText: CharSequence?,
) = multiParagraph.getLineTop(
    multiParagraph.getLineForOffset(annotatedString.indexOf(highlightedText.toString())),
)

@Composable
private fun TranscriptError(
    state: UiState.Error,
    colors: DefaultColors,
    scrollState: ScrollState,
    modifier: Modifier,
) {
    val errorMessage = when (val error = state.error) {
        is TranscriptError.NotSupported ->
            stringResource(LR.string.error_transcript_format_not_supported, error.format)

        is TranscriptError.FailedToLoad ->
            stringResource(LR.string.error_transcript_failed_to_load)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
            .background(colors.backgroundColor())
            .verticalScroll(scrollState),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    color = colors.contentColor(),
                    shape = RoundedCornerShape(size = 4.dp),
                ),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                TextH30(
                    text = stringResource(LR.string.error),
                    color = colors.titleColor(),
                    textAlign = TextAlign.Center,
                )
                TextP40(
                    text = errorMessage,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                    color = colors.textColor(),
                )
            }
        }
    }
}

private data class DefaultColors(
    val theme: Theme,
    val podcast: Podcast?,
) {
    @Composable
    fun backgroundColor() =
        Color(theme.playerBackgroundColor(podcast))

    @Composable
    fun contentColor() =
        MaterialTheme.theme.colors.playerContrast06

    @Composable
    fun textColor() =
        MaterialTheme.theme.colors.playerContrast02

    @Composable
    fun titleColor() =
        MaterialTheme.theme.colors.playerContrast01
}
