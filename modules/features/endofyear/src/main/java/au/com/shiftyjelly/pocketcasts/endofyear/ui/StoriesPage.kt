package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollDirection
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollingRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.endofyear.Story
import au.com.shiftyjelly.pocketcasts.endofyear.UiState
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.utils.Util
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.math.tan
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun StoriesPage(
    state: UiState,
    onClose: () -> Unit,
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
                sizes = EndOfYearSizes(
                    width = this@BoxWithConstraints.maxWidth,
                    height = this@BoxWithConstraints.maxHeight,
                    closeButtonBottomEdge = 44.dp,
                    coverFontSize = coverFontSize,
                    coverTextHeight = coverTextHeight,
                ),
            )
        }

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
    sizes: EndOfYearSizes,
) {
    val pagerState = rememberPagerState(pageCount = { stories.size })
    val coroutineScope = rememberCoroutineScope()
    val widthPx = LocalDensity.current.run { sizes.width.toPx() }

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
            is Story.Cover -> CoverStory(story, sizes)
            is Story.NumberOfShows -> NumberOfShowsStory(story, sizes)
            is Story.TopShow -> TopShowStory(story, sizes)
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
    sizes: EndOfYearSizes,
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
                    fontSize = sizes.coverFontSize,
                    modifier = Modifier.sizeIn(maxHeight = sizes.coverTextHeight),
                )
            }
        }
        Image(
            painter = painterResource(IR.drawable.end_of_year_2024_sticker_2),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 26.dp, y = 52.dp)
                .size(width = 172.dp, height = 163.dp),
        )
        Image(
            painter = painterResource(IR.drawable.end_of_year_2024_sticker_1),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = -24.dp, y = -48.dp)
                .size(width = 250.dp, height = 188.dp),
        )
    }
}

private val rotationDegrees = -15f
private val rotationAngle = (rotationDegrees * Math.PI / 180).toFloat()

@Composable
private fun NumberOfShowsStory(
    story: Story.NumberOfShows,
    sizes: EndOfYearSizes,
) {
    val coverSize = 160.dp * sizes.scale
    val spacingSize = coverSize / 10
    val carouselHeight = coverSize * 2 + spacingSize
    val carouselRotationOffset = (sizes.width / 2) * tan(rotationAngle)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .rotate(rotationDegrees)
                .offset(y = sizes.closeButtonBottomEdge + 8.dp - carouselRotationOffset)
                .requiredWidth(sizes.width * 1.5f), // Increase the size to account for rotation
        ) {
            PodcastCoverCarousel(
                podcastIds = story.topShowIds,
                scrollDirection = ScrollDirection.Left,
                coverSize = coverSize,
                spacingSize = spacingSize,
            )
            Spacer(
                modifier = Modifier.height(spacingSize),
            )
            PodcastCoverCarousel(
                podcastIds = story.bottomShowIds,
                scrollDirection = ScrollDirection.Right,
                coverSize = coverSize,
                spacingSize = spacingSize,
            )
        }

        // Fake sticker: lH66LwxxgG8btQ8NrM0ldx-fi-3070_28391#986464596
        val stickerWidth = 214.dp * sizes.scale
        val stickerHeight = 112.dp * sizes.scale
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = stickerWidth / 3,
                    y = sizes.closeButtonBottomEdge + 8.dp + carouselHeight,
                )
                .size(stickerWidth, stickerHeight)
                .background(Color.Black, shape = CircleShape),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.1f to story.backgroundColor,
                    ),
                ),
        ) {
            TextH10(
                text = stringResource(LR.string.end_of_year_story_listened_to_numbers, story.showCount, story.epsiodeCount),
                fontSize = 31.nonScaledSp,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            TextP40(
                text = stringResource(LR.string.end_of_year_story_listened_to_numbers_subtitle),
                fontSize = 15.nonScaledSp,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            RowOutlinedButton(
                text = stringResource(LR.string.end_of_year_share_story),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = Color.Black,
                ),
                border = ButtonDefaults.outlinedBorder.copy(
                    brush = SolidColor(Color.Black),
                ),
                onClick = {},
            )
        }
    }
}

@Composable
private fun PodcastCoverCarousel(
    podcastIds: List<String>,
    scrollDirection: ScrollDirection,
    coverSize: Dp,
    spacingSize: Dp,
) {
    ScrollingRow(
        items = podcastIds,
        scrollDirection = scrollDirection,
        scrollByPixels = 2f,
        horizontalArrangement = Arrangement.spacedBy(spacingSize),
    ) { podcastId ->
        PodcastImage(
            uuid = podcastId,
            elevation = 0.dp,
            cornerSize = 4.dp,
            modifier = Modifier.size(coverSize),
        )
    }
}

