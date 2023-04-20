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
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import java.util.Locale

fun Episode.getSummaryText(dateFormatter: RelativeDateFormatter, @ColorInt tintColor: Int, showDuration: Boolean, context: Context): Spannable {
    return when (this) {
        is PodcastEpisode -> episodeSummaryText(episode = this, dateFormatter = dateFormatter, tintColor = tintColor, showDuration = showDuration, context = context)
        is UserEpisode -> userEpisodeSummaryText(userEpisode = this, dateFormatter = dateFormatter)
        else -> "".toSpannable()
    }
}

private fun episodeSummaryText(episode: PodcastEpisode, dateFormatter: RelativeDateFormatter, @ColorInt tintColor: Int, showDuration: Boolean, context: Context): Spannable {
    val resources = context.resources
    var startText = when (episode.episodeType) {
        is PodcastEpisode.EpisodeType.Regular -> PodcastEpisode.seasonPrefix(episode.episodeType, episode.season, episode.number, resources)
        is PodcastEpisode.EpisodeType.Bonus -> resources.getString(R.string.episode_bonus).uppercase(
            Locale.getDefault()
        )
        is PodcastEpisode.EpisodeType.Trailer -> (
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
    val text = "$startText${dateFormatter.format(episode.publishedDate)}$duration".toSpannable()
    if (episode.episodeType != PodcastEpisode.EpisodeType.Regular) {
        text[0, startText.replace(" • ", "").length] = ForegroundColorSpan(tintColor)
    }

    return text
}

private fun userEpisodeSummaryText(userEpisode: UserEpisode, dateFormatter: RelativeDateFormatter): Spannable {
    return dateFormatter.format(userEpisode.publishedDate).toSpannable()
}
