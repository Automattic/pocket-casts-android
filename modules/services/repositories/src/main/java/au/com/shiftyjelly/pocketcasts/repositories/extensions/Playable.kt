package au.com.shiftyjelly.pocketcasts.repositories.extensions

import android.content.Context
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import androidx.core.text.set
import androidx.core.text.toSpannable
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import java.util.Locale

fun Playable.getSummaryText(dateFormatter: RelativeDateFormatter, @ColorInt tintColor: Int, showDuration: Boolean, context: Context): Spannable {
    return when (this) {
        is Episode -> episodeSummaryText(episode = this, dateFormatter = dateFormatter, tintColor = tintColor, showDuration = showDuration, context = context)
        is UserEpisode -> userEpisodeSummaryText(userEpisode = this, dateFormatter = dateFormatter, context = context)
        else -> "".toSpannable()
    }
}

private fun episodeSummaryText(episode: Episode, dateFormatter: RelativeDateFormatter, @ColorInt tintColor: Int, showDuration: Boolean, context: Context): Spannable {
    val resources = context.resources
    var startText = when (episode.episodeType) {
        is Episode.EpisodeType.Regular -> Episode.seasonPrefix(episode.episodeType, episode.season, episode.number, resources)
        is Episode.EpisodeType.Bonus -> resources.getString(R.string.episode_bonus).uppercase(
            Locale.getDefault()
        )
        is Episode.EpisodeType.Trailer -> (
            if ((episode.season ?: 0) > 0) resources.getString(R.string.episode_season_trailer, episode.season) else resources.getString(
                R.string.episode_trailer
            )
            ).uppercase(Locale.getDefault())
    }

    if (startText != null) {
        startText += " • "
    } else {
        startText = ""
    }

    val timeLeft = TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context)
    val duration = if (showDuration) " • ${timeLeft.text}" else ""
    val text = "$startText${dateFormatter.format(episode.publishedDate, context.resources)}$duration".toSpannable()
    if (episode.episodeType != Episode.EpisodeType.Regular) {
        text[0, startText.replace(" • ", "").length] = ForegroundColorSpan(tintColor)
    }

    return text
}

private fun userEpisodeSummaryText(userEpisode: UserEpisode, dateFormatter: RelativeDateFormatter, context: Context): Spannable {
    return dateFormatter.format(userEpisode.publishedDate, context.resources).toSpannable()
}
