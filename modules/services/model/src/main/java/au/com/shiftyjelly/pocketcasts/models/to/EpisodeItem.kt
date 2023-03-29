package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import java.util.Date

data class EpisodeItem(
    val uuid: String,
    val title: String,
    val duration: Double,
    val publishedAt: Date,
    val podcastUuid: String,
    val podcastTitle: String,
) {
    fun toEpisode() =
        Episode(
            uuid = uuid,
            title = title,
            duration = duration,
            publishedDate = publishedAt,
            podcastUuid = podcastUuid
        )
}
