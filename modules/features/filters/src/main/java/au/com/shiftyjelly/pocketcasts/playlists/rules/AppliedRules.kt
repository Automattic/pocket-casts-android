package au.com.shiftyjelly.pocketcasts.playlists.rules

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
        SmartRules(
            episodeStatus = episodeStatus ?: SmartRules.Default.episodeStatus,
            downloadStatus = downloadStatus ?: SmartRules.Default.downloadStatus,
            mediaType = mediaType ?: SmartRules.Default.mediaType,
            releaseDate = releaseDate ?: SmartRules.Default.releaseDate,
            starred = starred ?: SmartRules.Default.starred,
            podcasts = podcasts ?: SmartRules.Default.podcasts,
            episodeDuration = episodeDuration ?: SmartRules.Default.episodeDuration,
        )
    } else {
        null
    }

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
