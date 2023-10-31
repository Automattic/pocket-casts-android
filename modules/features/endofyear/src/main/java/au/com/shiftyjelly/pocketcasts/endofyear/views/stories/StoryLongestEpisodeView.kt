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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryLongestEpisode
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp

@Composable
fun StoryLongestEpisodeView(
    story: StoryLongestEpisode,
    modifier: Modifier = Modifier,
) {
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

        Spacer(modifier = modifier.height(14.dp))

        SecondaryText(story)

        Spacer(modifier = modifier.weight(0.2f))

        PodcastCoverStack(story)

        Spacer(modifier = modifier.weight(1f))
    }
}

@Composable
private fun PodcastCoverStack(
    story: StoryLongestEpisode,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val width = currentLocalView.width
    val podcastUuid = story.longestEpisode.podcastUuid
    Box(
        modifier = modifier
            .wrapContentSize()
            .offset(
                x = (width * 0.04f).toInt().pxToDp(context).dp,
                y = (width * 0.05f).toInt().pxToDp(context).dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PodcastCover(
            uuid = podcastUuid,
            coverWidth = (width * 0.5f).toInt().pxToDp(context).dp,
            modifier = Modifier.offset(
                x = -(width * 0.4f).toInt().pxToDp(context).dp,
                y = (width * 0.4f).toInt().pxToDp(context).dp,
            ),
        )
        PodcastCover(
            uuid = podcastUuid,
            coverWidth = (width * 0.55f).toInt().pxToDp(context).dp,
            modifier = Modifier.offset(
                x = -(width * 0.32f).toInt().pxToDp(context).dp,
                y = (width * 0.32f).toInt().pxToDp(context).dp,
            ),
        )
        PodcastCover(
            uuid = podcastUuid,
            coverWidth = (width * 0.6f).toInt().pxToDp(context).dp,
            modifier = Modifier.offset(
                x = -(width * 0.24f).toInt().pxToDp(context).dp,
                y = (width * 0.24f).toInt().pxToDp(context).dp,
            ),
        )
        PodcastCover(
            uuid = podcastUuid,
            coverWidth = (width * 0.65f).toInt().pxToDp(context).dp,
            modifier = Modifier.offset(
                x = -(width * 0.16f).toInt().pxToDp(context).dp,
                y = (width * 0.16f).toInt().pxToDp(context).dp,
            ),
        )
        PodcastCover(
            uuid = podcastUuid,
            coverWidth = (width * 0.7f).toInt().pxToDp(context).dp,
            modifier = Modifier.offset(
                x = -(width * 0.08f).toInt().pxToDp(context).dp,
                y = (width * 0.08f).toInt().pxToDp(context).dp,
            ),
        )
        PodcastCover(
            uuid = podcastUuid,
            coverWidth = (width * 0.75f).toInt().pxToDp(context).dp,
        )
    }
}

@Composable
private fun PrimaryText(
    story: StoryLongestEpisode,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val timeText = StatsHelper.secondsToFriendlyString(
        story.longestEpisode.duration.toLong(),
        context.resources
    )
    val text = stringResource(
        id = R.string.end_of_year_story_longest_episode_title,
        timeText
    )
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryLongestEpisode,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(
        id = R.string.end_of_year_story_longest_episode_subtitle,
        story.longestEpisode.title, story.longestEpisode.podcastTitle
    )
    StorySecondaryText(text = text, color = story.subtitleColor, modifier = modifier)
}
