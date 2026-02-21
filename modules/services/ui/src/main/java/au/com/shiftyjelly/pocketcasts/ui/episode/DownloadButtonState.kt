package au.com.shiftyjelly.pocketcasts.ui.episode

/**
 * Represents the different states of an episode download button.
 * Shared utility used across mobile, wear, and automotive modules.
 */
sealed class DownloadButtonState {
    data class NotDownloaded(val downloadSize: String) : DownloadButtonState()
    object Queued : DownloadButtonState()
    data class Downloading(val progressPercent: Float) : DownloadButtonState()
    data class Downloaded(val downloadSize: String) : DownloadButtonState()
    object Errored : DownloadButtonState()
}
