package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDirection
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImageDeprecated
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollSpeed
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollingRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.to.Story
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.math.tan
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NumberOfShowsStory(
    story: Story.NumberOfShows,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    val smallCoverSize = 160.dp * measurements.scale
    val smallSpacingSize = smallCoverSize / 10
    val largeCoverSize = 200.dp * measurements.scale
    val largeSpacingSize = smallCoverSize / 10
    val carouselRotationOffset = (measurements.width / 1.5f) * tan(STORY_ROTATION_RADIANS)

    Box(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .rotate(STORY_ROTATION_DEGREES)
                .offset(y = measurements.closeButtonBottomEdge + 8.dp - carouselRotationOffset)
                .requiredWidth(measurements.width * 1.5f), // Increase the size to account for rotation
        ) {
            PodcastCoverCarousel(
                podcastIds = story.topShowIds,
                scrollDirection = HorizontalDirection.Left,
                coverSize = smallCoverSize,
                coverElevation = 0.dp,
                spacingSize = smallSpacingSize,
            )
            PodcastCoverCarousel(
                podcastIds = story.bottomShowIds,
                scrollDirection = HorizontalDirection.Right,
                coverSize = largeCoverSize,
                coverElevation = 12.dp,
                spacingSize = largeSpacingSize,
                modifier = Modifier.offset(y = -smallCoverSize / 6),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.1f to story.backgroundColor,
                    ),
                ),
        ) {
            TextH10(
                text = stringResource(
                    R.string.end_of_year_story_listened_to_numbers,
                    story.showCount,
                    story.epsiodeCount,
                ),
                disableAutoScale = true,
                fontScale = measurements.smallDeviceFactor,
                color = colorResource(UR.color.coolgrey_90),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            TextP40(
                text = stringResource(R.string.end_of_year_story_listened_to_numbers_subtitle),
                fontSize = 15.sp,
                disableAutoScale = true,
                color = colorResource(UR.color.coolgrey_90),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            ShareStoryButton(
                story = story,
                controller = controller,
                onShare = onShareStory,
            )
        }
    }
}

@Composable
private fun PodcastCoverCarousel(
    podcastIds: List<String>,
    scrollDirection: HorizontalDirection,
    coverSize: Dp,
    coverElevation: Dp,
    spacingSize: Dp,
    modifier: Modifier = Modifier,
) {
    ScrollingRow(
        items = podcastIds,
        scrollDirection = scrollDirection,
        scrollSpeed = ScrollSpeed(300.dp, 10.seconds),
        horizontalArrangement = Arrangement.spacedBy(spacingSize),
        modifier = modifier,
    ) { podcastId ->
        @Suppress("DEPRECATION")
        PodcastImageDeprecated(
            uuid = podcastId,
            elevation = coverElevation,
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
                epsiodeCount = 125,
                topShowIds = List(4) { "id-$it" },
                bottomShowIds = List(4) { "id-$it" },
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}
