package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCoverType
import au.com.shiftyjelly.pocketcasts.compose.components.transformPodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastLogoWhite
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.utils.podcastDynamicBackground
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListeningTime
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.settings.util.FunnyTimeConverter
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StoryListeningTimeView(
    story: StoryListeningTime,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .podcastDynamicBackground(story.podcasts[0].toPodcast())
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = modifier.height(40.dp))

        Spacer(modifier = modifier.weight(.75f))

        PrimaryText(story, modifier)

        Spacer(modifier = modifier.weight(0.25f))

        SecondaryText(story, modifier)

        Spacer(modifier = modifier.weight(0.25f))

        PodcastCoverRow(story, modifier)

        Spacer(modifier = modifier.weight(.5f))

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
    val language = Locale.current.language
    val timeText = StatsHelper.secondsToFriendlyString(story.listeningTimeInSecs, context.resources)
    val textResId = if (language == "en") {
        LR.string.end_of_year_listening_time_english_only
    } else {
        LR.string.end_of_year_listening_time
    }
    val text = stringResource(textResId, timeText)
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
    Box(
        modifier = modifier
            .graphicsLayer { translationY = coverWidth.toPx() / 1.75f }
            .height(coverWidth * 2.2f)
    ) {
        Row(
            modifier
                .transformPodcastCover()
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
}
