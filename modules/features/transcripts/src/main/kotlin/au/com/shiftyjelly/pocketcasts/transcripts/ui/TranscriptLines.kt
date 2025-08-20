package au.com.shiftyjelly.pocketcasts.transcripts.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
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
import au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection.CustomMenuItemOption
import au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection.CustomTextToolbar
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.transcripts.SearchState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.utils.search.SearchCoordinates
import au.com.shiftyjelly.pocketcasts.utils.search.SearchMatches

@Composable
internal fun TranscriptLines(
    transcript: Transcript.Text,
    searchState: SearchState,
    modifier: Modifier = Modifier,
    isContentObscured: Boolean = false,
    state: LazyListState = rememberLazyListState(),
    theme: TranscriptTheme = TranscriptTheme.default(MaterialTheme.theme.colors),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .then(if (isContentObscured) Modifier.obsureContent() else Modifier),
    ) {
        CompositionLocalProvider(
            LocalTextToolbar provides CustomTextToolbar(
                view = LocalView.current,
                customMenuItems = buildList {
                    // Only show the share option on older versions of Android, as the new versions
                    // have a share feature built into the copy
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        add(CustomMenuItemOption.Share)
                    }
                },
                clipboard = LocalClipboard.current,
            ),
        ) {
            SelectionContainer {
                FadedLazyColumn(
                    state = state,
                    modifier = Modifier.verticalScrollBar(
                        scrollState = state,
                        thumbColor = theme.secondaryElement,
                        contentPadding = PaddingValues(bottom = 64.dp),
                    ),
                ) {
                    item {
                        if (transcript.isGenerated) {
                            GeneratedTranscriptHeader(
                                theme = theme,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )
                        } else {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    itemsIndexed(transcript.entries) { index, entry ->
                        TranscriptLine(
                            entryIndex = index,
                            entry = entry,
                            searchState = searchState,
                            theme = theme,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(entry.padding()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedTranscriptHeader(
    theme: TranscriptTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.transcript_generated_header),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = theme.primaryText,
        )
        Box(
            modifier = Modifier
                .background(theme.secondaryElement)
                .width(48.dp)
                .height(1.dp),
        )
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
        val searchTermLength = searchState.searchTerm.length
        searchState
            .matches
            .matchingCoordinates[entryIndex]
            ?.map { start -> start to start + searchTermLength }
            ?.filter { (start, end) -> isValidHighlightRange(start, end, entryText.length) }
            .orEmpty()
    }

    Text(
        text = buildAnnotatedString {
            append(entryText)
            searchHighlights.forEach { (startIndex, endIndex) ->
                val highlightCoordinates = SearchCoordinates(line = entryIndex, match = startIndex)
                val style = if (highlightCoordinates == searchState.matches.selectedCoordinate) {
                    theme.searchHighlightSpanStyle
                } else {
                    theme.searchDefaultSpanStyle
                }
                addStyle(style, startIndex, endIndex)
            }
        },
        style = entry.textStyle(),
        color = theme.primaryText,
        modifier = modifier,
    )
}

private fun Modifier.obsureContent(): Modifier {
    return if (Build.VERSION.SDK_INT >= 31) {
        blur(6.dp, BlurredEdgeTreatment.Unbounded)
    } else {
        alpha(0.1f)
    }
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
    return start < end && start >= 0 && end <= maxLength
}

private val SimpleTextStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 16.sp * 1.5f,
    fontWeight = FontWeight.Medium,
    fontFamily = TranscriptTheme.RobotoSerifFontFamily,
)
private val SpeakerTextStyle = SimpleTextStyle.copy(
    fontSize = 12.sp,
    lineHeight = 12.sp * 1.5f,
)

private val SimplePadding = PaddingValues(bottom = 16.dp)
private val SpeakerPadding = PaddingValues(bottom = 12.dp, top = 16.dp)

@Preview
@Composable
private fun TranscriptLinesNonGeneratedPreview() {
    AppThemeWithBackground(ThemeType.DARK) {
        Column {
            TranscriptLines(
                transcript = Transcript.TextPreview.copy(isGenerated = false),
                searchState = SearchState.Empty,
            )
        }
    }
}

@Preview
@Composable
private fun TranscriptLinesObsucredPreview() {
    AppThemeWithBackground(ThemeType.DARK) {
        Column {
            TranscriptLines(
                transcript = Transcript.TextPreview.copy(isGenerated = false),
                searchState = SearchState.Empty,
                isContentObscured = true,
            )
        }
    }
}

@Preview
@Composable
private fun TranscriptLinesPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        Column {
            TranscriptLines(
                transcript = Transcript.TextPreview.copy(isGenerated = true),
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
                    transcript = Transcript.TextPreview.copy(isGenerated = true),
                    searchState = remember { SearchStatePreview },
                    theme = transcriptTheme,
                )
            }
        }
    }
}

private val SearchStatePreview: SearchState
    get() {
        val searchTerm = "lorem"
        return SearchState(
            isSearchOpen = false,
            searchTerm = searchTerm,
            matches = SearchMatches(
                selectedCoordinate = SearchCoordinates(
                    line = 0,
                    match = TranscriptEntry.PreviewList[0].text().lastIndexOf(searchTerm, ignoreCase = true),
                ),
                matchingCoordinates = TranscriptEntry.PreviewList
                    .mapIndexedNotNull { index, entry ->
                        val text = entry.text()
                        val startIndices = searchTerm.toRegex(RegexOption.IGNORE_CASE)
                            .findAll(text)
                            .map { it.range.first }
                            .toList()
                            .takeIf { it.isNotEmpty() }
                        startIndices?.let { index to it }
                    }
                    .toMap(),
            ),
        )
    }
