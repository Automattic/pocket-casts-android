package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NamedSettingsSettings(
    @field:Json(name = "skipForward") val skipForward: Int? = null,
    @field:Json(name = "skipBack") val skipBack: Int? = null,
    @field:Json(name = "marketingOptIn") val marketingOptIn: Boolean? = null,
    @field:Json(name = "freeGiftAcknowledgement") val freeGiftAcknowledged: Boolean? = null,
    @field:Json(name = "gridOrder") val gridOrder: Int? = null,
)

@JsonClass(generateAdapter = true)
data class NamedSettingsRequest(
    @field:Json(name = "m") val m: String = "Android",
    @field:Json(name = "v") val v: Int = 1,
    @field:Json(name = "settings") val settings: NamedSettingsSettings
)

typealias NamedSettingsResponse = Map<String, SettingResponse>

@JsonClass(generateAdapter = true)
data class SettingResponse(
    @field:Json(name = "value") val value: Any,
    @field:Json(name = "changed") val changed: Boolean
)
