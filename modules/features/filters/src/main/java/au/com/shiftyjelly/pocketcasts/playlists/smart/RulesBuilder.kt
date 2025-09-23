package au.com.shiftyjelly.pocketcasts.playlists.smart

import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class RulesBuilder(
    val useAllPodcasts: Boolean,
    val selectedPodcasts: Set<String>,
    val episodeStatusRule: EpisodeStatusRule,
    val releaseDateRule: ReleaseDateRule,
    val isEpisodeDurationConstrained: Boolean,
    val minEpisodeDuration: Duration,
    val maxEpisodeDuration: Duration,
    val downloadStatusRule: DownloadStatusRule,
    val mediaTypeRule: MediaTypeRule,
    val useStarredEpisode: Boolean,
) {
    val podcastsRule
        get() = if (useAllPodcasts) {
            PodcastsRule.Any
        } else {
            PodcastsRule.Selected(selectedPodcasts)
        }

    val episodeDurationRule
        get() = if (isEpisodeDurationConstrained) {
            EpisodeDurationRule.Constrained(minEpisodeDuration, maxEpisodeDuration)
        } else {
            EpisodeDurationRule.Any
        }

    val starredRule
        get() = if (useStarredEpisode) {
            StarredRule.Starred
        } else {
            StarredRule.Any
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

    fun applyRules(rules: SmartRules) = copy(
        useAllPodcasts = when (rules.podcasts) {
            is PodcastsRule.Any -> true
            is PodcastsRule.Selected -> false
        },
        selectedPodcasts = when (val podcasts = rules.podcasts) {
            is PodcastsRule.Any -> selectedPodcasts
            is PodcastsRule.Selected -> podcasts.uuids
        },
        episodeStatusRule = rules.episodeStatus,
        releaseDateRule = rules.releaseDate,
        isEpisodeDurationConstrained = when (rules.episodeDuration) {
            is EpisodeDurationRule.Any -> false
            is EpisodeDurationRule.Constrained -> true
        },
        minEpisodeDuration = when (val duration = rules.episodeDuration) {
            is EpisodeDurationRule.Any -> minEpisodeDuration
            is EpisodeDurationRule.Constrained -> duration.longerThan
        },
        maxEpisodeDuration = when (val duration = rules.episodeDuration) {
            is EpisodeDurationRule.Any -> maxEpisodeDuration
            is EpisodeDurationRule.Constrained -> duration.shorterThan
        },
        downloadStatusRule = rules.downloadStatus,
        mediaTypeRule = rules.mediaType,
        useStarredEpisode = when (rules.starred) {
            StarredRule.Any -> false
            StarredRule.Starred -> true
        },
    )

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
            mediaTypeRule = MediaTypeRule.Any,
            useStarredEpisode = false,
        )
    }
}
