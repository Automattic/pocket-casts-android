package au.com.shiftyjelly.pocketcasts.views.buttons

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.images.R as IR

enum class PlayButtonType(val drawableId: Int, val label: String) {
    DOWNLOAD(IR.drawable.button_download, "Download"),
    PLAY(IR.drawable.button_play, "Play"),
    PAUSE(IR.drawable.button_pause, "Pause"),
    PLAYED(IR.drawable.button_played, "Mark unplayed"),
    PLAYBACK_FAILED(IR.drawable.button_retry, "Playback failed"),
    STOP_DOWNLOAD(IR.drawable.ic_downloading, "Stop Downloading"),
    ;

    companion object {
        fun from(episode: BaseEpisode, streamByDefault: Boolean): PlayButtonType {
            return when {
                episode.lastPlaybackFailed() -> PLAYBACK_FAILED
                episode.playing -> PAUSE
                episode.isFinished -> PLAYED
                episode.isDownloaded -> PLAY
                episode.isDownloading || episode.isDownloadPending -> if (streamByDefault) PLAY else STOP_DOWNLOAD
                !streamByDefault -> DOWNLOAD
                else -> PLAY
            }
        }
    }
}
