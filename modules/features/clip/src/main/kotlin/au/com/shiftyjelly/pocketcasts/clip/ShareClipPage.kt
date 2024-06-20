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
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ShareClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    episodeCount: Int,
    isPlaying: Boolean,
    useEpisodeArtwork: Boolean,
    clipColors: ClipColors,
    onClip: () -> Unit,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onClose: () -> Unit,
) = when (LocalConfiguration.current.orientation) {
    Configuration.ORIENTATION_LANDSCAPE -> HorizontalClipPage(
        episode = episode,
        podcast = podcast,
        episodeCount = episodeCount,
        isPlaying = isPlaying,
        useEpisodeArtwork = useEpisodeArtwork,
        clipColors = clipColors,
        onClip = onClip,
        onPlayClick = onPlayClick,
        onPauseClick = onPauseClick,
        onClose = onClose,
    )
    else -> VerticalClipPage(
        episode = episode,
        podcast = podcast,
        isPlaying = isPlaying,
        useEpisodeArtwork = useEpisodeArtwork,
        clipColors = clipColors,
        onClip = onClip,
        onPlayClick = onPlayClick,
        onPauseClick = onPauseClick,
        onClose = onClose,
    )
}

@Composable
private fun VerticalClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    isPlaying: Boolean,
    useEpisodeArtwork: Boolean,
    clipColors: ClipColors,
    onClip: () -> Unit,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onClose: () -> Unit,
) = Box {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(clipColors.backgroundColor),
    ) {
        Spacer(
            modifier = Modifier.weight(0.5f),
        )

        TextH30(
            text = stringResource(LR.string.podcast_create_clip),
            color = clipColors.backgroundTextColor,
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
        }

        ClipSelector(
            isPlaying = isPlaying,
            clipColors = clipColors,
            onPlayClick = onPlayClick,
            onPauseClick = onPauseClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        RowButton(
            text = stringResource(LR.string.podcast_share_clip),
            onClick = onClip,
            colors = ButtonDefaults.buttonColors(backgroundColor = clipColors.buttonColor),
            textColor = clipColors.buttonTextColor,
            elevation = null,
            includePadding = false,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        Image(
            painter = painterResource(IR.drawable.ic_close_sheet),
            contentDescription = stringResource(LR.string.close),
            modifier = Modifier
                .padding(top = 16.dp, end = 16.dp)
                .clickable(onClick = onClose),
        )
    }
}

@Composable
private fun HorizontalClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    episodeCount: Int,
    isPlaying: Boolean,
    useEpisodeArtwork: Boolean,
    clipColors: ClipColors,
    onClip: () -> Unit,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier
            .fillMaxSize()
            .background(clipColors.backgroundColor),
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
                ClipSelector(
                    isPlaying = isPlaying,
                    clipColors = clipColors,
                    onPlayClick = onPlayClick,
                    onPauseClick = onPauseClick,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
                RowButton(
                    text = stringResource(LR.string.podcast_share_clip),
                    onClick = onClip,
                    colors = ButtonDefaults.buttonColors(backgroundColor = clipColors.buttonColor),
                    textColor = clipColors.buttonTextColor,
                    elevation = null,
                    includePadding = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp),
                )
            }
        }
        TextH30(
            text = stringResource(LR.string.podcast_create_clip),
            color = clipColors.backgroundTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
        )
        Image(
            painter = painterResource(IR.drawable.ic_close_sheet),
            contentDescription = stringResource(LR.string.close),
            modifier = Modifier
                .clickable(onClick = onClose)
                .padding(top = 24.dp, end = 16.dp),
        )
    }
}

@ShowkaseComposable(name = "ShareClipPageVertical", group = "Clip")
@Preview(name = "ShareClipPageVertical")
@Composable
fun ShareClipPageVerticalPreview() = ShareClipPage(
    episode = PodcastEpisode(
        uuid = "episode-id",
        podcastUuid = "podcast-id",
        publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
        title = "Episode title",
    ),
    podcast = Podcast(
        uuid = "podcast-id",
        title = "Podcast title",
        episodeFrequency = "monthly",
    ),
    episodeCount = 120,
    isPlaying = false,
    useEpisodeArtwork = true,
    clipColors = ClipColors(Color(0xFF931B17)),
    onClip = {},
    onPlayClick = {},
    onPauseClick = {},
    onClose = {},
)

@ShowkaseComposable(name = "ShareClipVerticalSmallPage", group = "Clip")
@Preview(name = "ShareClipVerticalSmallPage", device = "spec:width=360dp,height=640dp,dpi=320,orientation=portrait")
@Composable
fun ShareClipPageVerticalSmallPreview() = ShareClipPage(
    episode = PodcastEpisode(
        uuid = "episode-id",
        podcastUuid = "podcast-id",
        publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
        title = "Episode title",
    ),
    podcast = Podcast(
        uuid = "podcast-id",
        title = "Podcast title",
        episodeFrequency = "monthly",
    ),
    episodeCount = 120,
    isPlaying = false,
    useEpisodeArtwork = true,
    clipColors = ClipColors(Color(0xFF931B17)),
    onClip = {},
    onPlayClick = {},
    onPauseClick = {},
    onClose = {},
)

@ShowkaseComposable(name = "ShareClipHorizontalPage", group = "Clip")
@Preview(name = "ShareClipHorizontalPage", device = "spec:width=420dp,height=900dp,dpi=420,orientation=landscape")
@Composable
fun ShareClipPageHorizontalPreview() = ShareClipPage(
    episode = PodcastEpisode(
        uuid = "episode-id",
        podcastUuid = "podcast-id",
        publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
        title = "Episode title",
    ),
    podcast = Podcast(
        uuid = "podcast-id",
        title = "Podcast title",
        episodeFrequency = "monthly",
    ),
    episodeCount = 120,
    isPlaying = false,
    useEpisodeArtwork = true,
    clipColors = ClipColors(Color(0xFF931B17)),
    onClip = {},
    onPlayClick = {},
    onPauseClick = {},
    onClose = {},
)

@ShowkaseComposable(name = "ShareClipHorizontalSmallPage", group = "Clip")
@Preview(name = "ShareClipHorizontalSmallPage", device = "spec:width=360dp,height=640dp,dpi=320,orientation=landscape")
@Composable
fun ShareClipPageHorizontalSmallPreview() = ShareClipPage(
    episode = PodcastEpisode(
        uuid = "episode-id",
        podcastUuid = "podcast-id",
        publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
        title = "Episode title",
    ),
    podcast = Podcast(
        uuid = "podcast-id",
        title = "Podcast title",
        episodeFrequency = "monthly",
    ),
    episodeCount = 120,
    isPlaying = false,
    useEpisodeArtwork = true,
    clipColors = ClipColors(Color(0xFF931B17)),
    onClip = {},
    onPlayClick = {},
    onPauseClick = {},
    onClose = {},
)
