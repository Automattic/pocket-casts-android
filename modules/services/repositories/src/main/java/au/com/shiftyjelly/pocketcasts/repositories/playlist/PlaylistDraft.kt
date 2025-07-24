package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule

data class PlaylistDraft(
    val title: String,
    val rules: SmartRules,
) {
    companion object {
        val NewReleases = PlaylistDraft(
            title = "New Releases",
            rules = SmartRules.Default.copy(
                releaseDate = ReleaseDateRule.Last2Weeks,
            ),
        )

        val InProgress = PlaylistDraft(
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
