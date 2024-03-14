package au.com.shiftyjelly.pocketcasts.player.binding

import android.view.View
import au.com.shiftyjelly.pocketcasts.player.view.PlayerSeekBar
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.lottie.LottieAnimationView
import okhttp3.HttpUrl

object BindingAdapters {

    fun View.showIfPresent(url: HttpUrl?) {
        visibility = if (url?.toString().isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    fun PlayerSeekBar.setPlaybackState(playbackState: PlaybackState?) {
        if (playbackState == null) {
            return
        }
        setDurationMs(playbackState.durationMs)
        setCurrentTimeMs(playbackState.positionMs)
        setTintColor(playbackState.podcast?.tintColorForDarkBg, Theme.ThemeType.DARK)
    }

    fun PlayerSeekBar.setSeekBarState(durationMs: Int, positionMs: Int, tintColor: Int, bufferedUpTo: Int, isBuffering: Boolean, theme: Theme.ThemeType) {
        setDurationMs(durationMs)
        setCurrentTimeMs(positionMs)
        setTintColor(tintColor, theme)
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
}
