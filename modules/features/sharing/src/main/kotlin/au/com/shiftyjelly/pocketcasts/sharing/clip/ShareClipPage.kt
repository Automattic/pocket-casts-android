package au.com.shiftyjelly.pocketcasts.sharing.clip

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.BaseRowButton
import au.com.shiftyjelly.pocketcasts.compose.components.PagerDotIndicator
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.social.PlatformBar
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import au.com.shiftyjelly.pocketcasts.sharing.ui.ClipSelector
import au.com.shiftyjelly.pocketcasts.sharing.ui.CloseButton
import au.com.shiftyjelly.pocketcasts.sharing.ui.EpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.sharing.ui.VisualCardType
import au.com.shiftyjelly.pocketcasts.sharing.ui.estimateCardCoordinates
import au.com.shiftyjelly.pocketcasts.sharing.ui.scrollBottomFade
import java.sql.Date
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal interface ShareClipPageListener {
    suspend fun onShareClipLink(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range)
    suspend fun onShareClipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range)
    suspend fun onShareClipVideo(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range)
    fun onClickPlay()
    fun onClickPause()
    fun onUpdateClipStart(duration: Duration)
    fun onUpdateClipEnd(duration: Duration)
    fun onUpdateClipProgress(duration: Duration)
    fun onUpdateTimeline(scale: Float, secondsPerTick: Int)
    fun onClose()

    companion object {
        val Preview = object : ShareClipPageListener {
            override suspend fun onShareClipLink(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) = Unit
            override suspend fun onShareClipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) = Unit
            override suspend fun onShareClipVideo(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) = Unit
            override fun onClickPlay() = Unit
            override fun onClickPause() = Unit
            override fun onUpdateClipStart(duration: Duration) = Unit
            override fun onUpdateClipEnd(duration: Duration) = Unit
            override fun onUpdateClipProgress(duration: Duration) = Unit
            override fun onUpdateTimeline(scale: Float, secondsPerTick: Int) = Unit
            override fun onClose() = Unit
        }
    }
}

