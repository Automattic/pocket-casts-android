package au.com.shiftyjelly.pocketcasts.podcasts.helper

import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButtonType
import com.google.android.material.button.MaterialButton

object BindingAdapters {

    @BindingAdapter("show")
    @JvmStatic
    fun setShow(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    @BindingAdapter("showIfPresent")
    @JvmStatic
    fun setShowIfPresent(view: View, string: String?) {
        view.visibility = if (string.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    @BindingAdapter(value = ["backgroundTintInt"])
    @JvmStatic
    fun setBackgroundTintInt(button: MaterialButton, tintColor: Int) {
        button.backgroundTintList = ColorStateList.valueOf(tintColor)
    }

    @BindingAdapter(value = ["episode", "buttonType", "color", "fromListUuid"])
    @JvmStatic
    fun setupPlayButton(button: PlayButton, episode: Playable, buttonType: PlayButtonType, color: Int, fromListUuid: String?) {
        button.setButtonType(episode, buttonType, color, fromListUuid)
    }

    @BindingAdapter("shortTime")
    @JvmStatic
    fun setShortTime(textView: TextView, duration: Double?) {
        if (duration == null) {
            textView.text = null
        } else {
            textView.text = TimeHelper.getTimeDurationShortString(timeMs = duration.toLong() * 1000, context = textView.context)
        }
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
}
