package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.time.Duration.Companion.days
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val ANIMATION_START_SCALE = 1.8f
private const val ANIMATION_END_SCALE = 1.3f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TopShowStory(
    story: Story.TopShow,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Box {
        BoxWithConstraints(
            modifier = Modifier
                .capturable(controller.captureController(story))
                .fillMaxSize()
                .background(story.backgroundColor)
                .padding(top = measurements.closeButtonBottomEdge + 16.dp, bottom = 64.dp),
        ) {
            var headerHeight by remember { mutableStateOf(0.dp) }
            var footerHeight by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            Header(
                measurements = measurements,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        with(density) { headerHeight = size.height.toDp() }
                    }
                    .align(Alignment.TopCenter),
            )

            Footer(
                story = story,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        with(density) { footerHeight = size.height.toDp() }
                    }
                    .align(Alignment.BottomCenter),
            )

            val contentHeight = maxHeight - headerHeight - footerHeight
            // reduce the size to account for the growth during the animation
            val contentHeightMinusGrowth = contentHeight / ANIMATION_END_SCALE

            CenterContent(
                story = story,
                size = contentHeightMinusGrowth,
                modifier = Modifier
                    .size(contentHeightMinusGrowth)
                    .align(Alignment.Center),
            )
        }
        ShareStoryButton(
            modifier = Modifier.padding(bottom = 18.dp).align(Alignment.BottomCenter),
            story = story,
            controller = controller,
            onShare = onShareStory,
        )
    }
}

@Composable
private fun CenterContent(
    story: Story.TopShow,
    size: Dp,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.playback_top_show_lottie),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
    )
    val isPlaying = progress > 0f
    val animationSpec = tween<Float>(
        durationMillis = 1000,
        easing = CubicBezierEasing(.33f, 0f, 0f, 1f),
    )

    val lottieScaleAnimation by animateFloatAsState(
        targetValue = if (isPlaying) ANIMATION_END_SCALE else ANIMATION_START_SCALE,
        animationSpec = animationSpec,
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                scaleX = lottieScaleAnimation
                scaleY = lottieScaleAnimation
            },
        contentScale = ContentScale.Fit,
    )
    var artworkTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(isPlaying) {
        artworkTrigger = isPlaying
    }

    val artworkTransition = updateTransition(artworkTrigger, "artwork transition")
    val scaleAnimation by artworkTransition.animateFloat(
        transitionSpec = {
            animationSpec
        },
    ) {
        if (it) {
            .95f
        } else {
            1f
        }
    }
    val alphaAnimation by artworkTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 100, easing = LinearEasing)
        },
    ) {
        if (it) {
            1f
        } else {
            0f
        }
    }

    PodcastImage(
        uuid = story.show.uuid,
        elevation = 0.dp,
        contentDescription = story.show.title,
        imageSize = size,
        modifier = Modifier
            .requiredSize(size.times(.7f))
            .scale(scaleAnimation)
            .graphicsLayer {
                alpha = alphaAnimation
            },
    )
}

@Composable
private fun Header(
    measurements: EndOfYearMeasurements,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.semantics(mergeDescendants = true) {},
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    TextH10(
        text = stringResource(
            LR.string.end_of_year_story_top_podcast_title,
            2025,
        ),
        fontSize = 25.sp,
        lineHeight = 30.sp,
        fontScale = measurements.smallDeviceFactor,
        disableAutoScale = true,
        color = colorResource(UR.color.white),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        textAlign = TextAlign.Center,
    )
    TextP40(
        text = stringResource(LR.string.end_of_year_story_top_podcast_subtitle),
        disableAutoScale = true,
        color = colorResource(UR.color.white),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.W500,
    )
}

@Composable
private fun Footer(
    story: Story.TopShow,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier,
    contentAlignment = Alignment.BottomCenter,
) {
    val formattedEpisodeCount = pluralStringResource(LR.plurals.episodes, story.show.playedEpisodeCount, story.show.playedEpisodeCount)
    val numberOfDays = story.show.playbackTime.inWholeDays
    val formattedDaysCount = numberOfDays.toString() + " " + pluralStringResource(LR.plurals.day, numberOfDays.toInt())
    val remainingHours = story.show.playbackTime.minus(numberOfDays.days).inWholeHours
    val formattedHoursCount = remainingHours.toString() + " " + pluralStringResource(LR.plurals.hour, remainingHours.toInt())
    TextP40(
        text = if (numberOfDays > 0) {
            stringResource(
                id = LR.string.end_of_year_story_top_podcast_stats_days,
                formattedEpisodeCount,
                formattedDaysCount,
                formattedHoursCount,
            )
        } else {
            stringResource(
                id = LR.string.end_of_year_story_top_podcast_stats_hours,
                formattedEpisodeCount,
                formattedHoursCount,
            )
        },
        disableAutoScale = true,
        color = colorResource(UR.color.white),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        textAlign = TextAlign.Center,
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
