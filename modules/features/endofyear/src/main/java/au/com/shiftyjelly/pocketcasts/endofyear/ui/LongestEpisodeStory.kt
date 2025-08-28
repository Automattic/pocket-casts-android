package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImageDeprecated
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.localization.helper.FriendlyDurationUnit
import au.com.shiftyjelly.pocketcasts.localization.helper.toFriendlyString
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Story
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun LongestEpisodeStory(
    story: Story.LongestEpisode,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) = LongestEpisodeStory(
    story = story,
    measurements = measurements,
    controller = controller,
    areCoversVisible = false,
    onShareStory = onShareStory,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LongestEpisodeStory(
    story: Story.LongestEpisode,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    areCoversVisible: Boolean,
    onShareStory: (File) -> Unit,
) {
    Column(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge),
    ) {
        CoversSection(
            story = story,
            measurements = measurements,
            areCoversVisible = areCoversVisible,
            forceCoversVisible = controller.isSharing,
            modifier = Modifier.weight(1f),
        )
        TextInfo(
            story = story,
            measurements = measurements,
            controller = controller,
            onShareStory = onShareStory,
        )
    }
}

@Composable
private fun CoversSection(
    story: Story.LongestEpisode,
    measurements: EndOfYearMeasurements,
    areCoversVisible: Boolean,
    forceCoversVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    var areVisible by remember { mutableStateOf(areCoversVisible) }
    LaunchedEffect(Unit) {
        delay(200)
        areVisible = true
    }

    val transition = updateTransition(areVisible, "cover-transtion")
    val widthPx = LocalDensity.current.run { measurements.width.roundToPx() }
    val offset by transition.animateIntOffset(
        transitionSpec = {
            spring(
                stiffness = 75f,
                dampingRatio = 0.6f,
                visibilityThreshold = IntOffset(1, 1),
            )
        },
        targetValueByState = {
            when (it) {
                true -> IntOffset.Zero
                false -> IntOffset(-widthPx / 2, widthPx / 2)
            }
        },
    )
    val scale by transition.animateFloat(
        transitionSpec = {
            spring(
                stiffness = 75f,
                dampingRatio = 0.6f,
                visibilityThreshold = 0.001f,
            )
        },
        targetValueByState = {
            when (it) {
                true -> 1f
                false -> 0.0f
            }
        },
    )

    val coverOffset = 19.dp
    val baseCoverSize = if (measurements.width > 380.dp) 280.dp else 240.dp
    val podcastCoverSizes = List(5) { it }.runningFold(baseCoverSize) { dp, _ -> dp * 0.9f }.reversed()

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier
                .offset(y = baseCoverSize * measurements.scale / 3)
                .offset { if (forceCoversVisible) IntOffset.Zero else offset }
                .scale(if (forceCoversVisible) 1f else scale),
        ) {
            podcastCoverSizes.forEachIndexed { index, size ->
                PodcastCover(
                    episode = story.episode,
                    index = index,
                    modifier = Modifier
                        .offset(
                            x = coverOffset * (index - 1) * measurements.scale,
                            y = -coverOffset * index * measurements.scale,
                        )
                        .size(size * measurements.scale),
                )
            }
        }
        val stickerSize = DpSize(194.dp, 135.dp) * measurements.scale
        val baseStickerOffset = coverOffset * (podcastCoverSizes.size - 1) * measurements.scale
        val xFactor = if (measurements.width > 380.dp) 2f else 2.5f
        val yFactor = if (measurements.width > 380.dp) 3f else 4f
        Image(
            painter = painterResource(IR.drawable.end_of_year_2024_sticker_7),
            contentDescription = null,
            modifier = Modifier
                .offset(
                    x = baseStickerOffset + stickerSize.width / xFactor,
                    y = -baseStickerOffset * measurements.scale - stickerSize.height / yFactor,
                )
                .size(stickerSize),
        )
    }
}

private val previewColors = listOf(
    Color.Black,
    Color.White,
    Color.Red,
    Color.Blue,
    Color.Green,
    Color.Gray,
)

@Composable
private fun PodcastCover(
    episode: LongestEpisode,
    index: Int,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        @Suppress("DEPRECATION")
        PodcastImageDeprecated(
            uuid = episode.podcastId,
            elevation = 0.dp,
            cornerSize = 8.dp,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(
                color = previewColors[index % previewColors.size],
                shape = RoundedCornerShape(8.dp),
            ),
        )
    }
}

@Composable
private fun TextInfo(
    story: Story.LongestEpisode,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Column(
        modifier = Modifier.background(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.1f to story.backgroundColor,
            ),
        ),
    ) {
        Spacer(
            modifier = Modifier.height(16.dp),
        )
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
            fontScale = measurements.smallDeviceFactor,
            disableAutoScale = true,
            color = colorResource(UR.color.coolgrey_90),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP40(
            text = stringResource(
                LR.string.end_of_year_story_longest_episode_subtitle,
                story.episode.episodeTitle,
                story.episode.podcastTitle,
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
            areCoversVisible = true,
            onShareStory = {},
        )
    }
}
