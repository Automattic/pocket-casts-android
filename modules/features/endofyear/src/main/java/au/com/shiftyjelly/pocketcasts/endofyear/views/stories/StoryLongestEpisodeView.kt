package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.models.db.helper.LongestEpisode
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryLongestEpisode
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun StoryLongestEpisodeView(
    story: StoryLongestEpisode,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val timeText = remember {
        StatsHelper.secondsToFriendlyString(
            story.longestEpisode.duration.toLong(),
            context.resources
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PodcastImage(
            uuid = story.longestEpisode.podcastUuid,
            modifier = modifier
                .size(200.dp)
                .padding(top = 4.dp, end = 12.dp, bottom = 4.dp)
        )
        TextH30(
            text = "The longest episode you listened to was ${story.longestEpisode.title} from the podcast ${story.longestEpisode.podcastTitle}",
            color = story.tintColor,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(16.dp)
        )
        TextP50(
            text = "This episode was $timeText long",
            color = story.tintColor,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StoryLongestEpisodePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Surface(color = Color.Black) {
            StoryLongestEpisodeView(
                story = StoryLongestEpisode(
                    longestEpisode = LongestEpisode(
                        title = "Episode Title",
                        duration = 100.0,
                        podcastUuid = "1",
                        podcastTitle = "Podcast Title",
                    )
                ),
            )
        }
    }
}
