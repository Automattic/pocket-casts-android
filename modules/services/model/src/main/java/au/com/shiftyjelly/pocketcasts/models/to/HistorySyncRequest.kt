package au.com.shiftyjelly.pocketcasts.models.to

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HistorySyncRequest(
    @Json(name = "changes") val changes: List<HistorySyncChange>,
    @Json(name = "deviceTime") val deviceTime: Long,
    @Json(name = "serverModified") val serverModified: Long,
    @Json(name = "version") val version: Int,
)

@JsonClass(generateAdapter = true)
data class HistorySyncResponse(
    @Json(name = "serverModified") val serverModified: Long,
    @Json(name = "lastCleared") val lastCleared: Long,
    @Json(name = "changes") val changes: List<HistorySyncChange>?,
) {
    fun hasChanged(existingServerModified: Long): Boolean {
        return serverModified != 0L && serverModified != existingServerModified
    }
}

@JsonClass(generateAdapter = true)
data class HistorySyncChange(
    @Json(name = "action") val action: Int,
    @Json(name = "episode") val episode: String? = null,
    @Json(name = "modifiedAt") val modifiedAt: String,
    @Json(name = "podcast") val podcast: String? = null,
    @Json(name = "published") val published: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "url") val url: String? = null,
)
