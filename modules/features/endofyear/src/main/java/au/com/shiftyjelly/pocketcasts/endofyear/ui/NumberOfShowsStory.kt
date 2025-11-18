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
import androidx.compose.ui.zIndex
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
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
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

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 3,
            userScrollEnabled = false,
            pageSize = PageSize.Fixed(coverSize),
            pageSpacing = (-coverSize * 0.9f),
            contentPadding = PaddingValues(vertical = coverSize * 0.75f),
        ) { page ->
            val podcastId = podcastIds[page % podcastIds.size]

            val relativePosition = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
            val pageOffset = relativePosition.absoluteValue

            if (pageOffset <= 3f) {
                val scale = when {
                    pageOffset < 1f -> 1f - (pageOffset * 0.05f)
                    pageOffset < 2f -> 0.95f - ((pageOffset - 1f) * 0.05f)
                    pageOffset < 3f -> 0.9f - ((pageOffset - 2f) * 0.05f)
                    else -> 0.85f
                }

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
                        }
                        .zIndex(1f - pageOffset),
                )
            }
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
