package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollSpeed
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollingRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.models.to.Story
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NumberOfShowsStory(
    story: Story.NumberOfShows,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Box(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge),
    ) {
        TextH10(
            text = stringResource(
                LR.string.end_of_year_story_listened_to_numbers,
                story.showCount,
                story.epsiodeCount,
            ),
            fontSize = 25.sp,
            lineHeight = 30.sp,
            disableAutoScale = true,
            textAlign = TextAlign.Center,
            fontScale = measurements.smallDeviceFactor,
            color = colorResource(UR.color.white),
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        val composition by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(R.raw.playback_number_of_shows_lottie),
        )
        val isPreview = LocalInspectionMode.current
        val freezeAnimation = controller.isSharing || isPreview

        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            isPlaying = !freezeAnimation,
        )

        LottieAnimation(
            composition = composition,
            progress = { if (freezeAnimation) 1f else progress },
            modifier = Modifier
                .matchParentSize()
                .scale(1.2f),
            contentScale = ContentScale.FillWidth,
        )

        PodcastCoverCarousel(
            podcastIds = story.bottomShowIds,
            coverSize = 260.dp,
            modifier = Modifier.align(alignment = Alignment.Center),
        )
        ShareStoryButton(
            modifier = Modifier
                .padding(bottom = 18.dp)
                .align(alignment = Alignment.BottomCenter),
            story = story,
            controller = controller,
            onShare = onShareStory,
        )
    }
}


@Composable
private fun PodcastCoverCarousel(
    podcastIds: List<String>,
    coverSize: Dp,
    modifier: Modifier = Modifier,
) {
    ScrollingRow(
        items = podcastIds,
        scrollSpeed = ScrollSpeed(300.dp, 10.seconds),
        modifier = modifier,
    ) { podcastId ->
        PodcastImage(
            uuid = podcastId,
            cornerSize = 4.dp,
            modifier = Modifier.size(coverSize),
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun NumberOfShowsPreview() {
    PreviewBox(currentPage = 1) { measurements ->
        NumberOfShowsStory(
            story = Story.NumberOfShows(
                showCount = 20,
                episodeCount = 125,
                topShowIds = List(4) { "id-$it" },
                bottomShowIds = List(4) { "id-$it" },
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}
