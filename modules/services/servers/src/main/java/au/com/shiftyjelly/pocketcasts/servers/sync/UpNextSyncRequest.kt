package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpNextSyncRequest(
    @Json(name = "deviceTime") val deviceTime: Long,
    @Json(name = "version") val version: String,
    @Json(name = "upNext") val upNext: UpNext,
) {

    @JsonClass(generateAdapter = true)
    data class UpNext(
        @Json(name = "serverModified") val serverModified: Long,
        @Json(name = "changes") val changes: List<Change>,
    )

    @JsonClass(generateAdapter = true)
    data class Change(
        @Json(name = "action") val action: Int,
        @Json(name = "modified") val modified: Long,
        @Json(name = "uuid") val uuid: String? = null,
        @Json(name = "title") val title: String? = null,
        @Json(name = "url") val url: String? = null,
        @Json(name = "published") val published: String? = null,
        @Json(name = "podcast") val podcast: String? = null,
        @Json(name = "episodes") val episodes: List<ChangeEpisode>? = null,
    )

    @JsonClass(generateAdapter = true)
    data class ChangeEpisode(
        @Json(name = "uuid") val uuid: String,
        @Json(name = "title") val title: String?,
        @Json(name = "url") val url: String?,
        @Json(name = "podcast") val podcast: String?,
        @Json(name = "published") val published: String?,
    )
}
