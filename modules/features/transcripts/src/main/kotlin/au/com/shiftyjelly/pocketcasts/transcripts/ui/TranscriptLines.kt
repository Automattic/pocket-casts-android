package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.extensions.verticalScrollBar
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.transcripts.SearchCoordinates
import au.com.shiftyjelly.pocketcasts.transcripts.SearchState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType

@Composable
internal fun TranscriptLines(
    entries: List<TranscriptEntry>,
    searchState: SearchState,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    theme: TranscriptTheme = TranscriptTheme.default(MaterialTheme.theme.colors),
) {
    FadedLazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = modifier.verticalScrollBar(
            scrollState = state,
            thumbColor = theme.secondaryElement,
        ),
    ) {
        itemsIndexed(entries) { index, entry ->
            TranscriptLine(
                entryIndex = index,
                entry = entry,
                searchState = searchState,
                theme = theme,
                modifier = Modifier.padding(entry.padding()),
            )
        }
    }
}

@Composable
private fun TranscriptLine(
    entryIndex: Int,
    entry: TranscriptEntry,
    searchState: SearchState,
    theme: TranscriptTheme,
    modifier: Modifier = Modifier,
) {
    val entryText = entry.text()
    val searchHighlights = remember(entryIndex, entryText, searchState) {
        val searchTermLength = searchState.searchTerm?.length ?: 0
        searchState
            .searchResultIndices[entryIndex]
            ?.map { start -> start to start + searchTermLength }
            ?.filter { (start, end) -> isValidHighlightRange(start, end, entryText.length) }
            .orEmpty()
    }

    Text(
        text = buildAnnotatedString {
            append(entryText)
            searchHighlights.forEach { (startIndex, endIndex) ->
                val highlightCoordinates = SearchCoordinates(
                    lineIndex = entryIndex,
                    matchIndex = startIndex,
                )
                val style = if (highlightCoordinates == searchState.selectedSearchCoordinates) {
                    theme.searchHighlightSpanStyle
                } else {
                    theme.searchDefaultSpanStyle
                }
                addStyle(style, startIndex, endIndex)
            }
        },
        style = entry.textStyle(),
        color = theme.text,
        modifier = modifier,
    )
}

private fun TranscriptEntry.text() = when (this) {
    is TranscriptEntry.Text -> value
    is TranscriptEntry.Speaker -> name
}

private fun TranscriptEntry.textStyle() = when (this) {
    is TranscriptEntry.Text -> SimpleTextStyle
    is TranscriptEntry.Speaker -> SpeakerTextStyle
}

private fun TranscriptEntry.padding() = when (this) {
    is TranscriptEntry.Text -> SimplePadding
    is TranscriptEntry.Speaker -> SpeakerPadding
}

private fun isValidHighlightRange(start: Int, end: Int, maxLength: Int): Boolean {
    if (start > end) {
        return false
    }
    if (start < 0 || end < 0) {
        return false
    }
    if (start > maxLength || end > maxLength) {
        return false
    }

    return true
}

private val SimpleTextStyle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    fontFamily = TranscriptTheme.RobotoSerifFontFamily,
)
private val SpeakerTextStyle = SimpleTextStyle.copy(
    fontSize = 12.sp,
)

private val SimplePadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 16.dp)
private val SpeakerPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 8.dp, top = 16.dp)

@Preview
@Composable
private fun TranscriptLinesPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        Column {
            TranscriptLines(
                entries = TranscriptEntry.PreviewList,
                searchState = remember { SearchStatePreview },
            )
        }
    }
}

@Preview
@Composable
private fun TranscriptLinesPlayerPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(ThemeType.ROSE) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            val transcriptTheme = rememberTranscriptTheme()
            Column(
                modifier = Modifier.background(transcriptTheme.background),
            ) {
                TranscriptLines(
                    entries = TranscriptEntry.PreviewList,
                    theme = transcriptTheme,
                    searchState = remember { SearchStatePreview },
                )
            }
        }
    }
}

private val SearchStatePreview
    get() = SearchState(
        searchTerm = "lorem",
        selectedSearchCoordinates = SearchCoordinates(
            lineIndex = 0,
            matchIndex = TranscriptEntry.PreviewList[0].text().lastIndexOf("lorem", ignoreCase = true),
        ),
        searchResultIndices = TranscriptEntry.PreviewList
            .mapIndexedNotNull { index, entry ->
                val text = entry.text()
                val startIndices = "lorem".toRegex(RegexOption.IGNORE_CASE)
                    .findAll(text)
                    .map { it.range.first }
                    .toList()
                    .takeIf { it.isNotEmpty() }
                startIndices?.let { index to it }
            }
            .toMap(),
    )