@Composable
internal fun ShareClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    useEpisodeArtwork: Boolean,
    platforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    assetController: BackgroundAssetController,
    listener: ShareClipPageListener,
    state: ClipPageState = rememberClipPageState(
        firstVisibleItemIndex = (clipRange.startInSeconds - 10).coerceAtLeast(0),
    ),
) = VerticalClipPage(
    episode = episode,
    podcast = podcast,
    clipRange = clipRange,
    playbackProgress = playbackProgress,
    isPlaying = isPlaying,
    useEpisodeArtwork = useEpisodeArtwork,
    platforms = platforms,
    shareColors = shareColors,
    assetController = assetController,
    listener = listener,
    state = state,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    useEpisodeArtwork: Boolean,
    platforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    assetController: BackgroundAssetController,
    listener: ShareClipPageListener,
    state: ClipPageState,
) {
    Box(
        modifier = Modifier
            .background(shareColors.background)
            .fillMaxSize(),
    ) {
        AnimatedVisiblity(podcast = podcast, episode = episode) { podcast, episode ->
            Column {
                val pagerState = rememberPagerState(pageCount = { CardType.entires.size })
                val scrollState = rememberScrollState()
                val selectedCard = CardType.entires[pagerState.currentPage]
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .scrollBottomFade(scrollState)
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .verticalScroll(scrollState),
                ) {
                    TopContent(
                        shareColors = shareColors,
                        state = state,
                        selectedCard = selectedCard,
                    )
                    MiddleContent(
                        episode = episode,
                        podcast = podcast,
                        useEpisodeArtwork = useEpisodeArtwork,
                        shareColors = shareColors,
                        assetController = assetController,
                        state = state,
                        scrollState = scrollState,
                        pagerState = pagerState,
                    )
                }
                BottomContent(
                    podcast = podcast,
                    episode = episode,
                    clipRange = clipRange,
                    playbackProgress = playbackProgress,
                    isPlaying = isPlaying,
                    platforms = platforms,
                    shareColors = shareColors,
                    selectedCard = selectedCard,
                    listener = listener,
                    state = state,
                )
            }
        }
        CloseButton(
            shareColors = shareColors,
            onClick = listener::onClose,
            modifier = Modifier
                .padding(top = 12.dp, end = 12.dp)
                .align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun TopContent(
    shareColors: ShareColors,
    state: ClipPageState,
    selectedCard: CardType,
) {
    val titleId = if (selectedCard is CardType.Audio) LR.string.share_clip_create_audio_label else LR.string.share_clip_create_label
    val descriptionId = if (selectedCard is CardType.Audio) LR.string.share_clip_create_audio_description else LR.string.single_space
    AnimatedContent(
        label = "TopContent",
        targetState = Triple(state.step, titleId, descriptionId),
        modifier = Modifier.onGloballyPositioned { coordinates -> state.topContentHeight = coordinates.size.height },
    ) { (step, titleId, descriptionId) ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(
                modifier = Modifier.height(
                    when (step) {
                        SharingStep.Creating -> 40.dp
                        SharingStep.Sharing -> 24.dp
                    },
                ),
            )
            TextH30(
                text = stringResource(
                    when (step) {
                        SharingStep.Creating -> titleId
                        SharingStep.Sharing -> LR.string.share_clip_share_label
                    },
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = shareColors.backgroundPrimaryText,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(
                modifier = Modifier.height(8.dp),
            )
            when (step) {
                SharingStep.Creating -> TextH40(
                    text = stringResource(descriptionId),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = shareColors.backgroundSecondaryText,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                SharingStep.Sharing -> TextH40(
                    text = stringResource(LR.string.share_clip_edit_label),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = shareColors.backgroundPrimaryText,
                    fontWeight = FontWeight.W400,
                    modifier = Modifier
                        .background(shareColors.closeButton, CircleShape)
                        .defaultMinSize(minHeight = 24.dp)
                        .clickable(
                            interactionSource = remember(::MutableInteractionSource),
                            indication = rememberRipple(color = shareColors.base),
                            onClickLabel = stringResource(LR.string.share_clip_edit_label),
                            role = Role.Button,
                            onClick = { state.step = SharingStep.Creating },
                        )
                        .padding(vertical = 4.dp, horizontal = 16.dp),
                )
            }
            Spacer(
                modifier = Modifier.height(
                    when (step) {
                        SharingStep.Creating -> 12.dp
                        SharingStep.Sharing -> 48.dp
                    },
                ),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiddleContent(
    episode: PodcastEpisode,
    podcast: Podcast,
    useEpisodeArtwork: Boolean,
    shareColors: ShareColors,
    assetController: BackgroundAssetController,
    state: ClipPageState,
    scrollState: ScrollState,
    pagerState: PagerState,
) {
    AnimatedVisibility(
        label = "DotPagerIndicator",
        visible = state.step == SharingStep.Creating,
        modifier = Modifier.onGloballyPositioned { coordinates -> state.pagerIndicatorHeight = coordinates.size.height },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            PagerDotIndicator(
                state = pagerState,
            )
        }
    }

    val coordiantes = estimateCardCoordinates(
        topContentHeight = state.topContentHeight + state.pagerIndicatorHeight,
        scrollState = scrollState,
    )
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = state.step == SharingStep.Creating,
        modifier = Modifier.height(coordiantes.size.height),
    ) { pageIndex ->
        val cardType = CardType.entires[pageIndex]
        val offset by animateIntOffsetAsState(
            targetValue = coordiantes.offset(cardType),
        )
        val modifier = Modifier
            .offset { offset }
            .fillMaxSize()
            .padding(coordiantes.padding)

        when (cardType) {
            is VisualCardType -> EpisodeCard(
                cardType = cardType,
                episode = episode,
                podcast = podcast,
                useEpisodeArtwork = useEpisodeArtwork,
                shareColors = shareColors,
                captureController = assetController.captureController(cardType),
                customSize = coordiantes.size,
                modifier = modifier,
            )
            is CardType.Audio -> Box(
                contentAlignment = Alignment.Center,
                modifier = modifier,
            ) {
                Image(
                    painter = painterResource(IR.drawable.ic_audio_card),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(shareColors.backgroundPrimaryText),
                )
            }
        }
    }
}

@Composable
private fun BottomContent(
    podcast: Podcast,
    episode: PodcastEpisode,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    platforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    selectedCard: CardType,
    listener: ShareClipPageListener,
    state: ClipPageState,
) {
    AnimatedContent(
        label = "BottomContent",
        targetState = state.step,
    ) { step ->
        when (step) {
            SharingStep.Creating -> ClipControls(
                podcast = podcast,
                episode = episode,
                clipRange = clipRange,
                playbackProgress = playbackProgress,
                isPlaying = isPlaying,
                shareColors = shareColors,
                selectedCard = selectedCard,
                listener = listener,
                state = state,
            )
            SharingStep.Sharing -> Box(
                modifier = Modifier.padding(vertical = 24.dp),
            ) {
                PlatformBar(
                    platforms = platforms,
                    shareColors = shareColors,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun ClipControls(
    podcast: Podcast,
    episode: PodcastEpisode,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    shareColors: ShareColors,
    selectedCard: CardType,
    listener: ShareClipPageListener,
    state: ClipPageState,
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Spacer(
            modifier = Modifier.height(12.dp),
        )
        ClipSelector(
            episodeDuration = episode.duration.seconds,
            clipRange = clipRange,
            playbackProgress = playbackProgress,
            isPlaying = isPlaying,
            shareColors = shareColors,
            listener = listener,
            state = state.selectorState,
        )
        Spacer(
            modifier = Modifier.height(12.dp),
        )
        BaseRowButton(
            onClick = {
                if (!isLoading) {
                    when (selectedCard) {
                        CardType.Vertical, CardType.Horizontal, CardType.Square -> {
                            state.step = SharingStep.Sharing
                        }
                        CardType.Audio -> {
                            isLoading = true
                            scope.launch {
                                listener.onShareClipAudio(podcast, episode, clipRange)
                                isLoading = false
                            }
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = shareColors.clipButton),
            elevation = null,
            includePadding = false,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            val buttonText = stringResource(if (selectedCard is CardType.Audio) LR.string.share else LR.string.next)
            AnimatedContent(
                label = "ButtonText",
                targetState = buttonText to isLoading,
            ) { (buttonText, isLoading) ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextP40(
                        // Keep in the UI for to keep correct button size
                        text = if (isLoading) " " else buttonText,
                        textAlign = TextAlign.Center,
                        color = shareColors.clipButtonText,
                        modifier = Modifier.padding(6.dp),
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = shareColors.clipButtonText,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
        Spacer(
            modifier = Modifier.height(12.dp),
        )
    }
}

@Composable
private fun AnimatedVisiblity(
    podcast: Podcast?,
    episode: PodcastEpisode?,
    content: @Composable (Podcast, PodcastEpisode) -> Unit,
) = AnimatedVisibility(
    label = "ScreenContent",
    visible = podcast != null && episode != null,
    enter = fadeIn(),
    exit = fadeOut(),
) {
    if (podcast != null && episode != null) {
        content(podcast, episode)
    }
}

@Preview(name = "Regular device", device = Devices.PortraitRegular)
@Composable
private fun ShareClipVerticalRegularPreview() = ShareClipPagePreview()

@Preview(name = "Regular device sharing", device = Devices.PortraitRegular)
@Composable
private fun ShareClipVerticalRegularEditingPreview() = ShareClipPagePreview(
    step = SharingStep.Sharing,
)

@Preview(name = "Small device", device = Devices.PortraitSmall)
@Composable
private fun ShareClipVerticalSmallPreviewPreview() = ShareClipPagePreview()

@Preview(name = "Foldable device", device = Devices.PortraitFoldable)
@Composable
private fun ShareClipVerticalFoldablePreviewPreview() = ShareClipPagePreview()

@Preview(name = "Tablet device", device = Devices.PortraitTablet)
@Composable
private fun ShareClipVerticalTabletPreview() = ShareClipPagePreview()

@Composable
internal fun ShareClipPagePreview(
    color: Long = 0xFFEC0404,
    step: SharingStep = SharingStep.Creating,
) {
    val clipRange = Clip.Range(0.seconds, 15.seconds)
    ShareClipPage(
        episode = PodcastEpisode(
            uuid = "episode-id",
            podcastUuid = "podcast-id",
            publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
            title = "Episode title",
            duration = 250.0,
        ),
        podcast = Podcast(
            uuid = "podcast-id",
            title = "Podcast title",
            episodeFrequency = "monthly",
        ),
        clipRange = clipRange,
        playbackProgress = 8.seconds,
        isPlaying = false,
        useEpisodeArtwork = true,
        platforms = SocialPlatform.entries.toSet(),
        shareColors = ShareColors(Color(color)),
        assetController = BackgroundAssetController.preview(),
        listener = ShareClipPageListener.Preview,
        state = rememberClipPageState(
            firstVisibleItemIndex = 0,
            step = step,
        ),
    )
}
