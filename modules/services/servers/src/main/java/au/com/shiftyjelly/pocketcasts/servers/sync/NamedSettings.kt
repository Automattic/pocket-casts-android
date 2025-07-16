package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NamedSettingsSettings(
    @Json(name = "skipForward") val skipForward: Int? = null,
    @Json(name = "skipBack") val skipBack: Int? = null,
    @Json(name = "marketingOptIn") val marketingOptIn: Boolean? = null,
    @Json(name = "freeGiftAcknowledgement") val freeGiftAcknowledged: Boolean? = null,
    @Json(name = "gridOrder") val gridOrder: Int? = null,
)

@JsonClass(generateAdapter = true)
data class NamedSettingsRequest(
    @Json(name = "m") val m: String = "Android",
    @Json(name = "v") val v: Int = 1,
    @Json(name = "settings") val settings: NamedSettingsSettings,
)

typealias NamedSettingsResponse = Map<String, SettingResponse>

@JsonClass(generateAdapter = true)
data class SettingResponse(
    @Json(name = "value") val value: Any,
    @Json(name = "changed") val changed: Boolean,
)

@JsonClass(generateAdapter = true)
data class ChangedSettingResponse(
    @Json(name = "value") val value: Any,
    @Json(name = "changed") val changed: Boolean,
    @Json(name = "modifiedAt") val modifiedAt: String? = null,
)

interface NamedSettingsCaller {
    suspend fun namedSettings(request: NamedSettingsRequest): NamedSettingsResponse
}
