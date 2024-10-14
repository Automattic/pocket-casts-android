package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.to.RatingStats
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import kotlin.time.Duration

data class EndOfYearStats(
    val playedEpisodeCount: Int,
    val completedEpisodeCount: Int,
    val playedPodcastIds: List<String>,
    val playbackTime: Duration,
    val lastYearPlaybackTime: Duration,
    val topPodcasts: List<TopPodcast>,
    val longestEpisode: LongestEpisode?,
    val ratingStats: RatingStats,
) {
    val playedPodcastCount get() = playedPodcastIds.size
}
