package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackground
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.disableScale
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopFivePodcasts
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun StoryTopFivePodcastsView(
    story: StoryTopFivePodcasts,
    modifier: Modifier = Modifier,
) {
    Box {
        StoryBlurredBackground(
            Offset(
                LocalView.current.width * 0.6f,
                -LocalView.current.height * 0.4f
            ),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 40.dp)
        ) {
            Spacer(modifier = modifier.weight(0.2f))

            PrimaryText(story)

            Spacer(modifier = modifier.weight(0.1f))

            SecondaryText(story)

            Spacer(modifier = modifier.weight(0.5f))

            PodcastList(story)

            Spacer(modifier = modifier.weight(1f))
        }
    }
}

@Composable
private fun PrimaryText(
    story: StoryTopFivePodcasts,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(R.string.eoy_story_top_podcasts_title)
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryTopFivePodcasts,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(R.string.eoy_story_top_podcasts_subtitle)
    StorySecondaryText(text = text, color = story.tintColor, modifier = modifier)
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
        TextH30(
            text = "${position + 1}.",
            color = tintColor,
            fontWeight = FontWeight.Normal,
            disableScale = disableScale(),
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
                TextH30(
                    text = podcast.title,
                    color = tintColor,
                    maxLines = 2,
                    fontWeight = FontWeight.Bold,
                    disableScale = disableScale(),
                    modifier = modifier
                        .padding(bottom = 3.dp)
                )
                TextH70(
                    text = podcast.author,
                    color = tintColor,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    disableScale = disableScale(),
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
