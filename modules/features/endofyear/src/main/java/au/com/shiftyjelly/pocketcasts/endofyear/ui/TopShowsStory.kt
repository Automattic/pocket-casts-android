package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun TopShowsStory(
    story: Story.TopShows,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) = TopShowsStory(
    story = story,
    measurements = measurements,
    onShareStory = onShareStory,
    controller = controller,
    initialAnimationProgress = 0f,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TopShowsStory(
    story: Story.TopShows,
    measurements: EndOfYearMeasurements,
    initialAnimationProgress: Float,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Column(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge + 16.dp),
    ) {
        val animationProgress = remember { Animatable(initialAnimationProgress) }
        LaunchedEffect(Unit) {
            delay(350.milliseconds)
            while (isActive) {
                if (animationProgress.value == 0f) {
                    animationProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = 75f,
                            visibilityThreshold = 0.01f,
                        ),
                    )
                    delay(4.seconds)
                } else {
                    animationProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 400f,
                            visibilityThreshold = 0.01f,
                        ),
                    )
                    delay(1200.milliseconds)
                }
            }
        }

        val scrollState = rememberScrollState()
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
                .fadeScrollingEdges(scrollState)
                .verticalScroll(scrollState),
        ) {
            story.shows.forEachIndexed { index, podcast ->
                PodcastItem(
                    podcast = podcast,
                    index = index,
                    measurements = measurements,
                    animationProgress = if (controller.isSharing) 1f else animationProgress.value,
                )
            }
        }
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        Column {
            TextH10(
                text = stringResource(LR.string.eoy_story_top_podcasts_title),
                fontScale = measurements.smallDeviceFactor,
                disableAutoScale = true,
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
private fun PodcastItem(
    podcast: TopPodcast,
    index: Int,
    measurements: EndOfYearMeasurements,
    animationProgress: Float,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp * measurements.scale),
    ) {
        TextH20(
            text = "#${index + 1}",
            disableAutoScale = true,
            color = colorResource(UR.color.coolgrey_90),
            modifier = Modifier
                .offset { IntOffset(x = (50.dp * (1f - animationProgress)).roundToPx(), y = 0) }
                .alpha(animationProgress),
        )
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp * measurements.scale)
                .scale(animationProgress)
                .alpha(animationProgress),
        ) {
            Image(
                painter = painterResource(stickers[index % stickers.size]),
                contentDescription = null,
            )
            PodcastImage(
                uuid = podcast.uuid,
                elevation = 0.dp,
                cornerSize = 4.dp,
                modifier = Modifier.size(72.dp * measurements.scale),
            )
        }
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .offset { IntOffset(x = -(50.dp * (1f - animationProgress)).roundToPx(), y = 0) }
                .fillMaxSize()
                .alpha(animationProgress),
        ) {
            TextP40(
                text = podcast.author,
                fontSize = 15.sp,
                fontScale = measurements.smallDeviceFactor,
                disableAutoScale = true,
                color = colorResource(UR.color.coolgrey_90),
                maxLines = 1,
            )
            TextH20(
                text = podcast.title,
                fontScale = measurements.smallDeviceFactor,
                disableAutoScale = true,
                color = colorResource(UR.color.coolgrey_90),
                maxLines = 2,
            )
        }
    }
}

private val stickers = listOf(
    IR.drawable.end_of_year_2024_sticker_3,
    IR.drawable.end_of_year_2024_sticker_4,
    IR.drawable.end_of_year_2024_sticker_5,
)

private fun Modifier.fadeScrollingEdges(
    scrollState: ScrollState,
) = then(
    Modifier
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()

            val viewPortHeight = scrollState.viewportSize.toFloat()
            if (!scrollState.canScrollBackward && !scrollState.canScrollForward) {
                return@drawWithContent
            }
            val scrollFactor = when (val maxValue = scrollState.maxValue) {
                0, Int.MAX_VALUE -> 1f
                else -> (scrollState.value.toFloat() / maxValue).let { it * it }
            }

            val topFadeFactor = if (scrollFactor < 0.2) {
                (45 * scrollFactor * scrollFactor - 14 * scrollFactor + 1).coerceIn(0f, 1f)
            } else {
                0f
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent.copy(alpha = topFadeFactor),
                        0.1f to Color.Black,
                    ),
                    startY = 0f,
                    endY = viewPortHeight,
                ),
                blendMode = BlendMode.DstIn,
            )

            val bottomFadeFactor = if (scrollFactor > 0.8) {
                (-45 * scrollFactor * scrollFactor + 86 * scrollFactor - 40).coerceIn(0f, 1f)
            } else {
                0f
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent.copy(alpha = bottomFadeFactor),
                        0.1f to Color.Black,
                    ),
                    startY = viewPortHeight,
                    endY = 0f,
                ),
                blendMode = BlendMode.DstIn,
            )
        },
)

@Preview(device = Devices.PortraitRegular)
@Composable
private fun TopShowsPreview() {
    PreviewBox(currentPage = 3) { measurements ->
        TopShowsStory(
            story = Story.TopShows(
                shows = List(5) { index ->
                    TopPodcast(
                        uuid = "podcast-id-$index",
                        title = if (index == 1) {
                            "A long long long loooooooooong long title"
                        } else {
                            "Podcast Title $index"
                        },
                        author = if (index == 1) {
                            "A long long long loooooooooong long author"
                        } else {
                            "Podcast Author $index"
                        },
                        playbackTimeSeconds = Random.nextDouble(50_000.0, 150_000.0),
                        playedEpisodeCount = Random.nextInt(60, 100),
                    )
                },
                podcastListUrl = null,
            ),
            measurements = measurements,
            initialAnimationProgress = 1f,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}
