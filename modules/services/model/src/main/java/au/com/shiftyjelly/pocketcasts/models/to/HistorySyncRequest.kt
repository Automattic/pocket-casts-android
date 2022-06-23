package au.com.shiftyjelly.pocketcasts.models.to

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HistorySyncRequest(
    @field:Json(name = "changes") val changes: List<HistorySyncChange>,
    @field:Json(name = "deviceTime") val deviceTime: Long,
    @field:Json(name = "serverModified") val serverModified: Long,
    @field:Json(name = "version") val version: Int
)

@JsonClass(generateAdapter = true)
data class HistorySyncResponse(
    @field:Json(name = "serverModified") val serverModified: Long,
    @field:Json(name = "lastCleared") val lastCleared: Long,
    @field:Json(name = "changes") val changes: List<HistorySyncChange>?
) {

    fun hasChanged(existingServerModified: Long): Boolean {
        return serverModified != 0L && serverModified != existingServerModified
    }
}

@JsonClass(generateAdapter = true)
data class HistorySyncChange(
    @field:Json(name = "action") val action: Int,
    @field:Json(name = "episode") val episode: String? = null,
    @field:Json(name = "modifiedAt") val modifiedAt: String,
    @field:Json(name = "podcast") val podcast: String? = null,
    @field:Json(name = "published") val published: String? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "url") val url: String? = null
)
