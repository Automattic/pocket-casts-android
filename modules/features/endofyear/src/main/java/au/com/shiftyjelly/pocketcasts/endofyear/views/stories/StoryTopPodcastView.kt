package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.CoverSize
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.compose.components.RectangleCover
import au.com.shiftyjelly.pocketcasts.compose.components.transformPodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastLogoWhite
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.utils.podcastDynamicBackground
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopPodcast
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp

@Composable
fun StoryTopPodcastView(
    story: StoryTopPodcast,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .podcastDynamicBackground(story.topPodcast.toPodcast())
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = modifier.height(40.dp))

        Spacer(modifier = modifier.weight(0.7f))

        PodcastCoverStack(story)

        Spacer(modifier = modifier.weight(0.3f))

        PrimaryText(story)

        Spacer(modifier = modifier.weight(0.25f))

        SecondaryText(story)

        Spacer(modifier = modifier.weight(0.8f))

        PodcastLogoWhite()

        Spacer(modifier = modifier.height(30.dp))
    }
}

@Composable
private fun PodcastCoverStack(
    story: StoryTopPodcast,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val coverWidth = (currentLocalView.width.pxToDp(context).dp) / 2.5f

    Box {
        (0..2).reversed().forEach { index ->
            Box(
                modifier = modifier
                    .padding(top = (index * (coverWidth.value * .17)).dp)
                    .transformPodcastCover()
            ) {
                with(story.topPodcast) {
                    when (index) {
                        0 -> PodcastCover(
                            uuid = uuid,
                            coverWidth = coverWidth,
                            coverSize = CoverSize.BIG
                        )

                        1 -> {
                            val backgroundColor = Color(toPodcast().getTintColor(false))
                            RectangleCover(
                                coverWidth = coverWidth,
                                backgroundColor = backgroundColor
                            )
                        }

                        2 -> {
                            val backgroundColor = Color(toPodcast().getTintColor(true))
                            RectangleCover(
                                coverWidth = coverWidth,
                                backgroundColor = backgroundColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryText(
    story: StoryTopPodcast,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(
        id = R.string.end_of_year_story_top_podcast,
        story.topPodcast.title, story.topPodcast.author
    )
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryTopPodcast,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val timeText = StatsHelper.secondsToFriendlyString(
        story.topPodcast.totalPlayedTime.toLong(),
        context.resources
    )
    val text = stringResource(
        id = R.string.end_of_year_story_top_podcast_subtitle,
        story.topPodcast.numberOfPlayedEpisodes,
        timeText
    )
    StorySecondaryText(text = text, color = story.tintColor, modifier = modifier)
}
