package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.localization.helper.FriendlyDurationUnit
import au.com.shiftyjelly.pocketcasts.localization.helper.toFriendlyString
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.math.sqrt
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
    Box {
        Column(
            modifier = Modifier
                .capturable(controller.captureController(story))
                .fillMaxSize()
                .background(story.backgroundColor)
                .padding(top = measurements.closeButtonBottomEdge),
        ) {
            TopShowCover(
                story = story,
                measurements = measurements,
                controller = controller,
            )
            TopShowInfo(
                story = story,
                measurements = measurements,
                controller = controller,
                onShareStory = onShareStory,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(measurements.closeButtonBottomEdge)
                .background(story.backgroundColor),
        )
    }
}

@Composable
private fun ColumnScope.TopShowCover(
    story: Story.TopShow,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(story.backgroundColor),
    ) {
        PodcastImage(
            uuid = story.show.uuid,
            elevation = 0.dp,
            roundCorners = false,
            modifier = Modifier.requiredSize(maxOf(maxWidth, maxHeight)),
        )
        val transition = rememberInfiniteTransition(label = "transition")
        val rotation = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
            label = "rotation",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    val widthPx = density.run { maxWidth.toPx() }
                    val heightPx = density.run { maxHeight.toPx() }
                    val edgeSize = heightPx / sqrt(2f)
                    val dentSize = edgeSize / 20

                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(edgeSize / 2, dentSize)
                        lineTo(edgeSize, 0f)
                        lineTo(edgeSize - dentSize, edgeSize / 2)
                        lineTo(edgeSize, edgeSize)
                        lineTo(edgeSize / 2, edgeSize - dentSize)
                        lineTo(0f, edgeSize)
                        lineTo(dentSize, edgeSize / 2)
                        lineTo(0f, 0f)
                        close()
                    }

                    onDrawWithContent {
                        drawContent()

                        rotate(
                            degrees = rotation.value,
                            pivot = Offset(widthPx / 2, heightPx / 2),
                        ) {
                            translate(
                                left = (widthPx - edgeSize) / 2,
                                top = (heightPx - edgeSize) / 2,
                            ) {
                                drawPath(
                                    color = Color(0xFFFFFFFF),
                                    blendMode = BlendMode.DstOut,
                                    path = path,
                                )
                            }
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
        Image(
            painter = painterResource(IR.drawable.end_of_year_2024_sticker_8),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 8.dp)
                .size(
                    width = 215.dp * measurements.scale,
                    height = 103.dp * measurements.scale,
                ),
        )
    }
}

@Composable
private fun TopShowInfo(
    story: Story.TopShow,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Column(
        modifier = Modifier.background(story.backgroundColor),
    ) {
        TextH10(
            text = stringResource(
                R.string.end_of_year_story_top_podcast_title,
                2024,
            ),
            fontScale = measurements.smallDeviceFactor,
            disableAutoScale = true,
            color = colorResource(UR.color.coolgrey_90),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        val context = LocalContext.current
        TextP40(
            text = stringResource(
                R.string.end_of_year_story_top_podcast_subtitle,
                story.show.playedEpisodeCount,
                remember(story.show.playbackTime, context) {
                    story.show.playbackTime.toFriendlyString(
                        resources = context.resources,
                        maxPartCount = 3,
                        minUnit = FriendlyDurationUnit.Minute,
                    )
                },
                story.show.title,
            ),
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

@Preview(device = Devices.PortraitRegular)
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
