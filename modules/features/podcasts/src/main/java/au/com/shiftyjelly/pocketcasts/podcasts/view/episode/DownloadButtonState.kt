package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

sealed class DownloadButtonState {
    data class NotDownloaded(val downloadSize: String) : DownloadButtonState()
    object Queued : DownloadButtonState()
    data class Downloading(val progressPercent: Float) : DownloadButtonState()
    data class Downloaded(val downloadSize: String) : DownloadButtonState()
    object Errored : DownloadButtonState()
}
