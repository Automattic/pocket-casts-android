package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpNextSyncRequest(
    @field:Json(name = "deviceTime") val deviceTime: Long,
    @field:Json(name = "version") val version: String,
    @field:Json(name = "upNext") val upNext: UpNext
) {

    @JsonClass(generateAdapter = true)
    data class UpNext(
        @field:Json(name = "serverModified") val serverModified: Long,
        @field:Json(name = "changes") val changes: List<Change>
    )

    @JsonClass(generateAdapter = true)
    data class Change(
        @field:Json(name = "action") val action: Int,
        @field:Json(name = "modified") val modified: Long,
        @field:Json(name = "uuid") val uuid: String? = null,
        @field:Json(name = "title") val title: String? = null,
        @field:Json(name = "url") val url: String? = null,
        @field:Json(name = "published") val published: String? = null,
        @field:Json(name = "podcast") val podcast: String? = null,
        @field:Json(name = "episodes") val episodes: List<ChangeEpisode>? = null
    )

    @JsonClass(generateAdapter = true)
    data class ChangeEpisode(
        @field:Json(name = "uuid") val uuid: String,
        @field:Json(name = "title") val title: String?,
        @field:Json(name = "url") val url: String?,
        @field:Json(name = "podcast") val podcast: String?,
        @field:Json(name = "published") val published: String?
    )
}
