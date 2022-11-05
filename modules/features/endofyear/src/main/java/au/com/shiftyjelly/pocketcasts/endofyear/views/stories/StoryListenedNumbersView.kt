package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun StoryListenedNumbersView(
    story: StoryListenedNumbers,
    modifier: Modifier = Modifier,
) {
    TextH30(
        text = "You listened to ${story.listenedNumbers.numberOfPodcasts} different podcasts and ${story.listenedNumbers.numberOfEpisodes} episodes, but there was one that you kept coming back to...",
        textAlign = TextAlign.Center,
        color = story.tintColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun StoryListenedNumbersPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Surface(color = Color.Black) {
            StoryListenedNumbersView(
                story = StoryListenedNumbers(
                    ListenedNumbers(
                        numberOfPodcasts = 2,
                        numberOfEpisodes = 3,
                    )
                ),
            )
        }
    }
}
