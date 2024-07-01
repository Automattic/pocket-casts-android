package au.com.shiftyjelly.pocketcasts.clip

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import java.sql.Date
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ShareClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    clipRange: Clip.Range,
    episodeCount: Int,
    isPlaying: Boolean,
    useEpisodeArtwork: Boolean,
    clipColors: ClipColors,
    onClip: () -> Unit,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onClipStartUpdate: (Duration) -> Unit,
    onClipEndUpdate: (Duration) -> Unit,
    onClose: () -> Unit,
    state: ClipSelectorState = rememberClipSelectorState(
        firstVisibleItemIndex = (clipRange.startInSeconds - 10).coerceAtLeast(0),
    ),
) = when (LocalConfiguration.current.orientation) {
    Configuration.ORIENTATION_LANDSCAPE -> HorizontalClipPage(
        episode = episode,
        podcast = podcast,
        clipRange = clipRange,
        episodeCount = episodeCount,
        isPlaying = isPlaying,
        useEpisodeArtwork = useEpisodeArtwork,
        clipColors = clipColors,
        onClip = onClip,
        onPlayClick = onPlayClick,
        onPauseClick = onPauseClick,
        onClipStartUpdate = onClipStartUpdate,
        onClipEndUpdate = onClipEndUpdate,
        onClose = onClose,
        state = state,
    )

    else -> VerticalClipPage(
        episode = episode,
        podcast = podcast,
        clipRange = clipRange,
        isPlaying = isPlaying,
        useEpisodeArtwork = useEpisodeArtwork,
        clipColors = clipColors,
        onClip = onClip,
        onPlayClick = onPlayClick,
        onPauseClick = onPauseClick,
        onClipStartUpdate = onClipStartUpdate,
        onClipEndUpdate = onClipEndUpdate,
        onClose = onClose,
        state = state,
    )
}

@Composable
private fun VerticalClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    clipRange: Clip.Range,
    isPlaying: Boolean,
    useEpisodeArtwork: Boolean,
    clipColors: ClipColors,
    onClip: () -> Unit,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onClipStartUpdate: (Duration) -> Unit,
    onClipEndUpdate: (Duration) -> Unit,
    onClose: () -> Unit,
    state: ClipSelectorState,
) = Box {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(clipColors.background),
    ) {
        Spacer(
            modifier = Modifier.weight(0.5f),
        )

        TextH30(
            text = stringResource(LR.string.podcast_create_clip),
            color = clipColors.backgroundText,
        )

        Spacer(
            modifier = Modifier.weight(1f),
        )

        if (episode != null && podcast != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 50.dp),
            ) {
                VerticalClipCard(
                    episode = episode,
                    podcast = podcast,
                    useEpisodeArtwork = useEpisodeArtwork,
                    clipColors = clipColors,
                )
            }
            Spacer(
                modifier = Modifier.weight(1f),
            )
            ClipSelector(
                episodeDuration = episode.duration.seconds,
                clipRange = clipRange,
                isPlaying = isPlaying,
                clipColors = clipColors,
                onPlayClick = onPlayClick,
                onPauseClick = onPauseClick,
                onClipStartUpdate = onClipStartUpdate,
                onClipEndUpdate = onClipEndUpdate,
                modifier = Modifier.padding(horizontal = 16.dp),
                state = state,
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            ClipButton(
                episode = episode,
                clip = clipRange,
                clipColors = clipColors,
                onClip = onClip,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
            )
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        CloseButton(
            clipColors = clipColors,
            onClose = onClose,
            modifier = Modifier.padding(top = 16.dp, end = 16.dp),
        )
    }
}

