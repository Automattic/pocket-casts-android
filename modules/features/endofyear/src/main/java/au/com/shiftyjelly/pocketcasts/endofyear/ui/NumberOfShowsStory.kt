package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
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
    val pagerState = rememberPagerState(
        initialPage = Int.MAX_VALUE / 2,
        pageCount = { Int.MAX_VALUE },
    )

    // Auto-scroll effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // Wait 1 second before next scroll
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    Box(
        modifier = modifier.height(coverSize * 2.5f), // Taller to show 3 above + center + 3 below
        contentAlignment = Alignment.Center,
    ) {
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 3,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
            pageSize = PageSize.Fixed(coverSize), // Tight spacing for stacked effect
            pageSpacing = (-coverSize * 0.9f), // Negative spacing for 0.1f peek
            contentPadding = PaddingValues(vertical = coverSize * 0.75f) // Center the current page
        ) { page ->
            val podcastId = podcastIds[page % podcastIds.size]

            // Calculate relative position from current page (negative = above, positive = below)
            val relativePosition = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
            val pageOffset = relativePosition.absoluteValue

            // Aggressive scale for compact carousel effect
            // Center: 1.0f, Adjacent: 0.75f, Distance 2: 0.65f, Distance 3+: 0.6f
            val scale = when {
                pageOffset < 1f -> 1f - (pageOffset * 0.25f) // Interpolate from 1.0 to 0.75
                pageOffset < 2f -> 0.75f - ((pageOffset - 1f) * 0.1f) // Interpolate from 0.75 to 0.65
                pageOffset < 3f -> 0.65f - ((pageOffset - 2f) * 0.05f) // Interpolate from 0.65 to 0.6
                else -> 0.6f
            }

            // Subtle horizontal offset for depth
            val translationX = pageOffset * 5f

            PodcastImage(
                uuid = podcastId,
                cornerSize = 4.dp,
                modifier = Modifier
                    .size(coverSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.translationX = translationX
                        shadowElevation = (1f - pageOffset.coerceAtMost(1f)) * 16f
                    }
                    .zIndex(1f - pageOffset), // Center item on top, others layered behind
            )
        }
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
