package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryLongestEpisode
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

private const val AnimDurationInMs = 1000
private val animTargetValue = listOf(0.4f, 0.32f, 0.24f, 0.16f, 0.08f, 0f)

@Composable
fun StoryLongestEpisodeView(
    story: StoryLongestEpisode,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 30.dp),
    ) {
        Spacer(modifier = modifier.height(40.dp))

        PrimaryText(story)

        Spacer(modifier = modifier.height(14.dp))

        SecondaryText(story)

        Spacer(modifier = modifier.weight(0.2f))

        PodcastCoverStack(story, paused)

        Spacer(modifier = modifier.weight(1f))
    }
}

@Composable
private fun PodcastCoverStack(
    story: StoryLongestEpisode,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val width = currentLocalView.width
    val podcastUuid = story.longestEpisode.podcastUuid

    Box(
        modifier = modifier
            .wrapContentSize(),
        contentAlignment = Alignment.BottomStart,
    ) {
        val animationSpec = tween<Float>(
            durationMillis = AnimDurationInMs,
            delayMillis = 0,
        )
        for (i in 0..5) {
            val animOffsetX = remember { Animatable(1f) }
            val animOffsetY = remember { Animatable(0.5f) }
            LaunchedEffect(paused) {
                try {
                    if (paused) {
                        /* Stop animations when story is paused */
                        animOffsetX.stop()
                        animOffsetY.stop()
                    }
                    /* Launch concurrent offset animations along x and y axis */
                    launch {
                        if (!paused) {
                            animOffsetY.animateTo(
                                targetValue = animTargetValue[i],
                                animationSpec = animationSpec,
                            )
                        }
                    }
                    launch {
                        if (!paused) {
                            animOffsetX.animateTo(
                                targetValue = animTargetValue[i],
                                animationSpec = animationSpec,
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    Timber.e(e)
                }
            }

            PodcastCover(
                uuid = podcastUuid,
                coverWidth = (width * (0.5f + i * 0.05f)).toInt().pxToDp(context).dp,
                modifier = Modifier.offset {
                    IntOffset(
                        -(width * animOffsetX.value).roundToInt(),
                        (width * animOffsetY.value).roundToInt(),
                    )
                },
            )
        }
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
        context.resources,
    )
    val text = stringResource(
        id = R.string.end_of_year_story_longest_episode_title,
        timeText,
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
        story.longestEpisode.title,
        story.longestEpisode.podcastTitle,
    )
    StorySecondaryText(text = text, color = story.subtitleColor, modifier = modifier)
}
