package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeRowData
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal enum class FileStatusIcon(@DrawableRes val drawableId: Int) {
    PlaybackFailed(IR.drawable.ic_failed),
    Downloaded(IR.drawable.ic_downloaded),
    DownloadFailed(IR.drawable.ic_download_failed_row),
    WaitingForPower(IR.drawable.ic_waitingforpower),
    WaitingForStorage(IR.drawable.ic_waitingforstorage),
    WaitingForWifi(IR.drawable.ic_waitingforwifi),
}

internal sealed interface FileStatusText {
    data class Resource(@StringRes val id: Int, val progress: Int? = null) : FileStatusText

    data class Raw(val text: String) : FileStatusText

    data object TimeLeft : FileStatusText
}

internal data class FileStatus(
    val text: FileStatusText,
    val icon: FileStatusIcon? = null,
    val progressCircle: ProgressCircleState? = null,
    val isBuffering: Boolean = false,
    val isUploaded: Boolean = false,
) {
    companion object {
        fun from(data: UserEpisodeRowData, hideErrorDetails: Boolean): FileStatus {
            val episode = data.episode
            val downloadStatus = when {
                data.playbackState.episodeUuid == episode.uuid && data.playbackState.isBuffering -> FileStatus(
                    text = FileStatusText.Resource(LR.string.episode_row_buffering),
                    isBuffering = true,
                )

                episode.playErrorDetails != null -> FileStatus(
                    text = FileStatusText.Raw(if (hideErrorDetails) "" else episode.playErrorDetails.orEmpty()),
                    icon = FileStatusIcon.PlaybackFailed,
                )

                episode.isDownloaded -> FileStatus(FileStatusText.TimeLeft, FileStatusIcon.Downloaded)

                episode.isDownloading -> FileStatus(
                    text = FileStatusText.Resource(LR.string.episode_row_downloading, data.downloadProgress),
                    progressCircle = ProgressCircleState.forPercent(data.downloadProgress / 100f),
                )

                episode.isDownloadFailure -> FileStatus(
                    text = FileStatusText.Resource(LR.string.episode_row_download_failed),
                    icon = FileStatusIcon.DownloadFailed,
                )

                episode.isWaitingForPower -> FileStatus(
                    text = FileStatusText.Resource(LR.string.episode_row_waiting_for_power),
                    icon = FileStatusIcon.WaitingForPower,
                )

                episode.isWaitingForStorage -> FileStatus(
                    text = FileStatusText.Resource(LR.string.episode_row_waiting_for_storage),
                    icon = FileStatusIcon.WaitingForStorage,
                )

                episode.isWaitingForWifi -> FileStatus(
                    text = FileStatusText.Resource(LR.string.episode_row_waiting_for_wifi),
                    icon = FileStatusIcon.WaitingForWifi,
                )

                episode.isQueuedForDownload -> FileStatus(FileStatusText.Resource(LR.string.episode_row_queued))

                else -> FileStatus(FileStatusText.TimeLeft)
            }.copy(isUploaded = episode.serverStatus == UserEpisodeServerStatus.UPLOADED)

            return when (episode.serverStatus) {
                UserEpisodeServerStatus.UPLOADING -> downloadStatus.copy(
                    text = FileStatusText.Resource(LR.string.episode_row_uploading, data.uploadProgress),
                    icon = null,
                    progressCircle = ProgressCircleState.forPercent(data.uploadProgress / 100f),
                )

                UserEpisodeServerStatus.WAITING_FOR_WIFI -> FileStatus(
                    text = FileStatusText.Resource(LR.string.episode_row_waiting_for_wifi),
                    icon = FileStatusIcon.WaitingForWifi,
                )

                UserEpisodeServerStatus.QUEUED -> FileStatus(FileStatusText.Resource(LR.string.episode_row_queued_upload))

                else -> downloadStatus
            }
        }
    }
}
