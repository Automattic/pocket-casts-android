package au.com.shiftyjelly.pocketcasts.models.to

import java.util.Date

data class EpisodeItem(
    val uuid: String,
    val title: String,
    val duration: Double,
    val publishedAt: Date,
    val podcastUuid: String,
    val podcastTitle: String,
)
