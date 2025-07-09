package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class UpNextSyncResponse(
    @Json(name = "serverModified") val serverModified: Long,
    @Json(name = "episodes") val episodes: List<Episode>?,
) {

    @JsonClass(generateAdapter = true)
    data class Episode(
        @Json(name = "uuid") val uuid: String,
        @Json(name = "title") val title: String?,
        @Json(name = "url") val url: String?,
        @Json(name = "podcast") val podcast: String?,
        @Json(name = "published") val published: String?,
    ) {
        fun toSkeletonEpisode(podcastUuid: String): PodcastEpisode {
            return PodcastEpisode(
                uuid = uuid,
                publishedDate = published?.parseIsoDate() ?: Date(),
                addedDate = Date(),
                playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                episodeStatus = EpisodeStatusEnum.NOT_DOWNLOADED,
                title = title ?: "",
                downloadUrl = url ?: "",
                podcastUuid = podcastUuid,
            )
        }
    }

    fun hasChanged(existingServerModified: Long): Boolean {
        return serverModified != 0L && serverModified != existingServerModified
    }
}
