package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.to.PlaybackPodcastStats

data class PlaybackStats(
    /** Per-podcast listening, most listened first. */
    val podcasts: List<PlaybackPodcastStats>,
    /** Listening seconds per month of the year, January first. Always 12 entries. */
    val monthlySeconds: List<Double>,
    val lastYearSeconds: Double,
    /** Episodes listened to in the year. */
    val startedEpisodeCount: Int,
    /** Episodes listened to in the year that were finished (marked played or 90% in). */
    val completedEpisodeCount: Int,
)
