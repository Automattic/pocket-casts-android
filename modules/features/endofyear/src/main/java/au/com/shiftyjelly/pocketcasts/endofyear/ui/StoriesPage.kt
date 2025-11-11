package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PagerProgressingIndicator
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.endofyear.UiState
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.utils.Util
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun StoriesPage(
    state: UiState,
    pagerState: PagerState,
    controller: StoryCaptureController,
    insets: WindowInsets,
    onChangeStory: (Boolean) -> Unit,
    onShareStory: (Story, File) -> Unit,
    onHoldStory: () -> Unit,
    onReleaseStory: () -> Unit,
    onLearnAboutRatings: () -> Unit,
    onClickUpsell: () -> Unit,
    onRestartPlayback: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val size = LocalContext.current.sizeLimit?.let(Modifier::size) ?: Modifier.fillMaxSize()
    BoxWithConstraints(
        modifier = Modifier.then(size),
    ) {
        val density = LocalDensity.current
        val widthPx = density.run { maxWidth.toPx() }

        var isTextSizeComputed by remember { mutableStateOf(false) }
        var coverFontSize by remember { mutableStateOf(24.sp) }
        var coverTextHeight by remember { mutableStateOf(0.dp) }
        val measurements = remember(maxWidth, maxHeight, insets, coverFontSize, coverTextHeight) {
            EndOfYearMeasurements(
                width = maxWidth,
                height = maxHeight,
                statusBarInsets = insets,
                coverFontSize = coverFontSize,
                coverTextHeight = coverTextHeight,
                closeButtonBottomEdge = density.run { insets.getTop(density).toDp() } + 36.dp,
            )
        }

        if (state is UiState.Failure) {
            ErrorMessage(onRetry)
        } else if (state is UiState.Syncing || !isTextSizeComputed) {
            LoadingIndicator()
        } else if (state is UiState.Synced) {
            Stories(
                stories = state.stories,
                measurements = measurements,
                controller = controller,
                pagerState = pagerState,
                onChangeStory = onChangeStory,
                onShareStory = onShareStory,
                onHoldStory = onHoldStory,
                onReleaseStory = onReleaseStory,
                onLearnAboutRatings = onLearnAboutRatings,
                onClickUpsell = onClickUpsell,
                onRestartPlayback = onRestartPlayback,
            )
        }

        TopControls(
            pagerState = pagerState,
            progress = state.storyProgress,
            color = (state as? UiState.Synced)?.stories?.get(pagerState.currentPage)?.controlsColor ?: Color.White,
            measurements = measurements,
            onClose = onClose,
            controller = controller,
        )

        // Use an invisible 'PLAYBACK' text to compute an appropriate font size.
        // The font should occupy the whole viewport's width with some padding.
        if (!isTextSizeComputed) {
            PlaybackText(
                color = Color.Transparent,
                fontSize = coverFontSize,
                onTextLayout = { result ->
                    when {
                        isTextSizeComputed -> Unit
                        else -> {
                            val textSize = result.size.width
                            val ratio = 0.88 * widthPx / textSize
                            if (ratio !in 0.95..1.01) {
                                coverFontSize *= ratio
                            } else {
                                coverTextHeight = density.run { (result.firstBaseline).toDp() * 1.1f }.coerceAtLeast(0.dp)
                                isTextSizeComputed = true
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun Stories(
    stories: List<Story>,
    measurements: EndOfYearMeasurements,
    pagerState: PagerState,
    controller: StoryCaptureController,
    onChangeStory: (Boolean) -> Unit,
    onShareStory: (Story, File) -> Unit,
    onHoldStory: () -> Unit,
    onReleaseStory: () -> Unit,
    onLearnAboutRatings: () -> Unit,
    onClickUpsell: () -> Unit,
    onRestartPlayback: () -> Unit,
) {
    val widthPx = LocalDensity.current.run { measurements.width.toPx() }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown().consume()
                val timeMark = TimeSource.Monotonic.markNow()
                onHoldStory()
                val up = waitForUpOrCancellation()?.also { it.consume() }
                if (up != null && timeMark.elapsedNow() < 250.milliseconds) {
                    val moveForward = up.position.x > widthPx / 2
                    onChangeStory(moveForward)
                }
                onReleaseStory()
            }
        },
    ) { index ->
        when (val story = stories[index]) {
            is Story.Cover -> CoverStory(
                story = story,
                measurements = measurements,
            )
            is Story.NumberOfShows -> NumberOfShowsStory(
                story = story,
                measurements = measurements,
                controller = controller,
                onShareStory = { file -> onShareStory(story, file) },
            )
            is Story.TopShow -> TopShowStory(
                story = story,
                measurements = measurements,
                controller = controller,
                onShareStory = { file -> onShareStory(story, file) },
            )
            is Story.TopShows -> TopShowsStory(
                story = story,
                measurements = measurements,
                controller = controller,
                onShareStory = { file -> onShareStory(story, file) },
            )
            is Story.Ratings -> RatingsStory(
                story = story,
                measurements = measurements,
                controller = controller,
                onShareStory = { file -> onShareStory(story, file) },
                onLearnAboutRatings = onLearnAboutRatings,
            )
            is Story.TotalTime -> TotalTimeStory(
                story = story,
                controller = controller,
                onShareStory = { file -> onShareStory(story, file) },
            )
            is Story.LongestEpisode -> LongestEpisodeStory(
                story = story,
                measurements = measurements,
                controller = controller,
                onShareStory = { file -> onShareStory(story, file) },
            )
            is Story.PlusInterstitial -> PlusInterstitialStory(
                story = story,
                measurements = measurements,
                onClickUpsell = onClickUpsell,
            )
            is Story.YearVsYear -> YearVsYearStory(
                story = story,
                measurements = measurements,
                controller = controller,
                onShareStory = { file -> onShareStory(story, file) },
            )
            is Story.CompletionRate -> CompletionRateStory(
                story = story,
                measurements = measurements,
                controller = controller,
                onShareStory = { file -> onShareStory(story, file) },
            )
            is Story.Ending -> EndingStory(
                story = story,
                measurements = measurements,
                onRestartPlayback = onRestartPlayback,
            )
        }
    }
}

@Composable
internal fun BoxScope.TopControls(
    pagerState: PagerState,
    progress: Float,
    color: Color,
    measurements: EndOfYearMeasurements,
    onClose: () -> Unit,
    controller: StoryCaptureController,
) {
    val density = LocalDensity.current
    // Calculate the height so that we can remove it from the share images
    val statusBarHeightPx = measurements.statusBarInsets.getTop(density)
    val extraPaddingPx = with(density) { 24.dp.roundToPx() }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .windowInsetsPadding(measurements.statusBarInsets)
            .padding(start = 16.dp, end = 16.dp)
            .onGloballyPositioned { controller.topControlsHeightPx = it.size.height + statusBarHeightPx - extraPaddingPx },
    ) {
        PagerProgressingIndicator(
            state = pagerState,
            progress = progress,
            activeColor = color,
        )
        Spacer(
            modifier = Modifier.height(10.dp),
        )
        Image(
            painter = painterResource(IR.drawable.ic_close),
            contentDescription = stringResource(LR.string.close),
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier
                // Increase touch target of the image
                .offset(x = 12.dp, y = (-12).dp)
                .size(48.dp)
                .clickable(
                    interactionSource = remember(::MutableInteractionSource),
                    indication = ripple(color = Color.Black, bounded = false),
                    onClickLabel = stringResource(LR.string.close),
                    role = Role.Button,
                    onClick = onClose,
                )
                .padding(10.dp),
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Story.Cover.backgroundColor)
            .padding(16.dp),
    ) {
        LinearProgressIndicator(color = Color.Black)
    }
}

@Composable
private fun ErrorMessage(
    onRetry: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(Story.Cover.backgroundColor),
    ) {
        TextH30(
            text = stringResource(id = LR.string.end_of_year_stories_failed),
            textAlign = TextAlign.Center,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEEB1F4)),
            modifier = Modifier.padding(top = 20.dp),
        ) {
            TextP40(
                text = stringResource(id = LR.string.retry),
                color = Color.Black,
            )
        }
    }
}

private val Context.sizeLimit: DpSize?
    get() {
        return if (Util.isTablet(this)) {
            val configuration = resources.configuration
            val screenHeightInDp = configuration.screenHeightDp
            val dialogHeight = (screenHeightInDp * 0.9f).coerceAtMost(700.dp.value)
            val dialogWidth = dialogHeight / 2f
            DpSize(dialogWidth.dp, dialogHeight.dp)
        } else {
            null
        }
    }

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun ErrorMessagePreview() {
    ErrorMessage(
        onRetry = {},
    )
}
