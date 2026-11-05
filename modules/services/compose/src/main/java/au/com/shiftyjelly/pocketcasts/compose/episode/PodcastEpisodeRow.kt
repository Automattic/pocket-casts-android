package au.com.shiftyjelly.pocketcasts.compose.episode

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowAction
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowState
import au.com.shiftyjelly.pocketcasts.compose.swipe.rememberSwipeRowState
import au.com.shiftyjelly.pocketcasts.compose.text.toAnnotatedString
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowData
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeRow(
    episode: PodcastEpisode,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
    rowData: EpisodeRowData? = null,
    isMultiSelecting: Boolean = false,
    isSelected: Boolean = false,
    leadingSwipeActions: List<SwipeRowAction> = emptyList(),
    trailingSwipeActions: List<SwipeRowAction> = emptyList(),
    swipeRowState: SwipeRowState = rememberSwipeRowState(episode.uuid),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val header = rememberEpisodeRowHeader(episode)
    val status = episodeRowStatus(episode, rowData, context)
    EpisodeRow(
        title = episode.title,
        header = header,
        status = status,
        artwork = { EpisodeRowArtwork(episode, useEpisodeArtwork) },
        isStarred = episode.isStarred,
        isGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived,
        showVideoIcon = episode.showsVideoIcon(rowData?.hasHlsAlternateEnclosure == true),
        isMultiSelecting = isMultiSelecting,
        isSelected = isSelected,
        contentDescription = episodeRowContentDescription(episode, header, status, context),
        leadingSwipeActions = leadingSwipeActions,
        trailingSwipeActions = trailingSwipeActions,
        swipeRowState = swipeRowState,
        onClick = onClick,
        onLongClick = onLongClick,
        trailing = trailing,
        modifier = modifier,
    )
}

@Composable
fun EpisodeRowArtwork(
    episode: BaseEpisode,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    EpisodeImage(
        episode = episode,
        useEpisodeArtwork = useEpisodeArtwork,
        placeholderType = PlaceholderType.Small,
        corners = 4.dp,
        modifier = modifier
            .fillMaxSize()
            .shadow(2.dp, RoundedCornerShape(4.dp)),
    )
}

@Composable
fun rememberEpisodeRowHeader(episode: BaseEpisode): AnnotatedString {
    val context = LocalContext.current
    val tint = MaterialTheme.theme.colors.primaryIcon01
    val formatter = remember(context) { RelativeDateFormatter(context) }
    return remember(episode, tint, formatter) {
        episode.getSummaryText(formatter, tint.toArgb(), showDuration = false, context).toAnnotatedString()
    }
}

fun episodeRowStatus(episode: PodcastEpisode, rowData: EpisodeRowData?, context: Context): EpisodeRowStatus {
    val timeLeft = TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context)
    val downloadProgress = rowData?.downloadProgress ?: 0
    val status = when {
        episode.isDownloaded -> EpisodeRowStatus(timeLeft.text, timeLeft.description, IR.drawable.ic_downloaded, isIconHighlighted = true)

        episode.isDownloading -> EpisodeRowStatus(context.getString(LR.string.episode_row_downloading, downloadProgress), progress = downloadProgress / 100f)

        episode.isDownloadFailure -> EpisodeRowStatus(context.getString(LR.string.episode_row_download_failed), iconId = IR.drawable.ic_download_failed_row)

        episode.isWaitingForPower -> EpisodeRowStatus(context.getString(LR.string.episode_row_waiting_for_power), iconId = IR.drawable.ic_waitingforpower)

        episode.isWaitingForStorage -> EpisodeRowStatus(context.getString(LR.string.episode_row_waiting_for_storage), iconId = IR.drawable.ic_waitingforstorage)

        episode.isWaitingForWifi -> EpisodeRowStatus(context.getString(LR.string.episode_row_waiting_for_wifi), iconId = IR.drawable.ic_waitingforwifi)

        episode.isQueuedForDownload -> EpisodeRowStatus(context.getString(LR.string.episode_row_queued), iconId = IR.drawable.ic_waitingforwifi)

        episode.isArchived -> {
            val archived = context.getString(LR.string.archived)
            EpisodeRowStatus("$archived · ${timeLeft.text}", "$archived. ${timeLeft.description}", IR.drawable.ic_archive)
        }

        episode.playErrorDetails != null -> EpisodeRowStatus(episode.playErrorDetails.orEmpty(), iconId = IR.drawable.ic_alert_small)

        else -> EpisodeRowStatus(timeLeft.text, timeLeft.description)
    }
    val playbackState = rowData?.playbackState
    val isBuffering = playbackState?.episodeUuid == episode.uuid && playbackState.isBuffering
    return if (isBuffering) {
        EpisodeRowStatus(context.getString(LR.string.episode_row_buffering), isBuffering = true)
    } else {
        status
    }.copy(
        isInUpNext = rowData?.isInUpNext == true,
        hasBookmarks = rowData?.hasBookmarks == true,
    )
}

private fun episodeRowContentDescription(
    episode: PodcastEpisode,
    header: AnnotatedString,
    status: EpisodeRowStatus,
    context: Context,
) = buildList {
    add(episode.title)
    add(header.text)
    add(status.contentDescription)
    if (episode.isInProgress) {
        add(context.getString(LR.string.in_progress))
    } else if (status.isInUpNext) {
        add(context.getString(LR.string.episode_in_up_next))
    }
    if (episode.isDownloaded) {
        add(context.getString(LR.string.downloaded))
    }
    if (episode.isDownloading) {
        add(context.getString(LR.string.episode_downloading))
    }
    if (episode.isStarred) {
        add(context.getString(LR.string.starred))
    }
}.joinToString(separator = ". ", postfix = ".")
