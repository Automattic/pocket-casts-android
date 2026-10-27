package au.com.shiftyjelly.pocketcasts.views.buttons

import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus

enum class PlayButtonMenuItem {
    PlayNext,
    PlayLast,
    Download,
    Stream,
    StopDownloading,
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
