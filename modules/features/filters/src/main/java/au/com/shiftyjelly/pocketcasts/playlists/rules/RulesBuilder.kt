package au.com.shiftyjelly.pocketcasts.playlists.rules

import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class RulesBuilder(
    val useAllPodcasts: Boolean,
    val selectedPodcasts: Set<String>,
    val episodeStatusRule: SmartRules.EpisodeStatusRule,
    val releaseDateRule: SmartRules.ReleaseDateRule,
    val isEpisodeDurationConstrained: Boolean,
    val minEpisodeDuration: Duration,
    val maxEpisodeDuration: Duration,
    val downloadStatusRule: SmartRules.DownloadStatusRule,
    val mediaTypeRule: SmartRules.MediaTypeRule,
    val useStarredEpisode: Boolean,
) {
    val podcastsRule
        get() = if (useAllPodcasts) {
            SmartRules.PodcastsRule.Any
        } else {
            SmartRules.PodcastsRule.Selected(selectedPodcasts.toList())
        }

    val episodeDurationRule
        get() = if (isEpisodeDurationConstrained) {
            SmartRules.EpisodeDurationRule.Constrained(minEpisodeDuration, maxEpisodeDuration)
        } else {
            SmartRules.EpisodeDurationRule.Any
        }

    val starredRule
        get() = if (useStarredEpisode) {
            SmartRules.StarredRule.Starred
        } else {
            SmartRules.StarredRule.Any
        }

    fun decrementMinDuration(): RulesBuilder {
        val minDuration = minEpisodeDuration
        val newDuration = minDuration - if (minDuration > 5.minutes) 5.minutes else 1.minutes
        return if (newDuration >= Duration.ZERO && newDuration < maxEpisodeDuration) {
            copy(minEpisodeDuration = newDuration)
        } else {
            this
        }
    }

    fun incrementMinDuration(): RulesBuilder {
        val minDuration = minEpisodeDuration
        val newDuration = minDuration + if (minDuration >= 5.minutes) 5.minutes else 1.minutes
        return if (newDuration >= Duration.ZERO && newDuration < maxEpisodeDuration) {
            copy(minEpisodeDuration = newDuration)
        } else {
            this
        }
    }

    fun decrementMaxDuration(): RulesBuilder {
        val maxDuration = maxEpisodeDuration
        val newDuration = maxDuration - if (maxDuration > 5.minutes) 5.minutes else 1.minutes
        return if (newDuration > Duration.ZERO && newDuration > minEpisodeDuration) {
            copy(maxEpisodeDuration = newDuration)
        } else {
            this
        }
    }

    fun incrementMaxDuration(): RulesBuilder {
        val maxDuration = maxEpisodeDuration
        val newDuration = maxDuration + if (maxDuration >= 5.minutes) 5.minutes else 1.minutes
        return if (newDuration > Duration.ZERO && newDuration > minEpisodeDuration) {
            copy(maxEpisodeDuration = newDuration)
        } else {
            this
        }
    }

    companion object {
        val Empty = RulesBuilder(
            useAllPodcasts = true,
            selectedPodcasts = emptySet(),
            episodeStatusRule = SmartRules.Default.episodeStatus,
            releaseDateRule = SmartRules.Default.releaseDate,
            isEpisodeDurationConstrained = false,
            minEpisodeDuration = 20.minutes,
            maxEpisodeDuration = 40.minutes,
            downloadStatusRule = SmartRules.Default.downloadStatus,
            mediaTypeRule = SmartRules.MediaTypeRule.Any,
            useStarredEpisode = false,
        )
    }
}
