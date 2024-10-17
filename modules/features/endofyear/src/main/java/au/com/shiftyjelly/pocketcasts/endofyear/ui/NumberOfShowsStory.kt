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
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollDirection
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollingRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.Story
import au.com.shiftyjelly.pocketcasts.localization.R
import kotlin.math.roundToLong
import kotlin.math.tan
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun NumberOfShowsStory(
    story: Story.NumberOfShows,
    measurements: EndOfYearMeasurements,
) {
    val smallCoverSize = 160.dp * measurements.scale
    val smallSpacingSize = smallCoverSize / 10
    val largeCoverSize = 200.dp * measurements.scale
    val largeSpacingSize = smallCoverSize / 10
    val carouselRotationOffset = (measurements.width / 1.5f) * tan(StoryRotationRadians)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .rotate(StoryRotationDegrees)
                .offset(y = measurements.closeButtonBottomEdge + 8.dp - carouselRotationOffset)
                .requiredWidth(measurements.width * 1.5f), // Increase the size to account for rotation
        ) {
            PodcastCoverCarousel(
                podcastIds = story.topShowIds,
                scrollDirection = ScrollDirection.Left,
                coverSize = smallCoverSize,
                coverElevation = 0.dp,
                spacingSize = smallSpacingSize,
            )
            PodcastCoverCarousel(
                podcastIds = story.bottomShowIds,
                scrollDirection = ScrollDirection.Right,
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
                disableScale = true,
                color = colorResource(UR.color.coolgrey_90),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            TextP40(
                text = stringResource(R.string.end_of_year_story_listened_to_numbers_subtitle),
                fontSize = 15.sp,
                disableScale = true,
                color = colorResource(UR.color.coolgrey_90),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            ShareStoryButton(onClick = {})
        }
    }
}

@Composable
private fun PodcastCoverCarousel(
    podcastIds: List<String>,
    scrollDirection: ScrollDirection,
    coverSize: Dp,
    coverElevation: Dp,
    spacingSize: Dp,
    modifier: Modifier = Modifier,
) {
    ScrollingRow(
        items = podcastIds,
        scrollDirection = scrollDirection,
        scrollByPixels = 1f,
        scrollDelay = { (60 / it.density).roundToLong().coerceAtLeast(4L) },
        horizontalArrangement = Arrangement.spacedBy(spacingSize),
        modifier = modifier,
    ) { podcastId ->
        PodcastImage(
            uuid = podcastId,
            elevation = coverElevation,
            cornerSize = 4.dp,
            modifier = Modifier.size(coverSize),
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun NumberOfShowsPreview() {
    PreviewBox { measurements ->
        NumberOfShowsStory(
            story = Story.NumberOfShows(
                showCount = 20,
                epsiodeCount = 125,
                topShowIds = List(4) { "id-$it" },
                bottomShowIds = List(4) { "id-$it" },
            ),
            measurements = measurements,
        )
    }
}