@Composable
private fun TopShowStory(
    story: Story.TopShow,
    sizes: EndOfYearSizes,
) {
    Box {
        val shapeSize = sizes.width * 1.12f
        val coverSize = shapeSize * sqrt(2f)
        val coverOffset = sizes.closeButtonBottomEdge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            PodcastImage(
                uuid = "221f43c0-355c-013b-efa5-0acc26574db2",
                elevation = 0.dp,
                roundCorners = false,
                modifier = Modifier
                    .requiredSize(coverSize)
                    .offset(y = coverOffset),
            )
        }
        val transition = rememberInfiniteTransition(label = "transition")
        val rotation = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
            label = "rotation",
        )

        val shapeSizePx = LocalDensity.current.run { shapeSize.toPx() }
        val shapeOffsetPx = LocalDensity.current.run { (coverOffset * 0.6f + (coverSize - shapeSize) / 2).toPx() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    val path = Path().apply {
                        val dentSize = (shapeSizePx - size.width) / 2

                        moveTo(-dentSize, shapeOffsetPx)
                        lineTo(size.width / 2, (shapeOffsetPx) + dentSize)
                        lineTo(size.width + dentSize, shapeOffsetPx)
                        lineTo(size.width, shapeOffsetPx + shapeSizePx / 2)
                        lineTo(size.width + dentSize, shapeOffsetPx + shapeSizePx)
                        lineTo(size.width / 2, shapeOffsetPx + shapeSizePx - dentSize)
                        lineTo(-dentSize, shapeOffsetPx + shapeSizePx)
                        lineTo(0f, shapeOffsetPx + shapeSizePx / 2)
                        lineTo(-dentSize, shapeOffsetPx)

                        close()
                    }

                    onDrawWithContent {
                        drawContent()

                        rotate(rotation.value, pivot = Offset(x = size.width / 2, y = shapeOffsetPx + shapeSizePx / 2)) {
                            drawPath(
                                color = Color(0xFFFFFFFF),
                                blendMode = BlendMode.DstOut,
                                path = path,
                            )
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

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            TextH10(
                text = stringResource(
                    LR.string.end_of_year_story_top_podcast_title,
                    story.show.title,
                ),
                fontSize = 31.nonScaledSp,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            TextP40(
                text = stringResource(
                    LR.string.end_of_year_story_top_podcast_subtitle,
                    story.show.playedEpisodeCount,
                    StatsHelper.secondsToFriendlyString(
                        story.show.playbackTime.inWholeSeconds,
                        LocalContext.current.resources,
                    ),
                ),
                fontSize = 15.nonScaledSp,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            RowOutlinedButton(
                text = stringResource(LR.string.end_of_year_share_story),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = Color.Black,
                ),
                border = ButtonDefaults.outlinedBorder.copy(
                    brush = SolidColor(Color.Black),
                ),
                onClick = {},
            )
        }

        // Clip the rotating shape at top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(sizes.closeButtonBottomEdge)
                .background(story.backgroundColor),
        )

        // Fake sticker: lH66LwxxgG8btQ8NrM0ldx-fi-3070_23365#986383416
        val stickerWidth = 208.dp * sizes.scale
        val stickerHeight = 87.dp * sizes.scale
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = -stickerWidth / 3,
                    y = sizes.closeButtonBottomEdge + stickerHeight / 2,
                )
                .size(stickerWidth, stickerHeight)
                .background(Color.Black, shape = CircleShape),
        )
    }
}

private data class EndOfYearSizes(
    val width: Dp = Dp.Unspecified,
    val height: Dp = Dp.Unspecified,
    val coverFontSize: TextUnit = TextUnit.Unspecified,
    val coverTextHeight: Dp = Dp.Unspecified,
    val closeButtonBottomEdge: Dp = Dp.Unspecified,
) {
    val scale = width / 393.dp
}

private val Story.backgroundColor
    get() = when (this) {
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
        sizes = EndOfYearSizes(
            coverFontSize = 260.sp,
            coverTextHeight = 210.dp,
        ),
    )
}

@Preview(device = Devices.PortraitRegular)
@Composable
fun NumberOfShowsPreview() {
    BoxWithConstraints {
        NumberOfShowsStory(
            story = Story.NumberOfShows(
                showCount = 20,
                epsiodeCount = 125,
                topShowIds = List(4) { "id-$it" },
                bottomShowIds = List(4) { "id-$it" },
            ),
            sizes = EndOfYearSizes(
                width = maxWidth,
                height = maxHeight,
                closeButtonBottomEdge = 44.dp,
            ),
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
fun TopShowPreview() {
    BoxWithConstraints {
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
            sizes = EndOfYearSizes(
                width = maxWidth,
                height = maxHeight,
                closeButtonBottomEdge = 44.dp,
            ),
        )
    }
}
