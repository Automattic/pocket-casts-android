package au.com.shiftyjelly.pocketcasts.sharing.clip

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.social.PlatformBar
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import au.com.shiftyjelly.pocketcasts.sharing.ui.ClipSelector
import au.com.shiftyjelly.pocketcasts.sharing.ui.CloseButton
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.sharing.ui.VerticalEpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.scrollBottomFade
import java.sql.Date
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
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
    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(shareColors.background),
        ) {
            val scrollState = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .scrollBottomFade(scrollState)
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(scrollState),
            ) {
                TopInfo(
                    shareColors = shareColors,
                    state = state,
                )
                if (podcast != null && episode != null) {
                    val cardPadding = maxOf(
                        LocalConfiguration.current.screenWidthDp.dp / 8,
                        42.dp, // Close button start edge position
                    )
                    VerticalEpisodeCard(
                        episode = episode,
                        podcast = podcast,
                        useEpisodeArtwork = useEpisodeArtwork,
                        shareColors = shareColors,
                        captureController = assetController.captureController(CardType.Vertical),
                        useHeightForAspectRatio = false,
                        modifier = Modifier.padding(horizontal = cardPadding),
                    )
                }
            }
            if (podcast != null && episode != null) {
                AnimatedContent(state.step) { step ->
                    when (step) {
                        SharingStep.Creating -> ClipControls(
                            episode = episode,
                            clipRange = clipRange,
                            playbackProgress = playbackProgress,
                            isPlaying = isPlaying,
                            shareColors = shareColors,
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
private fun TopInfo(
    shareColors: ShareColors,
    state: ClipPageState,
) {
    AnimatedContent(state.step) { step ->
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
                        SharingStep.Creating -> LR.string.share_clip_create_label
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
                    text = " ", // Placeholder until audio clips are added
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

@Composable
private fun ClipControls(
    episode: PodcastEpisode,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    shareColors: ShareColors,
    listener: ShareClipPageListener,
    state: ClipPageState,
) {
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
        RowButton(
            text = stringResource(LR.string.next),
            onClick = { state.step = SharingStep.Sharing },
            colors = ButtonDefaults.buttonColors(backgroundColor = shareColors.clipButton),
            textColor = shareColors.clipButtonText,
            elevation = null,
            includePadding = false,
        )
        Spacer(
            modifier = Modifier.height(12.dp),
        )
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
