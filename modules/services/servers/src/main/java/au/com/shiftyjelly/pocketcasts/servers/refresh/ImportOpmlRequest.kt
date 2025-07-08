package au.com.shiftyjelly.pocketcasts.servers.refresh

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImportOpmlRequest(
    @Json(name = "urls") val urls: List<String>? = null,
    @Json(name = "poll_uuids") val pollUuids: List<String>? = null,
    @Json(name = "device") override var deviceId: String? = null,
    @Json(name = "datetime") override var datetime: String? = null,
    @Json(name = "v") override var version: String? = null,
    @Json(name = "av") override var appVersion: String? = null,
    @Json(name = "ac") override var appVersionCode: String? = null,
    @Json(name = "h") override var hash: String? = null,
    @Json(name = "dt") override var deviceType: String? = null,
    @Json(name = "c") override var country: String? = null,
    @Json(name = "l") override var language: String? = null,
    @Json(name = "m") override var model: String? = null,
) : BaseRequest()
