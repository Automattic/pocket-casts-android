package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TopShowStory(
    story: Story.TopShow,
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
        val animationContainerSize = min(maxWidth, maxHeight)
        Header(
            measurements = measurements,
            modifier = Modifier
                .fillMaxWidth()
                .height((maxHeight - animationContainerSize.times(1.3f)) / 2)
                .align(Alignment.TopCenter)
        )

        BoxWithConstraints(
            modifier = Modifier
                .size(animationContainerSize)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(IR.raw.playback_story_top_podcast_lottie)
            )
            LottieAnimation(
                composition = composition,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.3f
                        scaleY = 1.3f
                    },
                contentScale = ContentScale.Fit,
            )
            PodcastImage(
                uuid = story.show.uuid,
                elevation = 0.dp,
                modifier = Modifier.requiredSize(maxOf(maxWidth.times(.7f), maxHeight.times(.7f))),
            )
        }

        Footer(
            story = story,
            controller = controller,
            onShareStory = onShareStory,
            modifier = Modifier
                .fillMaxWidth()
                .height((maxHeight - animationContainerSize.times(1.3f)) / 2)
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun Header(
    measurements: EndOfYearMeasurements,
    modifier: Modifier = Modifier
) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
) {
    TextH10(
        text = stringResource(
            R.string.end_of_year_story_top_podcast_title,
            2025,
        ),
        fontSize = 25.sp,
        fontScale = measurements.smallDeviceFactor,
        disableAutoScale = true,
        color = colorResource(UR.color.white),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        textAlign = TextAlign.Center,
    )
    TextP40(
        text = stringResource(R.string.end_of_year_story_top_podcast_subtitle),
        disableAutoScale = true,
        color = colorResource(UR.color.white),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Footer(
    story: Story.TopShow,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceAround,
) {
    TextP40(
        text = stringResource(
            R.string.end_of_year_story_top_podcast_stats,
            story.show.playedEpisodeCount,
            story.show.playbackTime.inWholeDays.toInt(),
            (story.show.playbackTime.minus(story.show.playbackTime.inWholeDays.days)).inWholeHours
        ),
        disableAutoScale = true,
        color = colorResource(UR.color.white),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        textAlign = TextAlign.Center,
    )
    ShareStoryButton(
        story = story,
        controller = controller,
        onShare = onShareStory,
    )
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun TopShowPreview() {
    PreviewBox(currentPage = 2) { measurements ->
        TopShowStory(
            story = Story.TopShow(
                show = TopPodcast(
                    uuid = "podcast-id",
                    title = "Podcast Title",
                    author = "Podcast Author",
                    playbackTimeSeconds = 200_250.0,
                    playedEpisodeCount = 87,
                ),
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}
