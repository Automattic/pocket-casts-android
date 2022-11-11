package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastCoverType
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastLogoWhite
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.transformPodcastCover
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListeningTime
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.settings.util.FunnyTimeConverter
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StoryListeningTimeView(
    story: StoryListeningTime,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = story.podcasts[0].toPodcast().getTintColor(false)

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(backgroundColor))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = modifier.height(40.dp))

        Spacer(modifier = modifier.weight(.75f))

        PrimaryText(story, modifier)

        Spacer(modifier = modifier.weight(0.25f))

        SecondaryText(story, modifier)

        Spacer(modifier = modifier.weight(1f))

        PodcastCoverRow(story, modifier)

        Spacer(modifier = modifier.weight(1f))

        PodcastLogoWhite()

        Spacer(modifier = modifier.height(40.dp))
    }
}

@Composable
private fun PrimaryText(
    story: StoryListeningTime,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val timeText = StatsHelper.secondsToFriendlyString(story.listeningTimeInSecs, context.resources)
    val text = stringResource(LR.string.end_of_year_listening_time, timeText)
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryListeningTime,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val funnyText = FunnyTimeConverter().timeSecsToFunnyText(
        story.listeningTimeInSecs,
        context.resources
    )
    StorySecondaryText(text = funnyText, color = story.tintColor, modifier = modifier)
}

@Composable
private fun PodcastCoverRow(
    story: StoryListeningTime,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val coverWidth = (currentLocalView.width.pxToDp(context).dp - 15.dp) / 3
    val translateBy = 20.dpToPx(context).toFloat()
    Row(
        modifier
            .transformPodcastCover()
            .graphicsLayer(translationX = -translateBy)
    ) {
        listOf(1, 0, 2).forEach { index ->
            val podcastIndex = index.coerceAtMost(story.podcasts.size - 1)
            Row {
                PodcastCover(
                    uuid = story.podcasts[podcastIndex].uuid,
                    coverWidth = coverWidth,
                    coverType = PodcastCoverType.SMALL
                )
                Spacer(modifier = modifier.width(5.dp))
            }
        }
    }
}
