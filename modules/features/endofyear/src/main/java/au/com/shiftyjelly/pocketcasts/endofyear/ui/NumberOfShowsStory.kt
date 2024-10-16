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
import androidx.compose.foundation.shape.CircleShape
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
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollDirection
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollingRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.endofyear.Story
import au.com.shiftyjelly.pocketcasts.localization.R
import kotlin.math.tan
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun NumberOfShowsStory(
    story: Story.NumberOfShows,
    measurements: EndOfYearMeasurements,
) {
    val coverSize = 160.dp * measurements.scale
    val spacingSize = coverSize / 10
    val carouselHeight = coverSize * 2 + spacingSize
    val carouselRotationOffset = (measurements.width / 2) * tan(StoryRotationRadians)

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
                coverSize = coverSize,
                spacingSize = spacingSize,
            )
            Spacer(
                modifier = Modifier.height(spacingSize),
            )
            PodcastCoverCarousel(
                podcastIds = story.bottomShowIds,
                scrollDirection = ScrollDirection.Right,
                coverSize = coverSize,
                spacingSize = spacingSize,
            )
        }

        // Fake sticker: lH66LwxxgG8btQ8NrM0ldx-fi-3070_28391#986464596
        val stickerWidth = 214.dp * measurements.scale
        val stickerHeight = 112.dp * measurements.scale
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = stickerWidth / 3,
                    y = measurements.closeButtonBottomEdge + 8.dp + carouselHeight,
                )
                .size(stickerWidth, stickerHeight)
                .background(Color.Black, shape = CircleShape),
        )

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
                fontSize = 31.nonScaledSp,
                color = colorResource(UR.color.coolgrey_90),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            TextP40(
                text = stringResource(R.string.end_of_year_story_listened_to_numbers_subtitle),
                fontSize = 15.nonScaledSp,
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
    spacingSize: Dp,
) {
    ScrollingRow(
        items = podcastIds,
        scrollDirection = scrollDirection,
        scrollByPixels = 2f,
        horizontalArrangement = Arrangement.spacedBy(spacingSize),
    ) { podcastId ->
        PodcastImage(
            uuid = podcastId,
            elevation = 0.dp,
            cornerSize = 4.dp,
            modifier = Modifier.size(coverSize),
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
fun NumberOfShowsPreview() {
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
