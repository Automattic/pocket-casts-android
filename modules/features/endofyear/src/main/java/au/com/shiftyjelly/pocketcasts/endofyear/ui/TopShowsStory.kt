package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.annotation.RawRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.random.Random
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TopShowsStory(
    story: Story.TopShows,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Column(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextH10(
            text = stringResource(LR.string.eoy_story_top_podcasts_title),
            fontScale = measurements.smallDeviceFactor,
            fontSize = 25.sp,
            disableAutoScale = true,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        val scrollState = rememberScrollState()
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(end = 24.dp)
                .fadeScrollingEdges(scrollState)
                .verticalScroll(scrollState),
        ) {
            story.shows.forEachIndexed { index, podcast ->
                AnimatedContainer(
                    animationRes = if (index % 2 == 0) IR.raw.playback_top_shows_wave_1_lottie else IR.raw.playback_top_shows_wave_2_lottie,
                    controller = controller,
                ) { scale ->
                    PodcastItem(
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            },
                        podcast = podcast,
                        index = index,
                        measurements = measurements,
                    )
                }
            }
        }
        ShareStoryButton(
            modifier = Modifier.padding(bottom = 18.dp),
            story = story,
            controller = controller,
            onShare = onShareStory,
        )
    }
}

@Composable
private fun AnimatedContainer(
    @RawRes animationRes: Int,
    controller: StoryCaptureController,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(Float) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        val composition by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(animationRes),
        )
        LottieAnimation(
            modifier = Modifier
                .height(94.dp)
                .widthIn(max = 76.dp),
            composition = composition,
            iterations = LottieConstants.IterateForever,
            contentScale = ContentScale.FillBounds,
        )

        var animationTrigger by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            animationTrigger = true
        }

        val scaleFactor by animateFloatAsState(
            targetValue = if (controller.isSharing || animationTrigger) {
                1f
            } else {
                .8f
            },
            label = "scaleAnimation",
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing,
            ),
        )

        content(scaleFactor)
    }
}

@Composable
private fun PodcastItem(
    podcast: TopPodcast,
    index: Int,
    measurements: EndOfYearMeasurements,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        TextP30(
            text = "#${index + 1}",
            disableAutoScale = true,
            color = colorResource(UR.color.coolgrey_90),
        )
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        val artworkSize = if (index == 0) 100.dp else 77.dp
        PodcastImage(
            uuid = podcast.uuid,
            elevation = 0.dp,
            cornerSize = 4.dp,
            modifier = Modifier.size(artworkSize * measurements.scale),
        )
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
        ) {
            TextP40(
                text = podcast.author,
                fontSize = 15.sp,
                fontScale = measurements.smallDeviceFactor,
                disableAutoScale = true,
                color = colorResource(UR.color.coolgrey_90),
                maxLines = 1,
            )
            TextP30(
                text = podcast.title,
                fontScale = measurements.smallDeviceFactor,
                disableAutoScale = true,
                color = colorResource(UR.color.coolgrey_90),
                maxLines = 2,
            )
        }
    }
}

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

@Preview(device = Devices.PORTRAIT_REGULAR)
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
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}
