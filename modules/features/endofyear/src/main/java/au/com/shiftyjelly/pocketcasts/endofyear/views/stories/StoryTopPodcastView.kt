package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.CoverSize
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackground
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopPodcast
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp

@Composable
fun StoryTopPodcastView(
    story: StoryTopPodcast,
    modifier: Modifier = Modifier,
) {
    Box {
        StoryBlurredBackground(
            Offset(
                -LocalView.current.width * 0.75f,
                LocalView.current.height * 0.3f
            ),
        )
        Column(
            verticalArrangement = Arrangement.Top,
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

            Spacer(modifier = modifier.weight(0.5f))

            PodcastCoverStack(story)

            Spacer(modifier = modifier.weight(1f))
        }
    }
}

@Composable
private fun PodcastCoverStack(
    story: StoryTopPodcast,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val widthInDp = currentLocalView.width.pxToDp(context)
    val heightInDp = currentLocalView.height.pxToDp(context)
    Box(
        modifier = modifier
            .wrapContentSize(unbounded = true),
        contentAlignment = Alignment.Center,
    ) {
        PodcastCoverOrEmpty(
            story = story,
            index = 1,
            coverWidth = (widthInDp * .3).dp,
            modifier = Modifier
                .offset(
                    x = -(widthInDp * .53).dp,
                    y = -(heightInDp * .13).dp,
                )
                .alpha(0.3f)
        )
        PodcastCoverOrEmpty(
            story = story,
            index = 3,
            coverWidth = (widthInDp * .25).dp,
            modifier = Modifier
                .offset(
                    x = -(widthInDp * .3).dp,
                    y = (heightInDp * .2).dp,
                )
                .alpha(0.5f)
        )
        PodcastCoverOrEmpty(
            story = story,
            index = 2,
            coverWidth = (widthInDp * .32).dp,
            modifier = Modifier
                .offset(
                    x = (widthInDp * .3).dp,
                    y = (heightInDp * .23).dp,
                )
                .alpha(0.5f)
        )
        PodcastCoverOrEmpty(
            story = story,
            index = 4,
            coverWidth = (widthInDp * .2).dp,
            modifier = Modifier
                .offset(
                    x = (widthInDp * .4).dp,
                    y = -(heightInDp * .11).dp,
                )
                .alpha(0.2f)
        )
        PodcastCover(
            uuid = story.topPodcast.uuid,
            coverWidth = (widthInDp * .7).dp,
            coverSize = CoverSize.BIG
        )
    }
}

@Composable
private fun PodcastCoverOrEmpty(
    story: StoryTopPodcast,
    index: Int,
    coverWidth: Dp,
    modifier: Modifier,
) {
    if (index < story.topPodcasts.size) {
        PodcastCover(
            uuid = story.topPodcasts[index].uuid,
            coverWidth = coverWidth,
            modifier = modifier
        )
    } else {
        Unit
    }
}

@Composable
private fun PrimaryText(
    story: StoryTopPodcast,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(
        id = R.string.end_of_year_story_top_podcast_title,
        story.topPodcast.title
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
    StorySecondaryText(text = text, color = story.subtitleColor, modifier = modifier)
}
