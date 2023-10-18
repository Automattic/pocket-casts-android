package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.CoverSize
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackground
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.utils.atSafeIndex
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
                .padding(vertical = 40.dp)
        ) {
            Spacer(modifier = modifier.weight(0.2f))

            PrimaryText(story)

            Spacer(modifier = modifier.weight(0.1f))

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
        PodcastCover(
            uuid = story.topPodcasts.atSafeIndex(1).uuid,
            coverWidth = (widthInDp * .3).dp,
            modifier = Modifier
                .offset(
                    x = -(widthInDp * .53).dp,
                    y = -(heightInDp * .13).dp,
                )
                .alpha(0.3f)
        )
        PodcastCover(
            uuid = story.topPodcasts.atSafeIndex(3).uuid,
            coverWidth = (widthInDp * .25).dp,
            modifier = Modifier
                .offset(
                    x = -(widthInDp * .3).dp,
                    y = (heightInDp * .2).dp,
                )
                .alpha(0.5f)
        )
        PodcastCover(
            uuid = story.topPodcasts.atSafeIndex(2).uuid,
            coverWidth = (widthInDp * .32).dp,
            modifier = Modifier
                .offset(
                    x = (widthInDp * .3).dp,
                    y = (heightInDp * .23).dp,
                )
                .alpha(0.5f)
        )
        PodcastCover(
            uuid = story.topPodcasts.atSafeIndex(4).uuid,
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
private fun PrimaryText(
    story: StoryTopPodcast,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(
        id = R.string.end_of_year_story_top_podcast,
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
    StorySecondaryText(text = text, color = story.tintColor, modifier = modifier)
}
