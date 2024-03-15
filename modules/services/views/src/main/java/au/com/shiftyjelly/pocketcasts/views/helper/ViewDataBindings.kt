package au.com.shiftyjelly.pocketcasts.views.helper

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.BindingConversion
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object ViewDataBindings {

    @BindingConversion
    @JvmStatic
    fun convertBooleanToVisibility(visible: Boolean): Int {
        return if (visible) View.VISIBLE else View.GONE
    }

    @BindingAdapter("circle")
    @JvmStatic
    fun clipToCircle(view: View, circle: Boolean) {
        if (!circle) {
            return
        }
        view.clipToOutline = true
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
    }

    fun View.toCircle(circle: Boolean) {
        if (!circle) {
            return
        }
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
    }

    @BindingAdapter("clipToOutline")
    @JvmStatic
    fun setClipToOutline(view: View, clip: Boolean) {
        view.clipToOutline = clip
    }

    @BindingAdapter("android:textColor")
    @JvmStatic
    fun setTextColor(textView: TextView, color: Int) {
        textView.setTextColor(color)
    }

    /**
     * Date format: d MMMM yyyy
     * For example: 4 January 2017
     */
    @BindingAdapter("mediumDate")
    @JvmStatic
    fun setMediumDate(textView: TextView, date: Date?) {
        if (date == null) {
            textView.text = ""
        } else {
            textView.text = date.toLocalizedFormatLongStyle()
        }
    }

    fun TextView.setLongStyleDate(date: Date?) {
        text = date?.toLocalizedFormatLongStyle().orEmpty()
    }

    @BindingAdapter("timeLeft")
    @JvmStatic
    fun setTimeLeft(textView: TextView, episode: BaseEpisode) {
        if (episode is UserEpisode && episode.serverStatus == UserEpisodeServerStatus.MISSING) {
            textView.text = textView.resources.getString(LR.string.podcast_episode_file_not_uploaded)
        } else {
            val timeLeft = TimeHelper.getTimeLeft(
                currentTimeMs = episode.playedUpToMs,
                durationMs = episode.durationMs.toLong(),
                inProgress = episode.isInProgress,
                context = textView.context,
            )
            textView.text = timeLeft.text
            textView.contentDescription = timeLeft.description
        }
    }

    fun TextView.setEpisodeTimeLeft(episode: BaseEpisode) {
        if (episode is UserEpisode && episode.serverStatus == UserEpisodeServerStatus.MISSING) {
            text = resources.getString(LR.string.podcast_episode_file_not_uploaded)
        } else {
            val timeLeft = TimeHelper.getTimeLeft(
                currentTimeMs = episode.playedUpToMs,
                durationMs = episode.durationMs.toLong(),
                inProgress = episode.isInProgress,
                context = context,
            )
            text = timeLeft.text
            contentDescription = timeLeft.description
        }
    }

    @BindingAdapter("timeLong")
    @JvmStatic
    fun setTimeLong(textView: TextView, time: Int) {
        val timeLeft = TimeHelper.getTimeLeft(
            currentTimeMs = 0,
            durationMs = time.toLong(),
            inProgress = false,
            context = textView.context,
        )
        textView.text = timeLeft.text
        textView.contentDescription = timeLeft.description
    }

    fun TextView.applyTimeLong(time: Int) {
        val timeLeft = TimeHelper.getTimeLeft(
            currentTimeMs = 0,
            durationMs = time.toLong(),
            inProgress = false,
            context = context,
        )
        text = timeLeft.text
        contentDescription = timeLeft.description
    }
}
