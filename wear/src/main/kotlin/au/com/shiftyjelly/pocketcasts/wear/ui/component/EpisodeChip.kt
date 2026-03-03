package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearColors
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeChip(
    episode: BaseEpisode,
    useEpisodeArtwork: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useUpNextIcon: Boolean = true,
    showImage: Boolean = true,
    viewModel: EpisodeChipViewModel = hiltViewModel(),
) {
    // Make sure the episode is always up-to-date
    val observedEpisode by viewModel
        .observeByUuid(episode)
        .collectAsState()
    val queueState by viewModel.upNextQueue.collectAsState(UpNextQueue.State.Empty)
    Content(
        episode = observedEpisode,
        queueState = queueState,
        useEpisodeArtwork = useEpisodeArtwork,
        onClick = onClick,
        modifier = modifier,
        useUpNextIcon = useUpNextIcon,
        showImage = showImage,
    )
}

@Composable
private fun Content(
    episode: BaseEpisode,
    queueState: UpNextQueue.State,
    useEpisodeArtwork: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useUpNextIcon: Boolean = true,
    showImage: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        val upNextQueue = (queueState as? UpNextQueue.State.Loaded)
            ?.queue
            ?: emptyList()
        val isInUpNextQueue = upNextQueue.any { it.uuid == episode.uuid }
        val showUpNextIcon = useUpNextIcon && isInUpNextQueue
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Max),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (showImage) {
                    EpisodeImage(
                        episode = episode,
                        useEpisodeArtwork = useEpisodeArtwork,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .alphaIfArchivedOrPlayed(episode),
                    )
                    IconsRow(
                        showUpNextIcon,
                        episode,
                        Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = episode.title,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colors.onPrimary,
                    style = MaterialTheme.typography.button.merge(
                        TextStyle(
                            platformStyle = PlatformTextStyle(
                                // So we can align the top of the text as closely as possible to the image
                                includeFontPadding = false,
                            ),
                        ),
                    ),
                    maxLines = 2,
                    modifier = Modifier.alphaIfArchivedOrPlayed(episode),
                )
                val shortDate = episode.publishedDate.toLocalizedFormatPattern("dd MMM")
                val timeLeft = TimeHelper.getTimeLeft(
                    currentTimeMs = episode.playedUpToMs,
                    durationMs = episode.durationMs.toLong(),
                    inProgress = episode.isInProgress,
                    context = LocalContext.current,
                ).text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!showImage) {
                        IconsRow(
                            showUpNextIcon,
                            episode,
                            modifier = Modifier
                                .padding(end = if (showUpNextIcon || episode.isDownloaded) 4.dp else 0.dp),
                        )
                    }
                    Text(
                        text = "$shortDate • $timeLeft",
                        color = MaterialTheme.colors.onSecondary,
                        style = MaterialTheme.typography.caption2,
                        modifier = Modifier.alphaIfArchivedOrPlayed(episode),
                    )
                }
                episode.playErrorDetails?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(IR.drawable.ic_alert_small),
                            contentDescription = stringResource(LR.string.podcast_episode_playback_error),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colors.onSecondary,
                        )
                        Text(
                            text = it,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = MaterialTheme.colors.onSecondary,
                            style = MaterialTheme.typography.caption3,
                            modifier = Modifier.padding(start = 5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconsRow(
    showUpNextIcon: Boolean,
    episode: BaseEpisode,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = spacedBy(4.dp),
        modifier = modifier,
    ) {
        if (showUpNextIcon) {
            Icon(
                painter = painterResource(R.drawable.ic_upnext),
                contentDescription = stringResource(LR.string.episode_in_up_next),
                tint = WearColors.upNextIcon,
                modifier = Modifier.size(12.dp),
            )
        }

        if (episode.isDownloaded) {
            Icon(
                painter = painterResource(R.drawable.ic_downloaded),
                contentDescription = stringResource(LR.string.downloaded),
                tint = WearColors.downloadedIcon,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

private const val ALPHA_ARCHIVED_OR_PLAYED = 0.6f

private fun Modifier.alphaIfArchivedOrPlayed(episode: BaseEpisode) = if (episode.isArchived || episode.isFinished) alpha(ALPHA_ARCHIVED_OR_PLAYED) else this

@Preview
@Composable
private fun EpisodeChipPreview() {
    val episode = PodcastEpisode(
        uuid = "preview-episode",
        publishedDate = Date(System.currentTimeMillis() - 86400000L), // 1 day ago
        title = "Sample Episode",
        podcastUuid = "podcast-uuid",
        downloadStatus = EpisodeDownloadStatus.Downloaded,
        playedUpTo = 300.0,
        duration = 1800.0,
    )
    val episodeLongTitle = episode.copy(
        title = "Sample Episode: With a title that is quite long and wrap across multiple lines",
    )
    val playedEpisode = episode.copy(
        title = "Played Episode",
        playingStatus = EpisodePlayingStatus.COMPLETED,
        playedUpTo = 1700.0,
    )
    val archivedEpisode = episode.copy(
        title = "Archived Episode",
        isArchived = true,
    )

    WearAppTheme {
        Column(
            verticalArrangement = spacedBy(8.dp),
            modifier = Modifier.background(Color.Black).padding(8.dp),
        ) {
            Content(
                episode = episode,
                queueState = UpNextQueue.State.Empty,
                useEpisodeArtwork = true,
                onClick = {},
                showImage = true,
            )
            Content(
                episode = episodeLongTitle,
                queueState = UpNextQueue.State.Empty,
                useEpisodeArtwork = true,
                onClick = {},
                showImage = true,
            )
            Content(
                episode = episode,
                queueState = UpNextQueue.State.Empty,
                useEpisodeArtwork = true,
                onClick = {},
                showImage = false,
            )
            Content(
                episode = playedEpisode,
                queueState = UpNextQueue.State.Empty,
                useEpisodeArtwork = true,
                onClick = {},
                showImage = true,
            )
            Content(
                episode = archivedEpisode,
                queueState = UpNextQueue.State.Empty,
                useEpisodeArtwork = true,
                onClick = {},
                showImage = true,
            )
        }
    }
}
