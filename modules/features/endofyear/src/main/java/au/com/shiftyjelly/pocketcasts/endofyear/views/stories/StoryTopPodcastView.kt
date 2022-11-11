package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.util.PodcastCoverBig
import au.com.shiftyjelly.pocketcasts.endofyear.util.RectangleCover
import au.com.shiftyjelly.pocketcasts.endofyear.util.transformPodcastCover
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopPodcast
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp

@Composable
fun StoryTopPodcastView(
    story: StoryTopPodcast,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = story.topPodcast.toPodcast().getTintColor(false)

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(backgroundColor))
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

        Logo()

        Spacer(modifier = modifier.height(40.dp))
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
    val translateBy = (coverWidth.value * .2).toInt().dpToPx(context)

    Box {
        (0..2).reversed().forEach { index ->
            Box(
                modifier = modifier
                    .padding(top = (index * (coverWidth.value * .25)).dp)
                    .transformPodcastCover()
                    .graphicsLayer(translationX = -translateBy.toFloat())
            ) {
                with(story.topPodcast) {
                    when (index) {
                        0 -> PodcastCoverBig(uuid = uuid, coverWidth = coverWidth)

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
    TextH20(
        text = stringResource(
            id = R.string.end_of_year_story_top_podcast,
            story.topPodcast.title, story.topPodcast.author
        ),
        textAlign = TextAlign.Center,
        color = story.tintColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
    )
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
    TextP40(
        text = stringResource(
            id = R.string.end_of_year_story_top_podcast_subtitle,
            story.topPodcast.numberOfPlayedEpisodes,
            timeText
        ),
        textAlign = TextAlign.Center,
        color = story.tintColor,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.8f)
            .padding(horizontal = 40.dp)
    )
}
