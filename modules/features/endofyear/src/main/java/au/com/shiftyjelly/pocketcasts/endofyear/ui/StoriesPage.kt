package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.endofyear.Story
import au.com.shiftyjelly.pocketcasts.endofyear.UiState
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.utils.Util
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
internal fun StoriesPage(
    state: UiState,
) {
    val size = getSizeLimit(LocalContext.current)?.let(Modifier::size) ?: Modifier.fillMaxSize()
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
                coverFontSize = coverFontSize,
                coverTextHeight = coverTextHeight,
            )
        }

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
private fun BoxWithConstraintsScope.Stories(
    stories: List<Story>,
    coverFontSize: TextUnit,
    coverTextHeight: Dp,
) {
    val pagerState = rememberPagerState(pageCount = { stories.size })
    val coroutineScope = rememberCoroutineScope()
    val widthPx = LocalDensity.current.run { maxWidth.toPx() }

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
            is Story.Cover -> CoverStory(story, coverFontSize, coverTextHeight)
            is Story.NumberOfShows -> StoryPlaceholder(story)
            is Story.TopShow -> StoryPlaceholder(story)
            is Story.TopShows -> StoryPlaceholder(story)
            is Story.Ratings -> StoryPlaceholder(story)
            is Story.TotalTime -> StoryPlaceholder(story)
            is Story.LongestEpisode -> StoryPlaceholder(story)
            is Story.PlusInterstitial -> StoryPlaceholder(story)
            is Story.YearVsYear -> StoryPlaceholder(story)
            is Story.CompletionRate -> StoryPlaceholder(story)
            is Story.Ending -> StoryPlaceholder(story)
        }
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

@Composable
private fun CoverStory(
    story: Story.Cover,
    fontSize: TextUnit,
    textHeight: Dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor),
    ) {
        val listState = rememberLazyListState()
        val scrollDelay = (10 / LocalDensity.current.density).roundToLong().coerceAtLeast(4L)
        LaunchedEffect(Unit) {
            while (isActive) {
                listState.scrollBy(2f)
                delay(scrollDelay)
            }
        }
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = false,
            state = listState,
        ) {
            items(Int.MAX_VALUE) {
                PlaybackText(
                    color = Color(0xFFEEB1F4),
                    fontSize = fontSize,
                    modifier = Modifier.sizeIn(maxHeight = textHeight),
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.end_of_year_2024_sticker_2),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 26.dp, y = 52.dp)
                .size(width = 172.dp, height = 163.dp),
        )
        Image(
            painter = painterResource(R.drawable.end_of_year_2024_sticker_1),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = -24.dp, y = -48.dp)
                .size(width = 250.dp, height = 188.dp),
        )
    }
}

private val Story.backgroundColor get() = when (this) {
    is Story.Cover -> Color(0xFFEE661C)
    is Story.NumberOfShows -> Color(0xFFEFECAD)
    is Story.TopShow -> Color(0xFFEDB0F3)
    is Story.TopShows -> Color(0xFFE0EFAD)
    is Story.Ratings -> Color(0xFFEFECAD)
    is Story.TotalTime -> Color(0xFFEDB0F3)
    is Story.LongestEpisode -> Color(0xFFE0EFAD)
    is Story.PlusInterstitial -> Color(0xFFEFECAD)
    is Story.YearVsYear -> Color(0xFFEEB1F4)
    is Story.CompletionRate -> Color(0xFFE0EFAD)
    is Story.Ending -> Color(0xFFEE661C)
}

private fun getSizeLimit(context: Context): DpSize? {
    return if (Util.isTablet(context)) {
        val configuration = context.resources.configuration
        val screenHeightInDp = configuration.screenHeightDp
        val dialogHeight = (screenHeightInDp * 0.9f).coerceAtMost(700.dp.value)
        val dialogWidth = dialogHeight / 2f
        DpSize(dialogWidth.dp, dialogHeight.dp)
    } else {
        null
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

@Preview(device = Devices.PortraitRegular)
@Composable
fun CoverStoryPreview() {
    CoverStory(
        story = Story.Cover,
        fontSize = 260.sp,
        textHeight = 210.dp,
    )
}
