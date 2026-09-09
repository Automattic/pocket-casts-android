package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.transcript.BookmarkTranscript
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TextSpan
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.math.roundToInt

/**
 * Renders a [BookmarkTranscript] with the bookmarked [passage] highlighted in the primary text
 * colour and the surrounding transcript dimmed, scrolling the passage into view. Used read-only in
 * the bookmark details and as the base for the editable passage editor.
 */
@Composable
fun BookmarkTranscriptView(
    transcript: BookmarkTranscript,
    passage: TextSpan?,
    modifier: Modifier = Modifier,
    scrollToPassage: Boolean = true,
) {
    val theme = rememberTranscriptTheme()
    val scrollState = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportHeight by remember { mutableIntStateOf(0) }

    val text = remember(transcript, passage, theme) {
        buildAnnotatedString {
            append(transcript.displayText)
            addStyle(SpanStyle(color = theme.secondaryText), 0, transcript.displayText.length)
            transcript.speakerSpans.forEach { span ->
                addStyle(SpeakerSpanStyle, span.start, span.end)
            }
            passage?.let { addStyle(SpanStyle(color = theme.primaryText), it.start, it.end) }
        }
    }

    Column(
        modifier = modifier
            .onSizeChanged { viewportHeight = it.height }
            .verticalScroll(scrollState),
    ) {
        Text(
            text = text,
            style = SimpleTextStyle,
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(ContentPadding),
        )
    }

    LaunchedEffect(layout, passage, viewportHeight) {
        val result = layout ?: return@LaunchedEffect
        if (!scrollToPassage || passage == null || viewportHeight == 0) return@LaunchedEffect
        val box = result.getBoundingBox(passage.start.coerceIn(0, transcript.displayText.length.coerceAtLeast(1) - 1))
        val target = (box.top - viewportHeight / 2 + box.height / 2).roundToInt()
        scrollState.scrollTo(target.coerceIn(0, scrollState.maxValue))
    }
}

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

private val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)

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
            modifier = Modifier.background(rememberTranscriptTheme().background),
        )
    }
}
