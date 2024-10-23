package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ripple.rememberRipple
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
import au.com.shiftyjelly.pocketcasts.endofyear.UiState
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StoriesPage(
    state: UiState,
    pagerState: PagerState,
    onChangeStory: (Boolean) -> Unit,
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

        if (state is UiState.Failure) {
            ErrorMessage(onRetry)
        } else if (state is UiState.Syncing || !isTextSizeComputed) {
            LoadingIndicator()
        } else if (state is UiState.Synced) {
            Stories(
                stories = state.stories,
                measurements = EndOfYearMeasurements(
                    width = this@BoxWithConstraints.maxWidth,
                    height = this@BoxWithConstraints.maxHeight,
                    closeButtonBottomEdge = 44.dp,
                    coverFontSize = coverFontSize,
                    coverTextHeight = coverTextHeight,
                ),
                pagerState = pagerState,
                onChangeStory = onChangeStory,
                onLearnAboutRatings = onLearnAboutRatings,
                onClickUpsell = onClickUpsell,
                onRestartPlayback = onRestartPlayback,
            )
        }

        TopControls(pagerState, state.storyProgress, onClose)

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Stories(
    stories: List<Story>,
    measurements: EndOfYearMeasurements,
    pagerState: PagerState,
    onChangeStory: (Boolean) -> Unit,
    onLearnAboutRatings: () -> Unit,
    onClickUpsell: () -> Unit,
    onRestartPlayback: () -> Unit,
) {
    val widthPx = LocalDensity.current.run { measurements.width.toPx() }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val moveForward = offset.x > widthPx / 2
                onChangeStory(moveForward)
            }
        },
    ) { index ->
        when (val story = stories[index]) {
            is Story.Cover -> CoverStory(story, measurements)
            is Story.NumberOfShows -> NumberOfShowsStory(story, measurements)
            is Story.TopShow -> TopShowStory(story, measurements)
            is Story.TopShows -> TopShowsStory(story, measurements)
            is Story.Ratings -> RatingsStory(story, measurements, onLearnAboutRatings)
            is Story.TotalTime -> TotalTimeStory(story, measurements)
            is Story.LongestEpisode -> LongestEpisodeStory(story, measurements)
            is Story.PlusInterstitial -> PlusInterstitialStory(story, measurements, onClickUpsell)
            is Story.YearVsYear -> YearVsYearStory(story, measurements)
            is Story.CompletionRate -> CompletionRateStory(story, measurements)
            is Story.Ending -> EndingStory(story, measurements, onRestartPlayback)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BoxScope.TopControls(
    pagerState: PagerState,
    progress: Float,
    onClose: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp),
    ) {
        PagerProgressingIndicator(
            state = pagerState,
            progress = progress,
            activeColor = Color.Black,
        )
        Spacer(
            modifier = Modifier.height(10.dp),
        )
        Image(
            painter = painterResource(IR.drawable.ic_close),
            contentDescription = stringResource(LR.string.close),
            colorFilter = ColorFilter.tint(Color.Black),
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember(::MutableInteractionSource),
                    indication = rememberRipple(color = Color.Black, bounded = false),
                    onClickLabel = stringResource(LR.string.close),
                    role = Role.Button,
                    onClick = onClose,
                ),
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

@Preview(device = Devices.PortraitRegular)
@Composable
private fun ErrorMessagePreview() {
    ErrorMessage(
        onRetry = {},
    )
}
