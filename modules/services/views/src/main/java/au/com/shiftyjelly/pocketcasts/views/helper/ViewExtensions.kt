package au.com.shiftyjelly.pocketcasts.views.helper

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.TextView
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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

fun TextView.setLongStyleDate(date: Date?) {
    text = date?.toLocalizedFormatLongStyle().orEmpty()
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
