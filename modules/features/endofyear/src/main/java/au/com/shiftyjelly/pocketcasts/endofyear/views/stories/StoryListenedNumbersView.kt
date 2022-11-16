package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastLogoWhite
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.transformPodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.utils.podcastDynamicBackground
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedNumbers
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp

@Composable
fun StoryListenedNumbersView(
    story: StoryListenedNumbers,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .podcastDynamicBackground(story.topPodcasts.atSafeIndex(3))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = modifier.height(40.dp))

        Spacer(modifier = modifier.weight(0.3f))

        PodcastCoverStack(story.topPodcasts.reversed())

        Spacer(modifier = modifier.weight(0.3f))

        PrimaryText(story)

        Spacer(modifier = modifier.weight(0.25f))

        SecondaryText(story)

        Spacer(modifier = modifier.weight(0.8f))

        PodcastLogoWhite()

        Spacer(modifier = modifier.height(40.dp))
    }
}

@Composable
private fun PodcastCoverStack(
    topPodcasts: List<TopPodcast>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val screenWidth = currentLocalView.width.pxToDp(context).dp
    val availableWidth = screenWidth * .74f
    Box(
        modifier = modifier
            .size(availableWidth)
            .padding(start = availableWidth * .41f, top = availableWidth * .09f)
            .transformPodcastCover()
    ) {
        PodcastCover(
            coverWidth = availableWidth * .23f,
            uuid = topPodcasts.atSafeIndex(5).uuid,
            modifier = modifier
                .offset(x = availableWidth / 4.5f, y = -availableWidth / 10.9f)
        )

        PodcastCover(
            coverWidth = availableWidth * .23f,
            uuid = topPodcasts.atSafeIndex(4).uuid,
            modifier = modifier
                .offset(x = -availableWidth / 2f, y = availableWidth / 2.3f)
        )

        PodcastCover(
            coverWidth = availableWidth * .31f,
            uuid = topPodcasts.atSafeIndex(0).uuid,
            modifier = modifier
                .offset(x = -availableWidth / 2.1f, y = -availableWidth / 6f)
        )

        PodcastCover(
            coverWidth = availableWidth * .27f,
            uuid = topPodcasts.atSafeIndex(2).uuid,
            modifier = modifier
                .offset(x = availableWidth / 4f, y = availableWidth / 2f)
        )

        PodcastCover(
            coverWidth = availableWidth * .38f,
            uuid = topPodcasts.atSafeIndex(1).uuid,
            modifier = modifier
                .offset(y = availableWidth / 4f)
        )
        PodcastCover(
            coverWidth = availableWidth * .35f,
            uuid = topPodcasts.atSafeIndex(3).uuid,
            modifier = modifier
                .offset(x = -availableWidth / 4f)
        )
    }
}

@Composable
private fun PrimaryText(
    story: StoryListenedNumbers,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(
        id = R.string.end_of_year_story_listened_to_numbers,
        story.listenedNumbers.numberOfPodcasts,
        story.listenedNumbers.numberOfEpisodes
    )
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryListenedNumbers,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(R.string.end_of_year_story_listened_to_numbers_subtitle)
    StorySecondaryText(text = text, color = story.tintColor, modifier = modifier)
}

private fun List<TopPodcast>.atSafeIndex(index: Int) =
    this[index.coerceAtMost(size - 1)]
        .toPodcast()
