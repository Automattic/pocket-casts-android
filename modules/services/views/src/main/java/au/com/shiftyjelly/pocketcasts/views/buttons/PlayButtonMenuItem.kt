package au.com.shiftyjelly.pocketcasts.views.buttons

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class PlayButtonMenuItem(@StringRes val titleId: Int) {
    PlayNext(LR.string.play_next),
    PlayLast(LR.string.play_last),
    Download(LR.string.download),
    Stream(LR.string.stream),
    StopDownloading(LR.string.stop_downloading),
    ;

    companion object {
        fun visibleItems(downloadStatus: EpisodeDownloadStatus, buttonType: PlayButtonType): List<PlayButtonMenuItem> {
            val notRequested = downloadStatus == EpisodeDownloadStatus.DownloadNotRequested
            return entries.filter { item ->
                when (item) {
                    PlayNext, PlayLast -> true
                    Download -> notRequested && buttonType != PlayButtonType.DOWNLOAD
                    Stream -> notRequested && buttonType == PlayButtonType.DOWNLOAD
                    StopDownloading -> downloadStatus.isCancellable
                }
            }
        }
    }
}
