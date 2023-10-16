package au.com.shiftyjelly.pocketcasts.player.binding

import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.BindingConversion
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
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

    @BindingAdapter("readMore")
    @JvmStatic
    fun setReadMore(textView: TextView, collapsedLines: Int) {
        textView.maxLines = collapsedLines
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.setOnClickListener {
            val transition = ChangeBounds().apply {
                duration = 200
                interpolator = FastOutSlowInInterpolator()
            }
            TransitionManager.beginDelayedTransition(textView.parent as ViewGroup, transition)
            textView.maxLines = if (textView.maxLines > collapsedLines) collapsedLines else Int.MAX_VALUE
        }
    }

    @BindingAdapter("showIfPresent")
    @JvmStatic
    fun setShowIfPresent(view: View, string: String?) {
        view.visibility = if (string.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    @BindingAdapter("showIfPresent")
    @JvmStatic
    fun setShowIfPresent(view: View, url: HttpUrl?) {
        view.visibility = if (url?.toString().isNullOrEmpty()) View.GONE else View.VISIBLE
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

    @BindingAdapter("tint")
    @JvmStatic
    fun setTint(progressBar: ProgressBar, color: Int) {
        if (color == 0) {
            return
        }
        progressBar.progressBackgroundTintList = ColorStateList.valueOf(color)
        progressBar.progressTintList = ColorStateList.valueOf(color)
        progressBar.secondaryProgressTintList = ColorStateList.valueOf(color)
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

    @BindingAdapter("duration", "position", "tintColor", "bufferedUpTo", "isBuffering", "theme")
    @JvmStatic
    fun setSeekBarState(seekBar: PlayerSeekBar, durationMs: Int, positionMs: Int, tintColor: Int, bufferedUpTo: Int, isBuffering: Boolean, theme: Theme.ThemeType) {
        seekBar.setDurationMs(durationMs)
        seekBar.setCurrentTimeMs(positionMs)
        seekBar.setTintColor(tintColor, theme)
        seekBar.isBuffering = isBuffering
        seekBar.bufferedUpToInSecs = bufferedUpTo / 1000
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

    @BindingAdapter("layout_constraintWidth_percent")
    @JvmStatic
    fun setConstraintWidthPercent(view: View, percent: Float) {
        view.updateLayoutParams<ConstraintLayout.LayoutParams> { matchConstraintPercentWidth = percent }
    }
}