@Composable
private fun HorizontalClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    clipRange: Clip.Range,
    episodeCount: Int,
    isPlaying: Boolean,
    useEpisodeArtwork: Boolean,
    clipColors: ClipColors,
    onClip: () -> Unit,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onClipStartUpdate: (Duration) -> Unit,
    onClipEndUpdate: (Duration) -> Unit,
    onClose: () -> Unit,
    state: ClipSelectorState,
) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier
            .fillMaxSize()
            .background(clipColors.background),
    ) {
        Row {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 32.dp),
            ) {
                if (episode != null && podcast != null) {
                    BoxWithConstraints {
                        HorizontalClipCard(
                            episode = episode,
                            podcast = podcast,
                            episodeCount = episodeCount,
                            useEpisodeArtwork = useEpisodeArtwork,
                            clipColors = clipColors,
                            modifier = Modifier
                                .width(minOf(maxWidth, 360.dp))
                                .height(180.dp),
                        )
                    }
                }
            }
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 32.dp),
            ) {
                if (episode != null) {
                    ClipSelector(
                        episodeDuration = episode.duration.seconds,
                        clipRange = clipRange,
                        isPlaying = isPlaying,
                        clipColors = clipColors,
                        onPlayClick = onPlayClick,
                        onPauseClick = onPauseClick,
                        onClipStartUpdate = onClipStartUpdate,
                        onClipEndUpdate = onClipEndUpdate,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        state = state,
                    )
                    Spacer(
                        modifier = Modifier.height(16.dp),
                    )
                    ClipButton(
                        episode = episode,
                        clip = clipRange,
                        clipColors = clipColors,
                        onClip = onClip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
        TextH30(
            text = stringResource(LR.string.podcast_create_clip),
            color = clipColors.backgroundText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
        )
        CloseButton(
            clipColors = clipColors,
            onClose = onClose,
            modifier = Modifier.padding(top = 24.dp, end = 16.dp),
        )
    }
}

@Composable
private fun ClipButton(
    episode: PodcastEpisode,
    clip: Clip.Range,
    clipColors: ClipColors,
    onClip: () -> Unit,
    modifier: Modifier = Modifier,
) = RowButton(
    text = stringResource(LR.string.podcast_share_clip),
    contentDescription = stringResource(
        id = LR.string.podcast_share_clip_description,
        episode.title,
        clip.startInSeconds,
        clip.endInSeconds,
    ),
    onClick = onClip,
    colors = ButtonDefaults.buttonColors(backgroundColor = clipColors.clipButton),
    textColor = clipColors.clipButtonText,
    elevation = null,
    includePadding = false,
    modifier = modifier,
)

@Composable
private fun CloseButton(
    clipColors: ClipColors,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) = Image(
    painter = painterResource(IR.drawable.ic_close_sheet),
    contentDescription = stringResource(LR.string.close),
    colorFilter = ColorFilter.tint(clipColors.closeButtonIcon),
    modifier = modifier
        .clickable(onClick = onClose)
        .clip(CircleShape)
        .background(clipColors.closeButton),
)

internal const val PreviewDevicePortrait = "spec:width=400dp,height=800dp,dpi=320"
internal const val PreviewDeviceLandscape = "$PreviewDevicePortrait,orientation=landscape"
internal const val PreviewPixelsPerDuration = 11f

@ShowkaseComposable(name = "ShareClipPageVertical", group = "Clip")
@Preview(name = "ShareClipPageVertical", device = PreviewDevicePortrait)
@Composable
fun ShareClipPageVerticalPreview() = ShareClipPagePreview()

@ShowkaseComposable(name = "ShareClipHorizontalPage", group = "Clip")
@Preview(name = "ShareClipHorizontalPage", device = PreviewDeviceLandscape)
@Composable
fun ShareClipPageHorizontalPreview() = ShareClipPagePreview()

@ShowkaseComposable(name = "ShareClipVerticalSmallPage", group = "Clip")
@Preview(name = "ShareClipVerticalSmallPage", device = "spec:width=360dp,height=640dp,dpi=320,orientation=portrait")
@Composable
fun ShareClipPageVerticalSmallPreview() = ShareClipPagePreview()

@ShowkaseComposable(name = "ShareClipHorizontalSmallPage", group = "Clip")
@Preview(name = "ShareClipHorizontalSmallPage", device = "spec:width=360dp,height=640dp,dpi=320,orientation=landscape")
@Composable
fun ShareClipPageHorizontalSmallPreview() = ShareClipPagePreview()

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
        episodeCount = 120,
        isPlaying = false,
        useEpisodeArtwork = true,
        clipColors = ClipColors(Color(color)),
        onClip = {},
        onPlayClick = {},
        onPauseClick = {},
        onClipStartUpdate = {},
        onClipEndUpdate = {},
        onClose = {},
        state = rememberClipSelectorState(
            firstVisibleItemIndex = 0,
            endOffset = clipRange.end.inWholeSeconds * PreviewPixelsPerDuration,
        ),
    )
}
