package au.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
import au.com.shiftyjelly.pocketcasts.ui.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType

@Composable
internal fun TranscriptLines(
    entries: List<TranscriptEntry>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    colors: TranscriptColors = TranscriptColors.default(MaterialTheme.theme.colors),
) {
    FadedLazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = modifier.verticalScrollBar(
            scrollState = state,
            thumbColor = colors.secondaryElement,
        ),
    ) {
        items(entries) { entry ->
            TranscriptLine(
                entry = entry,
                colors = colors,
                modifier = Modifier.padding(entry.padding()),
            )
        }
    }
}

@Composable
private fun TranscriptLine(
    entry: TranscriptEntry,
    colors: TranscriptColors,
    modifier: Modifier = Modifier,
) {
    Text(
        text = entry.text(),
        style = entry.textStyle(),
        color = colors.text,
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

internal val RobotoSerifFontFamily = FontFamily(Font(R.font.roboto_serif))

private val SimpleTextStyle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    fontFamily = RobotoSerifFontFamily,
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
            val transciptColors = rememberTranscriptColors()
            Column(
                modifier = Modifier.background(transciptColors.background),
            ) {
                TranscriptLines(
                    entries = TranscriptEntry.PreviewList,
                    colors = transciptColors,
                )
            }
        }
    }
}
