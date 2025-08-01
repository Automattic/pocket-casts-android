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
    val rules: SmartRules,
) {
    companion object {
        val NewReleases = SmartPlaylistDraft(
            title = "New Releases",
            rules = SmartRules.Default.copy(
                releaseDate = ReleaseDateRule.Last2Weeks,
            ),
        )

        val InProgress = SmartPlaylistDraft(
            title = "In Progress",
            rules = SmartRules.Default.copy(
                episodeStatus = EpisodeStatusRule(
                    unplayed = false,
                    inProgress = true,
                    completed = false,
                ),
                releaseDate = ReleaseDateRule.LastMonth,
            ),
        )
    }
}
