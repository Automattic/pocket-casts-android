package au.com.shiftyjelly.pocketcasts.sharing.clip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowLoadingButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.ui.ClipSelector
import au.com.shiftyjelly.pocketcasts.sharing.ui.ClipSelectorState
import au.com.shiftyjelly.pocketcasts.sharing.ui.CloseButton
import au.com.shiftyjelly.pocketcasts.sharing.ui.Devices
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.sharing.ui.VerticalEpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.rememberClipSelectorState
import java.sql.Date
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
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
    shareColors: ShareColors,
    listener: ShareClipPageListener,
    state: ClipSelectorState = rememberClipSelectorState(
        firstVisibleItemIndex = (clipRange.startInSeconds - 10).coerceAtLeast(0),
    ),
) = VerticalClipPage(
    episode = episode,
    podcast = podcast,
    clipRange = clipRange,
    playbackProgress = playbackProgress,
    isPlaying = isPlaying,
    useEpisodeArtwork = useEpisodeArtwork,
    shareColors = shareColors, listener = listener,
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
    shareColors: ShareColors,
    listener: ShareClipPageListener,
    state: ClipSelectorState,
) {
    val scope = rememberCoroutineScope()
    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(shareColors.background),
        ) {
            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = Modifier
                    .weight(0.1f)
                    .fillMaxSize(),
            ) {
                CloseButton(
                    shareColors = shareColors,
                    onClick = listener::onClose,
                    modifier = Modifier.padding(top = 12.dp, end = 12.dp),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    TextH30(
                        text = stringResource(LR.string.podcast_create_clip),
                        textAlign = TextAlign.Center,
                        color = shareColors.backgroundPrimaryText,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
            if (podcast != null && episode != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxSize(),
                ) {
                    VerticalEpisodeCard(
                        episode = episode,
                        podcast = podcast,
                        useEpisodeArtwork = useEpisodeArtwork,
                        shareColors = shareColors,
                    )
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(0.25f),
                ) {
                    ClipSelector(
                        episodeDuration = episode.duration.seconds,
                        clipRange = clipRange,
                        playbackProgress = playbackProgress,
                        isPlaying = isPlaying,
                        shareColors = shareColors,
                        listener = listener,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        state = state,
                    )
                    Spacer(
                        modifier = Modifier.height(16.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        var isClippingAudio by remember { mutableStateOf(false) }
                        RowButton(
                            text = "Link",
                            onClick = {
                                scope.launch {
                                    listener.onShareClipLink(podcast, episode, clipRange)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = shareColors.clipButton),
                            textColor = shareColors.clipButtonText,
                            elevation = null,
                            includePadding = false,
                            modifier = Modifier.weight(1f),
                        )
                        if (isClippingAudio) {
                            RowLoadingButton(
                                text = "",
                                isLoading = isClippingAudio,
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(backgroundColor = shareColors.clipButton),
                                textColor = shareColors.clipButtonText,
                                includePadding = false,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            RowButton(
                                text = "Audio",
                                enabled = !isClippingAudio,
                                onClick = {
                                    isClippingAudio = true
                                    scope.launch {
                                        listener.onShareClipAudio(podcast, episode, clipRange)
                                        isClippingAudio = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = shareColors.clipButton),
                                textColor = shareColors.clipButtonText,
                                elevation = null,
                                includePadding = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        RowButton(
                            text = "Video",
                            onClick = {
                                scope.launch {
                                    listener.onShareClipVideo(podcast, episode, clipRange)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = shareColors.clipButton),
                            textColor = shareColors.clipButtonText,
                            elevation = null,
                            includePadding = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "ShareClipPageVertical", device = Devices.PortraitRegular)
@Composable
private fun ShareClipPageVerticalPreview() = ShareClipPagePreview()

@Composable
internal fun ShareClipPagePreview(
    color: Long = 0xFFEC0404,
) {
    val clipRange = Clip.Range(0.seconds, 15.seconds)
    ShareClipPage(
        episode = PodcastEpisode(
            uuid = "episode-id",
            podcastUuid = "podcast-id",
            publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
            title = "Episode title",
            duration = 125.0,
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
        shareColors = ShareColors(Color(color)),
        listener = ShareClipPageListener.Preview,
        state = rememberClipSelectorState(
            firstVisibleItemIndex = 0,
        ),
    )
}
