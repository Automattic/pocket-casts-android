package au.com.shiftyjelly.pocketcasts.podcasts.view.components

import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.images.R as IR

enum class PlayButtonType(val drawableId: Int, val label: String) {
    DOWNLOAD(IR.drawable.button_download, "Download"),
    PLAY(IR.drawable.button_play, "Play"),
    PAUSE(IR.drawable.button_pause, "Pause"),
    PLAYED(R.drawable.button_played, "Mark unplayed"),
    PLAYBACK_FAILED(R.drawable.button_retry, "Playback failed"),
    STOP_DOWNLOAD(IR.drawable.ic_downloading, "Stop Downloading")
}
