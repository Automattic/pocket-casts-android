package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastListResponse(
    @field:Json(name = "podcasts") val podcasts: List<PodcastResponse>?,
    @field:Json(name = "folders") val folders: List<FolderResponse>?
)

@JsonClass(generateAdapter = true)
data class PodcastResponse(
    @field:Json(name = "uuid") val uuid: String?,
    @field:Json(name = "episodesSortOrder") val episodesSortOrder: Int?,
    @field:Json(name = "autoStartFrom") val autoStartFrom: Int?,
    @field:Json(name = "autoSkipLast") val autoSkipLast: Int?,
    @field:Json(name = "folderUuid") val folderUuid: String?,
    @field:Json(name = "sortPosition") val sortPosition: Int?,
    @field:Json(name = "dateAdded") val dateAdded: String?
)
