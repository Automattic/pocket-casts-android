package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EpisodeSyncRequest(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "podcast") val podcast: String,
    @field:Json(name = "position") val position: Long,
    @field:Json(name = "duration") val duration: Long,
    @field:Json(name = "status") val status: Int
) {
    companion object {
        const val STATUS_UNPLAYED = 1
        const val STATUS_IN_PROGRESS = 2
        const val STATUS_COMPLETE = 3
    }
}

class EpisodeSyncResponse
