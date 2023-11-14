package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopFivePodcasts
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import com.mxalbert.sharedelements.ProgressThresholds
import com.mxalbert.sharedelements.SharedElement
import com.mxalbert.sharedelements.SharedElementsTransitionSpec
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val AniDurationInMs = 500

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
                .padding(vertical = 30.dp)
        ) {
            Spacer(modifier = modifier.height(40.dp))

            PrimaryText(story)

            Spacer(modifier = modifier.height(14.dp))

            SecondaryText(story)

            Spacer(modifier = modifier.weight(0.3f))

            PodcastList(story)

            Spacer(modifier = modifier.weight(0.5f))
        }
    }
}

@Composable
private fun PrimaryText(
    story: StoryTopFivePodcasts,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(LR.string.eoy_story_top_podcasts_title)
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryTopFivePodcasts,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(LR.string.eoy_story_top_podcasts_subtitle)
    StorySecondaryText(text = text, color = story.subtitleColor, modifier = modifier)
}

@Composable
private fun PodcastList(story: StoryTopFivePodcasts) {
    story.topPodcasts.forEachIndexed { index, topPodcast ->
        PodcastItem(
            storyIdentifier = story.identifier,
            podcast = topPodcast.toPodcast(),
            position = index,
            tintColor = story.tintColor,
            subtitleColor = story.subtitleColor,
        )
    }
}

@Composable
fun PodcastItem(
    storyIdentifier: String,
    podcast: Podcast,
    position: Int,
    tintColor: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val heightInDp = currentLocalView.height.pxToDp(context)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
    ) {
        TextH30(
            text = "${position + 1}",
            color = subtitleColor,
            fontWeight = FontWeight.W700,
            fontFamily = FontFamily(listOf(Font(UR.font.dm_sans))),
            disableScale = disableScale(),
            modifier = modifier
                .padding(end = 14.dp)
                .widthIn(min = 16.dp)
        )
        Row(
            modifier = modifier
                .padding(vertical = 10.dp)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SharedElement(
                key = "cover$position",
                screenKey = storyIdentifier,
                transitionSpec = SharedElementsTransitionSpec(
                    durationMillis = AniDurationInMs,
                    scaleProgressThresholds = ProgressThresholds(0f, 0.9f),
                )
            ) {
                PodcastImage(
                    uuid = podcast.uuid,
                    modifier = modifier.size((heightInDp * 0.09f).dp)
                )
            }
            Column(
                modifier = modifier
                    .padding(start = 14.dp)
            ) {
                TextH30(
                    text = podcast.title,
                    color = tintColor,
                    maxLines = 2,
                    fontFamily = FontFamily(listOf(Font(UR.font.dm_sans))),
                    fontWeight = FontWeight.Bold,
                    disableScale = disableScale(),
                    modifier = modifier
                        .padding(bottom = 3.dp)
                )
                TextH70(
                    text = podcast.author,
                    color = subtitleColor,
                    maxLines = 1,
                    fontFamily = FontFamily(listOf(Font(UR.font.dm_sans))),
                    fontWeight = FontWeight.Bold,
                    disableScale = disableScale(),
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
                storyIdentifier = "storyIdentifier",
                podcast = Podcast(
                    uuid = "",
                    title = "Title",
                    author = "Author",
                ),
                position = 0,
                tintColor = Color.White,
                subtitleColor = Color(0xFF8F97A4),
            )
        }
    }
}
