package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule

data class SmartPlaylistDraft(
    val title: String,
    val episodeStatus: EpisodeStatusRule? = null,
    val downloadStatus: DownloadStatusRule? = null,
    val mediaType: MediaTypeRule? = null,
    val releaseDate: ReleaseDateRule? = null,
    val starred: StarredRule? = null,
    val podcasts: PodcastsRule? = null,
    val episodeDuration: EpisodeDurationRule? = null,
) {
    val rules = SmartRules(
        episodeStatus = episodeStatus ?: SmartRules.Default.episodeStatus,
        downloadStatus = downloadStatus ?: SmartRules.Default.downloadStatus,
        mediaType = mediaType ?: SmartRules.Default.mediaType,
        releaseDate = releaseDate ?: SmartRules.Default.releaseDate,
        starred = starred ?: SmartRules.Default.starred,
        podcasts = podcasts ?: SmartRules.Default.podcasts,
        episodeDuration = episodeDuration ?: SmartRules.Default.episodeDuration,
    )

    companion object {
        val NewReleases = SmartPlaylistDraft(
            title = "New Releases",
            releaseDate = ReleaseDateRule.Last2Weeks,
        )

        val InProgress = SmartPlaylistDraft(
            title = "In Progress",
            episodeStatus = EpisodeStatusRule(
                unplayed = false,
                inProgress = true,
                completed = false,
            ),
            releaseDate = ReleaseDateRule.LastMonth,
        )
    }
}
