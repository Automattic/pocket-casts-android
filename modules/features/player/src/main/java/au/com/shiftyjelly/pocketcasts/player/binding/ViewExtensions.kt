package au.com.shiftyjelly.pocketcasts.player.binding

import au.com.shiftyjelly.pocketcasts.player.view.PlayerSeekBar
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration.Companion.milliseconds

fun PlayerSeekBar.setPlaybackState(playbackState: PlaybackState?) {
    if (playbackState == null) {
        return
    }
    setDuration(playbackState.durationMs.milliseconds)
    setPlaybackSpeed(playbackState.playbackSpeed)
    setCurrentTime(playbackState.positionMs.milliseconds)
    setChapters(playbackState.chapters)
    setTintColor(playbackState.podcast?.tintColorForDarkBg, Theme.ThemeType.DARK)
}
