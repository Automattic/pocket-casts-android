package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.Story
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import kotlin.math.sqrt
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun TopShowStory(
    story: Story.TopShow,
    measurements: EndOfYearMeasurements,
) {
    Box {
        val shapeSize = measurements.width * 1.12f
        val coverSize = shapeSize * sqrt(2f)
        val coverOffset = measurements.closeButtonBottomEdge
        PodcastImage(
            uuid = story.show.uuid,
            elevation = 0.dp,
            roundCorners = false,
            modifier = Modifier
                .requiredSize(coverSize)
                .offset(y = coverOffset),
        )

        val transition = rememberInfiniteTransition(label = "transition")
        val rotation = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
            label = "rotation",
        )
        val shapeSizePx = LocalDensity.current.run { shapeSize.toPx() }
        val shapeOffsetPx = LocalDensity.current.run { (coverOffset * 0.6f + (coverSize - shapeSize) / 2).toPx() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    val path = Path().apply {
                        val dentSize = (shapeSizePx - size.width) / 2

                        moveTo(-dentSize, shapeOffsetPx)
                        lineTo(size.width / 2, (shapeOffsetPx) + dentSize)
                        lineTo(size.width + dentSize, shapeOffsetPx)
                        lineTo(size.width, shapeOffsetPx + shapeSizePx / 2)
                        lineTo(size.width + dentSize, shapeOffsetPx + shapeSizePx)
                        lineTo(size.width / 2, shapeOffsetPx + shapeSizePx - dentSize)
                        lineTo(-dentSize, shapeOffsetPx + shapeSizePx)
                        lineTo(0f, shapeOffsetPx + shapeSizePx / 2)
                        lineTo(-dentSize, shapeOffsetPx)

                        close()
                    }

                    onDrawWithContent {
                        drawContent()

                        rotate(
                            rotation.value,
                            pivot = Offset(x = size.width / 2, y = shapeOffsetPx + shapeSizePx / 2),
                        ) {
                            drawPath(
                                color = Color(0xFFFFFFFF),
                                blendMode = BlendMode.DstOut,
                                path = path,
                            )
                        }
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(story.backgroundColor),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            TextH10(
                text = stringResource(
                    R.string.end_of_year_story_top_podcast_title,
                    story.show.title,
                ),
                disableScale = true,
                color = colorResource(UR.color.coolgrey_90),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            TextP40(
                text = stringResource(
                    R.string.end_of_year_story_top_podcast_subtitle,
                    story.show.playedEpisodeCount,
                    StatsHelper.secondsToFriendlyString(
                        story.show.playbackTime.inWholeSeconds,
                        LocalContext.current.resources,
                    ),
                ),
                fontSize = 15.sp,
                disableScale = true,
                color = colorResource(UR.color.coolgrey_90),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            ShareStoryButton(onClick = {})
        }

        // Clip the rotating shape at top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(measurements.closeButtonBottomEdge)
                .background(story.backgroundColor),
        )

        // Fake sticker: lH66LwxxgG8btQ8NrM0ldx-fi-3070_23365#986383416
        val stickerWidth = 208.dp * measurements.scale
        val stickerHeight = 87.dp * measurements.scale
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = -stickerWidth / 3,
                    y = measurements.closeButtonBottomEdge + stickerHeight / 2,
                )
                .size(stickerWidth, stickerHeight)
                .background(Color.Black, shape = CircleShape),
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun TopShowPreview() {
    PreviewBox { measurements ->
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
        )
    }
}
