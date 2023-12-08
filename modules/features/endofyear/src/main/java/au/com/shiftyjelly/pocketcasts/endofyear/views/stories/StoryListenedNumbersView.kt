package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollDirection
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollingRow
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackground
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.utils.atSafeIndex
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedNumbers
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp

private const val PodcastCoverScaleFactor = 1.5f

@Composable
fun StoryListenedNumbersView(
    story: StoryListenedNumbers,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    Box {
        StoryBlurredBackground(
            Offset(
                LocalView.current.width * 0.4f,
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

            Spacer(modifier = modifier.weight(1f))

            PodcastCoverStack(story.topPodcasts.reversed(), paused)

            Spacer(modifier = modifier.weight(1f))
        }
    }
}

@Composable
private fun PodcastCoverStack(
    topPodcasts: List<TopPodcast>,
    paused: Boolean,
) {
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val screenWidth = currentLocalView.width.pxToDp(context).dp
    val size = screenWidth * 0.4f / PodcastCoverScaleFactor
    val spacedBy = (16 / PodcastCoverScaleFactor).dp
    Column(
        Modifier
            .wrapContentWidth()
            .rotate(-15f)
            .scale(PodcastCoverScaleFactor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacedBy)
    ) {
        ScrollingRow(
            scrollDirection = ScrollDirection.RIGHT,
            spacedBy = spacedBy,
            paused = paused,
        ) {
            for (i in 0..3) {
                PodcastCover(
                    coverWidth = size,
                    uuid = topPodcasts.atSafeIndex(i).uuid,
                )
            }
        }

        ScrollingRow(
            scrollDirection = ScrollDirection.LEFT,
            spacedBy = spacedBy,
            paused = paused,
        ) {
            for (i in 4..7) {
                PodcastCover(
                    coverWidth = size,
                    uuid = topPodcasts.atSafeIndex(i).uuid,
                )
            }
        }
    }
}

@Composable
private fun PrimaryText(
    story: StoryListenedNumbers,
    modifier: Modifier = Modifier,
) {
    val language = Locale.current.language
    val titleResId = if (language == "en") {
        R.string.end_of_year_story_listened_to_numbers_english_only
    } else {
        R.string.end_of_year_story_listened_to_numbers
    }
    val text = stringResource(
        id = titleResId,
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
    StorySecondaryText(text = text, color = story.subtitleColor, modifier = modifier)
}
