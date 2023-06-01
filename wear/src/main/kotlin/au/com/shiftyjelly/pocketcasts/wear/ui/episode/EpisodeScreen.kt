package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.W700
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.parseAsHtml
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.DownloadButtonState
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ExpandableText
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeScreen(
    columnState: ScalingLazyColumnState,
    navigateToPodcast: (podcastUuid: String) -> Unit,
    navigateToUpNextOptions: () -> Unit,
    navigateToConfirmDeleteDownload: () -> Unit,
    navigateToRemoveFromUpNextNotification: () -> Unit,
    navigateToStreamingConfirmation: () -> Unit,
    navigateToNowPlaying: () -> Unit,
) {

    val viewModel = hiltViewModel<EpisodeViewModel>()
    val state = viewModel.stateFlow.collectAsState().value
    if (state !is EpisodeViewModel.State.Loaded) return

    val showNowPlaying = viewModel.showNowPlaying.collectAsState(false).value
    if (showNowPlaying) navigateToNowPlaying()

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
            Text(
                text = episode.title,
                maxLines = 2,
                fontWeight = W700,
                lineHeight = headingLineHeight,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.title3,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        if (podcast != null) {
            item {
                TextP50(
                    text = podcast.title,
                    maxLines = 1,
                    color = MaterialTheme.colors.onSecondary,
                    style = MaterialTheme.typography.body1,
                    lineHeight = headingLineHeight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            navigateToPodcast(podcast.uuid)
                        }
                )
            }
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

                val tintColor = state.tintColor ?: MaterialTheme.colors.primary

                DownloadButton(
                    tint = tintColor,
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
                            state.downloadProgress ?: 0f
                        )

                        EpisodeStatusEnum.DOWNLOADED -> DownloadButtonState.Downloaded(downloadSize)
                        EpisodeStatusEnum.DOWNLOAD_FAILED -> DownloadButtonState.Errored
                    },
                    modifier = Modifier.size(24.dp)
                )

                PlayButton(
                    backgroundColor = tintColor,
                    onClick = {
                        if (state.isPlayingEpisode) {
                            viewModel.onPauseClicked()
                        } else {
                            viewModel.onPlayClicked(navigateToStreamingConfirmation)
                        }
                    },
                    isPlaying = state.isPlayingEpisode,
                )

                QueueButton(
                    inUpNext = state.inUpNext,
                    tint = tintColor,
                    onClick = {
                        viewModel.onUpNextClicked(
                            onRemoveFromUpNext = navigateToRemoveFromUpNextNotification,
                            navigateToUpNextOptions = navigateToUpNextOptions
                        )
                    }
                )
            }
        }

        state.errorData?.let {
            item {
                EpisodeErrorDetails(it)
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

        item {
            if (state.showNotes != null) {
                val coroutineScope = rememberCoroutineScope()
                Column {
                    ExpandableText(
                        text = state.showNotes.parseAsHtml().toString(),
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                        onClick = { isExpanded ->
                            if (!isExpanded) {
                                val thisItemIndex = 4
                                val visibleIndices = columnState.state.layoutInfo.visibleItemsInfo.map { it.index }
                                val isPreviousItemVisible = visibleIndices.any { it == thisItemIndex - 1 }

                                // If the previous item is not visible, scroll to this item to ensure that the top of
                                // the show notes is still visible after collapsing is complete. Otherwise, it is
                                // possible for collapsing the show notes to cause all of the content on the screen
                                // to change, leaving the user confused about what happened and where they have ended up.
                                if (!isPreviousItemVisible) {
                                    coroutineScope.launch {
                                        columnState.state.animateScrollToItem(thisItemIndex)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        val episodeIsMarkedPlayed = episode.playingStatus == EpisodePlayingStatus.COMPLETED
        items(
            listOf(
                if (episode is PodcastEpisode) {
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
                    )
                } else null,
                if (episode is PodcastEpisode) {
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
                    )
                } else null,
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
                if (podcast != null) {
                    EpisodeScreenItem(
                        title = LR.string.go_to_podcast,
                        iconRes = IR.drawable.ic_goto_32,
                        onClick = { navigateToPodcast(podcast.uuid) },
                    )
                } else null,
            )
                .filterNotNull()
        ) {
            EpisodeListChip(it)
        }
    }
}

@Composable
private fun EpisodeListChip(episodeScreenItem: EpisodeScreenItem) {
    WatchListChip(
        title = stringResource(episodeScreenItem.title),
        iconRes = episodeScreenItem.iconRes,
        onClick = episodeScreenItem.onClick,
        modifier = Modifier
            .padding(bottom = 4.dp)
            .fillMaxWidth()
    )
}

@Composable
private fun EpisodeErrorDetails(
    errorData: EpisodeViewModel.State.Loaded.ErrorData,
) {
    val padding = 8.dp
    Row(
        modifier = Modifier
            .padding(start = padding, end = padding, bottom = padding)
            .background(
                color = MaterialTheme.colors.secondary,
                shape = RoundedCornerShape(10.dp)
            ),
    ) {
        Box(modifier = Modifier.padding(start = padding, top = 10.dp)) {
            Icon(
                painter = painterResource(errorData.errorIconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        if (errorData.errorDescription == null) {
            Text(
                text = stringResource(errorData.errorTitleRes),
                color = MaterialTheme.colors.onSecondary,
                style = MaterialTheme.typography.caption2,
                modifier = Modifier.padding(padding)
            )
        } else {
            Text(
                text = errorData.errorDescription,
                color = MaterialTheme.colors.onSecondary,
                style = MaterialTheme.typography.caption3,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

private data class EpisodeScreenItem(
    @StringRes val title: Int,
    val iconRes: Int,
    val onClick: () -> Unit,
)
