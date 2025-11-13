package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.adaptive.isAtMostMediumHeight
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.localization.helper.FriendlyDurationUnit
import au.com.shiftyjelly.pocketcasts.localization.helper.toFriendlyString
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Story
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import dev.shreyaspatil.capturable.capturable
import java.io.File
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val SMALL_SCREEN_SIZE_FACTOR = .6f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun LongestEpisodeStory(
    story: Story.LongestEpisode,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge),
    ) {
        val windowSize = currentWindowAdaptiveInfo().windowSizeClass
        val sizeFactor = if (windowSize.isAtMostMediumHeight()) {
            SMALL_SCREEN_SIZE_FACTOR
        } else {
            1f
        }
        val animationContainerSize = min(maxWidth, maxHeight) * sizeFactor
        Header(
            story = story,
            measurements = measurements,
            modifier = Modifier
                .fillMaxWidth()
                .height((maxHeight - animationContainerSize) / 2)
                .align(Alignment.TopCenter),
        )
        val artworkSize = 196.dp * sizeFactor
        Content(
            story = story,
            forceVisible = controller.isSharing,
            artworkSize = artworkSize,
            modifier = Modifier
                .size(animationContainerSize)
                .align(Alignment.Center),
        )
        Footer(
            modifier = Modifier
                .fillMaxWidth()
                .height((maxHeight - animationContainerSize) / 2)
                .align(Alignment.BottomCenter),
            story = story,
            controller = controller,
            onShareStory = onShareStory,
            measurements = measurements,
        )
    }
}

@Composable
private fun Content(
    story: Story.LongestEpisode,
    artworkSize: Dp,
    forceVisible: Boolean,
    modifier: Modifier = Modifier,
) = BoxWithConstraints(
    modifier = modifier,
    contentAlignment = Alignment.Center,
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.playback_longest_episode_lottie),
    )
    val isPreview = LocalInspectionMode.current
    val freezeAnimation = forceVisible || isPreview

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !freezeAnimation,
    )
    val hasAnimationStarted = progress > 0f

    LottieAnimation(
        composition = composition,
        progress = { if (freezeAnimation) 1f else progress },
        modifier = Modifier
            .matchParentSize(),
        contentScale = ContentScale.FillWidth,
    )
    var artworkTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(hasAnimationStarted, freezeAnimation) {
        artworkTrigger = freezeAnimation || hasAnimationStarted
    }

    val artworkTransition = updateTransition(artworkTrigger, "artwork transition")
    val scaleAnimation by artworkTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 800, easing = FastOutSlowInEasing)
        },
    ) {
        if (it) {
            1f
        } else {
            1.1f
        }
    }
    val alphaAnimation by artworkTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 100, easing = LinearEasing)
        },
    ) {
        if (it) {
            1f
        } else {
            0f
        }
    }

    PodcastImage(
        uuid = story.episode.podcastId,
        elevation = 0.dp,
        cornerSize = 4.dp,
        modifier = Modifier
            .requiredSize(artworkSize)
            .scale(scaleAnimation)
            .offset(y = -maxHeight * .2f)
            .graphicsLayer {
                alpha = alphaAnimation
            },
    )
}

@Composable
private fun Header(
    story: Story.LongestEpisode,
    measurements: EndOfYearMeasurements,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val context = LocalContext.current
        TextH10(
            text = stringResource(
                LR.string.end_of_year_story_longest_episode_title,
                remember(story.episode.duration, context) {
                    story.episode.duration.toFriendlyString(
                        resources = context.resources,
                        minUnit = FriendlyDurationUnit.Minute,
                    )
                },
            ),
            fontSize = 25.sp,
            textAlign = TextAlign.Center,
            fontScale = measurements.smallDeviceFactor,
            disableAutoScale = true,
            color = colorResource(UR.color.white),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        TextP40(
            text = stringResource(
                LR.string.end_of_year_story_longest_episode_subtitle,
                story.episode.episodeTitle,
                story.episode.podcastTitle,
            ),
            disableAutoScale = true,
            color = colorResource(UR.color.white),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun Footer(
    story: Story.LongestEpisode,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
    measurements: EndOfYearMeasurements,
    modifier: Modifier = Modifier,
) = Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        TextP40(
            text = stringResource(LR.string.end_of_year_story_longest_episode_share_text, story.episode.episodeTitle, story.episode.podcastTitle),
            textAlign = TextAlign.Center,
            disableAutoScale = true,
            fontScale = measurements.smallDeviceFactor,
            fontWeight = FontWeight.W500,
            color = colorResource(UR.color.white),
            modifier = Modifier
                .padding(horizontal = 24.dp),
        )
    }
    ShareStoryButton(
        modifier = Modifier
            .padding(bottom = 18.dp),
        story = story,
        controller = controller,
        onShare = onShareStory,
    )
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun LongestEpisodePreview() {
    PreviewBox(currentPage = 6) { measurements ->
        LongestEpisodeStory(
            story = Story.LongestEpisode(
                episode = LongestEpisode(
                    episodeId = "episode-id",
                    episodeTitle = "Episode Title",
                    podcastId = "podcast-id",
                    podcastTitle = "Podcast Title",
                    durationSeconds = 9250.0,
                    coverUrl = null,
                ),
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}
