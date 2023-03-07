package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.W700
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.DownloadButtonState
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.wear.theme.theme
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object EpisodeScreen {
    const val episodeUuidArgument = "episodeUuid"
    const val route = "episode/{$episodeUuidArgument}"

    fun navigateRoute(episodeUuid: String) = "episode/$episodeUuid"
}

@Composable
fun EpisodeScreen(
    columnState: ScalingLazyColumnState,
    navigateToPodcast: (podcastUuid: String) -> Unit,
) {

    val viewModel = hiltViewModel<EpisodeViewModel>()
    val state = viewModel.stateFlow.collectAsState().value
    if (state !is EpisodeViewModel.State.Loaded) return

    if (state.showUpNextOptions) {
        UpNextOptionsScreen(
            viewModel = viewModel,
            state = state,
        )
    } else {
        MainEpisodeScreen(
            viewModel = viewModel,
            state = state,
            columnState = columnState,
            navigateToPodcast = navigateToPodcast,
        )
    }
}

@Composable
fun MainEpisodeScreen(
    columnState: ScalingLazyColumnState,
    navigateToPodcast: (podcastUuid: String) -> Unit,
    viewModel: EpisodeViewModel,
    state: EpisodeViewModel.State.Loaded,
) {

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
                    onClick = viewModel::onDownloadClicked,
                    downloadButtonState = when (episode.episodeStatus) {
                        EpisodeStatusEnum.NOT_DOWNLOADED -> DownloadButtonState.NotDownloaded(downloadSize)
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
                                Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                )
            }
        }

        item {
            DateTimeText(
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
                    title = LR.string.play_next,
                    iconRes = IR.drawable.ic_upnext_movetotop,
                    onClick = {},
                ),
                EpisodeScreenItem(
                    title = LR.string.play_last,
                    iconRes = IR.drawable.ic_upnext_movetobottom,
                    onClick = {},
                ),
                EpisodeScreenItem(
                    title = if (episode.isArchived) { LR.string.podcasts_unarchive } else { LR.string.archive },
                    iconRes = if (episode.isArchived) { IR.drawable.ic_unarchive } else { IR.drawable.ic_archive },
                    onClick = viewModel::onArchiveClicked,
                ),
                EpisodeScreenItem(
                    title = if (episode.isStarred) { LR.string.unstar } else { LR.string.star },
                    iconRes = if (episode.isStarred) { IR.drawable.ic_star_filled } else { IR.drawable.ic_star },
                    onClick = viewModel::onStarClicked,
                ),
                EpisodeScreenItem(
                    title = if (episodeIsMarkedPlayed) { LR.string.mark_unplayed } else { LR.string.mark_played },
                    iconRes = if (episodeIsMarkedPlayed) { IR.drawable.ic_markasunplayed } else { IR.drawable.ic_markasplayed },
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

@Suppress("UNUSED_PARAMETER")
@Composable
private fun UpNextOptionsScreen(
    viewModel: EpisodeViewModel,
    state: EpisodeViewModel.State.Loaded
) {
    Column {
        state.upNextOptions.forEach { upNextOption ->
            with(upNextOption) {
                WatchListChip(
                    titleRes = titleRes,
                    iconRes = iconRes,
                    onClick = onClick,
                )
            }
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
    )
}

@Composable
private fun WatchListChip(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
) {
    val title = stringResource(titleRes)
    Chip(
        onClick = onClick,
        colors = ChipDefaults.secondaryChipColors(
            secondaryContentColor = MaterialTheme.theme.colors.primaryText02
        ),
        label = {
            Text(title)
        },
        secondaryLabel = {
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}

private data class EpisodeScreenItem(
    @StringRes val title: Int,
    val iconRes: Int,
    val onClick: () -> Unit,
)

@Composable
private fun DownloadButton(
    tint: Color,
    onClick: () -> Unit,
    downloadButtonState: DownloadButtonState,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clickable(
            onClick = onClick,
        ),
    ) {

        when (downloadButtonState) {
            is DownloadButtonState.Downloading -> downloadButtonState.progressPercent
            is DownloadButtonState.Queued -> 0f
            is DownloadButtonState.Downloaded,
            is DownloadButtonState.NotDownloaded,
            DownloadButtonState.Errored -> null
        }?.let { progressPercent ->
            CircularProgressIndicator(
                progress = progressPercent,
                strokeWidth = 2.dp,
                indicatorColor = tint,
                trackColor = tint.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize()
            )
        }

        Icon(
            painter = painterResource(
                when (downloadButtonState) {
                    is DownloadButtonState.Downloaded -> IR.drawable.ic_downloaded
                    DownloadButtonState.Queued,
                    is DownloadButtonState.Downloading -> IR.drawable.ic_downloading

                    DownloadButtonState.Errored -> IR.drawable.ic_retry
                    is DownloadButtonState.NotDownloaded -> IR.drawable.ic_download
                }
            ),
            tint = tint,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PlayButton(
    isPlaying: Boolean,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color = backgroundColor)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(
                if (isPlaying) {
                    IR.drawable.button_pause
                } else {
                    IR.drawable.button_play
                }
            ),
            contentDescription = stringResource(LR.string.play),
            modifier = Modifier.size(52.dp)
        )
    }
}

@Composable
private fun QueueButton(inUpNext: Boolean, tint: Color, onClick: () -> Unit) {
    val icon = if (inUpNext) {
        IR.drawable.ic_upnext_remove
    } else {
        IR.drawable.ic_upnext_playnext
    }

    Icon(
        painter = painterResource(icon),
        tint = tint,
        contentDescription = stringResource(LR.string.podcasts_up_next),
        modifier = Modifier
            .size(24.dp)
            .clickable { onClick() }
    )
}

@Composable
private fun DateTimeText(episode: Episode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        val shortDate = episode.publishedDate.toLocalizedFormatPattern("dd MMM")
        val timeLeft = TimeHelper.getTimeLeft(
            episode.playedUpToMs,
            episode.durationMs.toLong(),
            episode.isInProgress,
            context
        ).text
        TextC70("$shortDate • $timeLeft")

        val downloadSize = Util.formattedBytes(episode.sizeInBytes, context)
        TextC70(downloadSize)
    }
}
