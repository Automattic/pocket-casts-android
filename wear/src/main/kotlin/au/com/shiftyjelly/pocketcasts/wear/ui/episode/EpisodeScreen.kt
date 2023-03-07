package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.W700
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.DownloadButtonState
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeScreen(
    columnState: ScalingLazyColumnState,
    navigateToPodcast: (podcastUuid: String) -> Unit,
    navigateToUpNextOptions: () -> Unit,
    navigateToConfirmDeleteDownload: () -> Unit,
) {

    val viewModel = hiltViewModel<EpisodeViewModel>()
    val state = viewModel.stateFlow.collectAsState().value
    if (state !is EpisodeViewModel.State.Loaded) return

    val episode = state.episode
    val podcast = state.podcast
    val context = LocalContext.current

    ScalingLazyColumn(
        columnState = columnState,
        modifier = Modifier
            .fillMaxSize()
    ) {

        val headingLineHeight = 14.sp
        item {
            TextP50(
                text = episode.title,
                maxLines = 2,
                fontWeight = W700,
                lineHeight = headingLineHeight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item {
            TextP50(
                text = podcast.title,
                maxLines = 1,
                lineHeight = headingLineHeight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                val downloadSize = Util.formattedBytes(episode.sizeInBytes, context)
                    .replace("-", stringResource(LR.string.podcasts_download_download))
                DownloadButton(
                    tint = state.tintColor,
                    onClick = {
                        if (episode.isDownloaded) {
                            navigateToConfirmDeleteDownload()
                        } else {
                            viewModel.downloadEpisode()
                        }
                    },
                    downloadButtonState = when (episode.episodeStatus) {
                        EpisodeStatusEnum.NOT_DOWNLOADED -> DownloadButtonState.NotDownloaded(
                            downloadSize
                        )

                        EpisodeStatusEnum.QUEUED,
                        EpisodeStatusEnum.WAITING_FOR_WIFI,
                        EpisodeStatusEnum.WAITING_FOR_POWER -> DownloadButtonState.Queued

                        EpisodeStatusEnum.DOWNLOADING -> DownloadButtonState.Downloading(
                            state.downloadProgress
                                ?: 0f
                        )

                        EpisodeStatusEnum.DOWNLOADED -> DownloadButtonState.Downloaded(downloadSize)
                        EpisodeStatusEnum.DOWNLOAD_FAILED -> DownloadButtonState.Errored
                    },
                    modifier = Modifier.size(24.dp)
                )

                PlayButton(
                    backgroundColor = state.tintColor,
                    onClick = {
                        if (state.isPlayingEpisode) {
                            viewModel.pause()
                        } else {
                            viewModel.play()
                        }
                    },
                    isPlaying = state.isPlayingEpisode,
                )

                QueueButton(
                    inUpNext = state.inUpNext,
                    tint = state.tintColor,
                    onClick = {
                        viewModel.onAddToUpNextClicked(
                            showToast = {
                                Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT)
                                    .show()
                            },
                            navigateToUpNextOptions = navigateToUpNextOptions
                        )
                    }
                )
            }
        }

        item {
            EpisodeDateTimeText(
                episode = episode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        val episodeIsMarkedPlayed = episode.playingStatus == EpisodePlayingStatus.COMPLETED
        items(
            listOf(
                EpisodeScreenItem(
                    title = if (episode.isArchived) {
                        LR.string.podcasts_unarchive
                    } else {
                        LR.string.archive
                    },
                    iconRes = if (episode.isArchived) {
                        IR.drawable.ic_unarchive
                    } else {
                        IR.drawable.ic_archive
                    },
                    onClick = viewModel::onArchiveClicked,
                ),
                EpisodeScreenItem(
                    title = if (episode.isStarred) {
                        LR.string.unstar
                    } else {
                        LR.string.star
                    },
                    iconRes = if (episode.isStarred) {
                        IR.drawable.ic_star_filled
                    } else {
                        IR.drawable.ic_star
                    },
                    onClick = viewModel::onStarClicked,
                ),
                EpisodeScreenItem(
                    title = if (episodeIsMarkedPlayed) {
                        LR.string.mark_unplayed
                    } else {
                        LR.string.mark_played
                    },
                    iconRes = if (episodeIsMarkedPlayed) {
                        IR.drawable.ic_markasunplayed
                    } else {
                        IR.drawable.ic_markasplayed
                    },
                    onClick = viewModel::onMarkAsPlayedClicked,
                ),
                EpisodeScreenItem(
                    title = LR.string.go_to_podcast,
                    iconRes = IR.drawable.ic_goto_32,
                    onClick = { navigateToPodcast(podcast.uuid) },
                ),
            )
        ) {
            EpisodeListChip(it)
        }
    }
}

@Composable
private fun EpisodeListChip(episodeScreenItem: EpisodeScreenItem) {
    WatchListChip(
        titleRes = episodeScreenItem.title,
        iconRes = episodeScreenItem.iconRes,
        onClick = episodeScreenItem.onClick,
        modifier = Modifier
            .padding(bottom = 4.dp)
            .fillMaxWidth()
    )
}

private data class EpisodeScreenItem(
    @StringRes val title: Int,
    val iconRes: Int,
    val onClick: () -> Unit,
)
