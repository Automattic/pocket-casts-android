package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class UpNextSyncResponse(
    @field:Json(name = "serverModified") val serverModified: Long,
    @field:Json(name = "episodes") val episodes: List<Episode>?
) {

    @JsonClass(generateAdapter = true)
    data class Episode(
        @field:Json(name = "uuid") val uuid: String,
        @field:Json(name = "title") val title: String?,
        @field:Json(name = "url") val url: String?,
        @field:Json(name = "podcast") val podcast: String?,
        @field:Json(name = "published") val published: String?
    ) {
        fun toSkeletonEpisode(podcastUuid: String): au.com.shiftyjelly.pocketcasts.models.entity.Episode {
            return Episode(
                uuid = uuid,
                publishedDate = published?.parseIsoDate() ?: Date(),
                addedDate = Date(),
                playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                episodeStatus = EpisodeStatusEnum.NOT_DOWNLOADED,
                title = title ?: "",
                downloadUrl = url ?: "",
                podcastUuid = podcastUuid
            )
        }
    }

    fun hasChanged(existingServerModified: Long): Boolean {
        return serverModified != 0L && serverModified != existingServerModified
    }
}
