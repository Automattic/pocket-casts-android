package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode

internal sealed interface DownloadAction {
    data object Cancel : DownloadAction

    data object ConfirmDataUse : DownloadAction

    data class Download(val waitForWifi: Boolean) : DownloadAction

    data object None : DownloadAction

    companion object {
        fun from(episode: BaseEpisode, warnOnMeteredNetwork: Boolean, isUnmeteredConnection: Boolean) = when {
            episode.isDownloadCancellable -> Cancel
            episode.isDownloaded -> None
            warnOnMeteredNetwork && !isUnmeteredConnection -> ConfirmDataUse
            else -> Download(waitForWifi = warnOnMeteredNetwork)
        }
    }
}
