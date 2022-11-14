package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastLogoWhite
import au.com.shiftyjelly.pocketcasts.endofyear.utils.podcastDynamicBackground
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopFivePodcasts
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun StoryTopFivePodcastsView(
    story: StoryTopFivePodcasts,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .podcastDynamicBackground(story.topPodcasts[0].toPodcast())
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = modifier.height(40.dp))

        Spacer(modifier = modifier.weight(1f))

        Title(story)

        Spacer(modifier = modifier.weight(0.5f))

        PodcastList(story)

        Spacer(modifier = modifier.weight(1f))

        PodcastLogoWhite()

        Spacer(modifier = modifier.height(40.dp))
    }
}

@Composable
private fun Title(
    story: StoryTopFivePodcasts,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(R.string.end_of_year_story_top_podcasts)
    Text(
        text = text,
        textAlign = TextAlign.Center,
        color = story.tintColor,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .padding(horizontal = 40.dp)
            .fillMaxWidth()
    )
}

@Composable
private fun PodcastList(story: StoryTopFivePodcasts) {
    story.topPodcasts.forEachIndexed { index, topPodcast ->
        PodcastItem(
            podcast = topPodcast.toPodcast(),
            position = index,
            tintColor = story.tintColor
        )
    }
}

@Composable
fun PodcastItem(
    podcast: Podcast,
    position: Int,
    tintColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
    ) {
        TextH20(
            text = "${position + 1}.",
            color = tintColor,
            modifier = modifier.padding(end = 14.dp)
        )
        Row(
            modifier = modifier
                .padding(vertical = 10.dp)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PodcastImage(
                uuid = podcast.uuid,
                modifier = modifier.size(64.dp)
            )
            Column(
                modifier = modifier
                    .padding(start = 14.dp)
            ) {
                TextH20(
                    text = podcast.title,
                    color = tintColor,
                    maxLines = 2,
                    modifier = modifier
                        .padding(bottom = 3.dp)
                )
                TextH70(
                    text = podcast.author,
                    color = tintColor,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    modifier = modifier.alpha(0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PodcastItemPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Surface(color = Color.Black) {
            PodcastItem(
                podcast = Podcast(
                    uuid = "",
                    title = "Title",
                    author = "Author",
                ),
                position = 0,
                tintColor = Color.White,
            )
        }
    }
}
