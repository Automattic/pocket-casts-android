package au.com.shiftyjelly.pocketcasts.servers.refresh

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshPodcastFeedRequest(
    @field:Json(name = "podcast_uuid") val podcastUuid: String? = null,
    @field:Json(name = "device") override var deviceId: String? = null,
    @field:Json(name = "datetime") override var datetime: String? = null,
    @field:Json(name = "v") override var version: String? = null,
    @field:Json(name = "av") override var appVersion: String? = null,
    @field:Json(name = "ac") override var appVersionCode: String? = null,
    @field:Json(name = "h") override var hash: String? = null,
    @field:Json(name = "dt") override var deviceType: String? = null,
    @field:Json(name = "c") override var country: String? = null,
    @field:Json(name = "l") override var language: String? = null,
    @field:Json(name = "m") override var model: String? = null
) : BaseRequest()
