package au.com.shiftyjelly.pocketcasts.playlists.rules

import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule

data class AppliedRules(
    val episodeStatus: EpisodeStatusRule?,
    val downloadStatus: DownloadStatusRule?,
    val mediaType: MediaTypeRule?,
    val releaseDate: ReleaseDateRule?,
    val starred: StarredRule?,
    val podcasts: PodcastsRule?,
    val episodeDuration: EpisodeDurationRule?,
) {
    val isAnyRuleApplied = episodeStatus != null ||
        downloadStatus != null ||
        mediaType != null ||
        releaseDate != null ||
        starred != null ||
        podcasts != null ||
        episodeDuration != null

    fun toSmartRules() = if (isAnyRuleApplied) {
        toSmartRulesOrDefault()
    } else {
        null
    }

    fun toSmartRulesOrDefault() = SmartRules(
        episodeStatus = episodeStatus ?: SmartRules.Default.episodeStatus,
        downloadStatus = downloadStatus ?: SmartRules.Default.downloadStatus,
        mediaType = mediaType ?: SmartRules.Default.mediaType,
        releaseDate = releaseDate ?: SmartRules.Default.releaseDate,
        starred = starred ?: SmartRules.Default.starred,
        podcasts = podcasts ?: SmartRules.Default.podcasts,
        episodeDuration = episodeDuration ?: SmartRules.Default.episodeDuration,
    )

    companion object {
        val Empty = AppliedRules(
            episodeStatus = null,
            downloadStatus = null,
            mediaType = null,
            releaseDate = null,
            starred = null,
            podcasts = null,
            episodeDuration = null,
        )
    }
}
