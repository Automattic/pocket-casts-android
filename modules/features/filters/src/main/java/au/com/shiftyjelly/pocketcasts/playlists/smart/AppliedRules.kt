package au.com.shiftyjelly.pocketcasts.playlists.smart

import au.com.shiftyjelly.pocketcasts.models.type.SmartRules

data class AppliedRules(
    val episodeStatus: SmartRules.EpisodeStatusRule?,
    val downloadStatus: SmartRules.DownloadStatusRule?,
    val mediaType: SmartRules.MediaTypeRule?,
    val releaseDate: SmartRules.ReleaseDateRule?,
    val starred: SmartRules.StarredRule?,
    val podcasts: SmartRules.PodcastsRule?,
    val episodeDuration: SmartRules.EpisodeDurationRule?,
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
        episodeStatus = episodeStatus ?: SmartRules.Companion.Default.episodeStatus,
        downloadStatus = downloadStatus ?: SmartRules.Companion.Default.downloadStatus,
        mediaType = mediaType ?: SmartRules.Companion.Default.mediaType,
        releaseDate = releaseDate ?: SmartRules.Companion.Default.releaseDate,
        starred = starred ?: SmartRules.Companion.Default.starred,
        podcasts = podcasts ?: SmartRules.Companion.Default.podcasts,
        episodeDuration = episodeDuration ?: SmartRules.Companion.Default.episodeDuration,
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
