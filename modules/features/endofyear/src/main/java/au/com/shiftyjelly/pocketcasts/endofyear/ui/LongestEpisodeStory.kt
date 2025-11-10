package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.localization.helper.FriendlyDurationUnit
import au.com.shiftyjelly.pocketcasts.localization.helper.toFriendlyString
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Story
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import dev.shreyaspatil.capturable.capturable
import java.io.File
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun LongestEpisodeStory(
    story: Story.LongestEpisode,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Column(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header(
            story = story,
            measurements = measurements,
        )
        Content(
            story = story,
            measurements = measurements,
            forceCoversVisible = controller.isSharing,
            modifier = Modifier.weight(1f),
        )
        Footer(
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
    measurements: EndOfYearMeasurements,
    forceCoversVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        val composition by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(IR.raw.playback_longest_episode_lottie)
        )
        LottieAnimation(
            composition = composition,
            modifier = Modifier.fillMaxWidth(),
        )

        PodcastImage(
            uuid = story.episode.podcastId,
            elevation = 0.dp,
            cornerSize = 4.dp,
            imageSize = 196.dp,
            modifier = modifier,
        )
    }
}

@Composable
private fun Header(
    story: Story.LongestEpisode,
    measurements: EndOfYearMeasurements,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
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
) {
    TextP40(
        text = stringResource(LR.string.end_of_year_story_longest_episode_share_text, story.episode.episodeTitle, story.episode.podcastTitle),
        textAlign = TextAlign.Center,
        disableAutoScale = true,
        fontScale = measurements.smallDeviceFactor,
        fontWeight = FontWeight.W500,
        color = colorResource(UR.color.white),
        modifier = Modifier.padding(horizontal = 24.dp),
    )
    ShareStoryButton(
        modifier = Modifier.padding(bottom = 18.dp),
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
