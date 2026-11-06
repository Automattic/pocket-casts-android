package au.com.shiftyjelly.pocketcasts.compose.episode

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowAction
import au.com.shiftyjelly.pocketcasts.compose.swipe.rememberSwipeRowState
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeRowData
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeRow(
    data: UserEpisodeRowData,
    modifier: Modifier = Modifier,
    isMultiSelecting: Boolean = false,
    isSelected: Boolean = false,
    leadingSwipeActions: List<SwipeRowAction> = emptyList(),
    trailingSwipeActions: List<SwipeRowAction> = emptyList(),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val episode = data.episode
    EpisodeRow(
        title = episode.title,
        header = rememberEpisodeRowHeader(episode),
        status = userEpisodeRowStatus(data, LocalContext.current),
        artwork = { EpisodeRowArtwork(episode, useEpisodeArtwork = true) },
        isGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED,
        isMultiSelecting = isMultiSelecting,
        isSelected = isSelected,
        leadingSwipeActions = leadingSwipeActions,
        trailingSwipeActions = trailingSwipeActions,
        swipeRowState = rememberSwipeRowState(episode.uuid),
        onClick = onClick,
        onLongClick = onLongClick,
        trailing = trailing,
        modifier = modifier,
    )
}

fun userEpisodeRowStatus(data: UserEpisodeRowData, context: Context): EpisodeRowStatus {
    val episode = data.episode
    fun status(textId: Int, iconId: Int? = null) = EpisodeRowStatus(context.getString(textId), iconId = iconId)
    val isBuffering = data.playbackState.episodeUuid == episode.uuid && data.playbackState.isBuffering
    val downloadStatus = when {
        isBuffering -> EpisodeRowStatus(context.getString(LR.string.episode_row_buffering), isBuffering = true)

        episode.playErrorDetails != null -> EpisodeRowStatus(episode.playErrorDetails.orEmpty(), iconId = IR.drawable.ic_failed, isIconTinted = false)

        episode.isDownloaded -> TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context)
            .let { EpisodeRowStatus(it.text, it.description, IR.drawable.ic_downloaded, isIconHighlighted = true) }

        episode.isDownloading -> EpisodeRowStatus(
            text = context.getString(LR.string.episode_row_downloading, data.downloadProgress),
            progress = data.downloadProgress / 100f,
        )

        episode.isDownloadFailure -> status(LR.string.episode_row_download_failed, IR.drawable.ic_download_failed_row)

        episode.isWaitingForPower -> status(LR.string.episode_row_waiting_for_power, IR.drawable.ic_waitingforpower)

        episode.isWaitingForStorage -> status(LR.string.episode_row_waiting_for_storage, IR.drawable.ic_waitingforstorage)

        episode.isWaitingForWifi -> status(LR.string.episode_row_waiting_for_wifi, IR.drawable.ic_waitingforwifi)

        episode.isQueuedForDownload -> status(LR.string.episode_row_queued)

        else -> TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context)
            .let { EpisodeRowStatus(it.text, it.description) }
    }.copy(isUploaded = episode.serverStatus == UserEpisodeServerStatus.UPLOADED)
    return when (episode.serverStatus) {
        UserEpisodeServerStatus.UPLOADING -> downloadStatus.copy(
            text = context.getString(LR.string.episode_row_uploading, data.uploadProgress),
            contentDescription = context.getString(LR.string.episode_row_uploading, data.uploadProgress),
            iconId = null,
            progress = data.uploadProgress / 100f,
        )

        UserEpisodeServerStatus.WAITING_FOR_WIFI -> status(LR.string.episode_row_waiting_for_wifi, IR.drawable.ic_waitingforwifi)

        UserEpisodeServerStatus.QUEUED -> status(LR.string.episode_row_queued_upload)

        else -> downloadStatus
    }.copy(isInUpNext = data.isInUpNext, hasBookmarks = data.hasBookmarks)
}
