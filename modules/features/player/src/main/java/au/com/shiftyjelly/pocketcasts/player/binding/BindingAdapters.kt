package au.com.shiftyjelly.pocketcasts.player.binding

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.BindingConversion
import au.com.shiftyjelly.pocketcasts.player.view.PlayerSeekBar
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.lottie.LottieAnimationView
import okhttp3.HttpUrl

object BindingAdapters {

    @BindingAdapter("show")
    @JvmStatic
    fun setShow(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun View.showIfPresent(url: HttpUrl?) {
        visibility = if (url?.toString().isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    @BindingAdapter("backgroundTint")
    @JvmStatic
    fun setBackgroundTint(view: View, color: Int) {
        if (color == 0) {
            return
        }
        ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(color))
    }

    @BindingAdapter("tint")
    @JvmStatic
    fun setTint(imageView: ImageView, color: Int) {
        if (color == 0) {
            return
        }
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color))
    }

    @BindingConversion
    @JvmStatic
    fun convertBooleanToVisibility(visible: Boolean): Int {
        return if (visible) View.VISIBLE else View.GONE
    }

    @BindingAdapter("playbackState")
    @JvmStatic
    fun setSeekBarPlaybackState(seekBar: PlayerSeekBar, playbackState: PlaybackState?) {
        if (playbackState == null) {
            return
        }
        seekBar.setDurationMs(playbackState.durationMs)
        seekBar.setCurrentTimeMs(playbackState.positionMs)
        seekBar.setTintColor(playbackState.podcast?.tintColorForDarkBg, Theme.ThemeType.DARK)
    }

    fun PlayerSeekBar.setSeekBarState(durationMs: Int, positionMs: Int, tintColor: Int, bufferedUpTo: Int, isBuffering: Boolean, theme: Theme.ThemeType) {
        setDurationMs(durationMs)
        setCurrentTimeMs(positionMs)
        setTintColor(tintColor, theme)
        this.isBuffering = isBuffering
        bufferedUpToInSecs = bufferedUpTo / 1000
    }

    @BindingAdapter("play")
    @JvmStatic
    fun setLottieAnimationViewPlay(view: LottieAnimationView, play: Boolean?) {
        if (play != null && play) {
            if (!view.isAnimating) {
                view.playAnimation()
            }
        } else {
            view.pauseAnimation()
            view.progress = 0.5f
        }
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
