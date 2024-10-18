package au.com.shiftyjelly.pocketcasts.player.binding

import android.view.View
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.player.view.PlayerSeekBar
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.lottie.LottieAnimationView
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import okhttp3.HttpUrl

fun View.showIfPresent(url: HttpUrl?) {
    visibility = if (url?.toString().isNullOrEmpty()) View.GONE else View.VISIBLE
}

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

fun PlayerSeekBar.setSeekBarState(
    duration: Duration,
    position: Duration,
    chapters: Chapters,
    playbackSpeed: Double,
    adjustDuration: Boolean,
    tintColor: Int,
    bufferedUpTo: Int,
    isBuffering: Boolean,
    theme: Theme.ThemeType,
) {
    setDuration(duration)
    setPlaybackSpeed(playbackSpeed)
    setCurrentTime(position)
    setChapters(chapters)
    setTintColor(tintColor, theme)
    setAdjustDuration(adjustDuration)
    this.isBuffering = isBuffering
    bufferedUpToInSecs = bufferedUpTo / 1000
}

fun LottieAnimationView.playIfTrue(play: Boolean?) {
    if (play != null && play) {
        if (!isAnimating) {
            playAnimation()
        }
    } else {
        pauseAnimation()
        progress = 0.5f
    }
}
