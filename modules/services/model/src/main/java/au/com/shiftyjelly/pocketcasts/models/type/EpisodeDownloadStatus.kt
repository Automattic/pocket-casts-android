package au.com.shiftyjelly.pocketcasts.models.type

enum class EpisodeDownloadStatus(
    val isCancellable: Boolean,
    val isPending: Boolean,
) {
    NotDownloaded(
        isCancellable = false,
        isPending = false,
    ),
    Queued(
        isCancellable = true,
        isPending = true,
    ),
    WaitingForWifi(
        isCancellable = true,
        isPending = true,
    ),
    WaitingForPower(
        isCancellable = true,
        isPending = true,
    ),
    Downloading(
        isCancellable = false,
        isPending = false,
    ),
    DownloadFailed(
        isCancellable = false,
        isPending = false,
    ),
    Downloaded(
        isCancellable = false,
        isPending = false,
    ),
}
