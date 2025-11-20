package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
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
import au.com.shiftyjelly.pocketcasts.compose.adaptive.isAtMostMediumHeight
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
import kotlin.time.Duration.Companion.milliseconds
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
            .padding(top = measurements.closeButtonBottomEdge + 16.dp),
    ) {
        val windowSize = currentWindowAdaptiveInfo().windowSizeClass
        TextH10(
            text = stringResource(
                LR.string.end_of_year_story_listened_to_numbers,
                story.showCount,
                story.episodeCount,
            ),
            fontSize = 25.sp,
            lineHeight = 30.sp,
            disableAutoScale = true,
            textAlign = TextAlign.Center,
            fontScale = measurements.smallDeviceFactor,
            color = colorResource(UR.color.white),
            modifier = Modifier.padding(
                horizontal = if (windowSize.isAtMostMediumHeight()) {
                    24.dp
                } else {
                    42.dp
                },
            ),
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

        val coverSize = if (windowSize.isAtMostMediumHeight()) {
            180.dp
        } else {
            260.dp
        }
        PodcastCoverCarousel(
            podcastIds = story.randomShowIds,
            coverSize = coverSize,
            modifier = Modifier.align(alignment = Alignment.Center),
            freezeAnimation = controller.isSharing,
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

private const val PAGE_COUNT = Int.MAX_VALUE
private const val SCROLL_ANIM_DURATION_MS = 650
private val SCROLL_INTERVAL = 700.milliseconds
private val ANIMATION_CURVE = CubicBezierEasing(.9f, 0f, .08f, 1f)

@Composable
private fun PodcastCoverCarousel(
    podcastIds: List<String>,
    coverSize: Dp,
    modifier: Modifier = Modifier,
    freezeAnimation: Boolean = false,
    peekFraction: Float = .1f,
    peekingItems: Int = 4,
) {
    val pagerState = rememberPagerState(
        initialPage = PAGE_COUNT / 2,
        pageCount = { PAGE_COUNT },
    )

    val isPreview = LocalInspectionMode.current
    val freezeAnimation = freezeAnimation || isPreview

    LaunchedEffect(freezeAnimation) {
        if (freezeAnimation) {
            return@LaunchedEffect
        }

        while (true) {
            delay(SCROLL_INTERVAL)
            pagerState.animateScrollToPage(
                page = pagerState.currentPage + 1,
                animationSpec = tween(
                    durationMillis = SCROLL_ANIM_DURATION_MS,
                    easing = ANIMATION_CURVE,
                ),
            )
        }
    }

    Box(
        modifier = modifier.height(coverSize + coverSize * peekFraction * peekingItems * 2),
        contentAlignment = Alignment.Center,
    ) {
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = peekingItems,
            userScrollEnabled = false,
            pageSize = PageSize.Fixed(coverSize),
            pageSpacing = -coverSize * (1f - peekFraction) + 8.dp,
            contentPadding = PaddingValues(vertical = coverSize * peekFraction * peekingItems),
        ) { page ->
            if (podcastIds.isEmpty()) return@VerticalPager
            val podcastId = podcastIds[page % podcastIds.size]

            val relativePosition = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
            val pageOffset = relativePosition.absoluteValue

            if (pageOffset <= peekingItems) {
                val scale = 1f - (pageOffset * peekFraction).coerceAtMost(peekFraction * peekingItems)
                val maxOffset = peekingItems.toFloat()
                val imageAlpha = if (pageOffset >= 0.0f && pageOffset <= (maxOffset - 1f)) {
                    1f
                } else {
                    val normalizedOffset = (-pageOffset + maxOffset).coerceIn(0f, 1f)
                    LinearEasing.transform(normalizedOffset)
                }

                PodcastImage(
                    uuid = podcastId,
                    cornerSize = 4.dp,
                    modifier = Modifier
                        .size(coverSize)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = imageAlpha
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
                randomShowIds = List(7) { "id-$it" },
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}
