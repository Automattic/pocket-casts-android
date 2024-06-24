package au.com.shiftyjelly.pocketcasts.clip

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import kotlin.time.Duration

data class Clip(
    val episode: PodcastEpisode,
    val range: Range,
) {
    data class Range(
        val start: Duration,
        val end: Duration,
    )
}
