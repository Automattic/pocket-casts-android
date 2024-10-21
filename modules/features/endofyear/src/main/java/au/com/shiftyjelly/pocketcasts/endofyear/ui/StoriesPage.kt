package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.endofyear.Story
import au.com.shiftyjelly.pocketcasts.endofyear.UiState
import au.com.shiftyjelly.pocketcasts.utils.Util
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun StoriesPage(
    state: UiState,
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
            ErrorMessage()
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
            )
        }

        CloseButton(onClose)

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
) {
    val pagerState = rememberPagerState(pageCount = { stories.size })
    val coroutineScope = rememberCoroutineScope()
    val widthPx = LocalDensity.current.run { measurements.width.toPx() }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                if (!pagerState.isScrollInProgress) {
                    coroutineScope.launch {
                        val nextPage = if (offset.x > widthPx / 2) {
                            pagerState.currentPage + 1
                        } else {
                            pagerState.currentPage - 1
                        }
                        pagerState.scrollToPage(nextPage)
                    }
                }
            }
        },
    ) { index ->
        when (val story = stories[index]) {
            is Story.Cover -> CoverStory(story, measurements)
            is Story.NumberOfShows -> NumberOfShowsStory(story, measurements)
            is Story.TopShow -> TopShowStory(story, measurements)
            is Story.TopShows -> TopShowsStory(story, measurements)
            is Story.Ratings -> RatingsStory(story, measurements)
            is Story.TotalTime -> TotalTimeStory(story, measurements)
            is Story.LongestEpisode -> LongestEpisodeStory(story, measurements)
            is Story.PlusInterstitial -> StoryPlaceholder(story)
            is Story.YearVsYear -> YearVsYearStory(story, measurements)
            is Story.CompletionRate -> CompletionRateStory(story, measurements)
            is Story.Ending -> EndingStory(story, measurements)
        }
    }
}

@Composable
internal fun BoxScope.CloseButton(
    onClose: () -> Unit,
) {
    Image(
        painter = painterResource(IR.drawable.ic_close),
        contentDescription = stringResource(LR.string.close),
        colorFilter = ColorFilter.tint(Color.Black),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 20.dp, end = 18.dp)
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
private fun ErrorMessage() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Story.Cover.backgroundColor)
            .padding(16.dp),
    ) {
        TextH10(text = "Whoops!")
    }
}

@Composable
private fun StoryPlaceholder(story: Story) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor),
    ) {
        TextP50("$story", color = Color.Black)
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
