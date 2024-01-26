package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Deprecated("This class can be removed when the sync settings feature flag is removed")
data class NamedSettingsSettings(
    @field:Json(name = "skipForward") val skipForward: Int? = null,
    @field:Json(name = "skipBack") val skipBack: Int? = null,
    @field:Json(name = "marketingOptIn") val marketingOptIn: Boolean? = null,
    @field:Json(name = "freeGiftAcknowledgement") val freeGiftAcknowledged: Boolean? = null,
    @field:Json(name = "gridOrder") val gridOrder: Int? = null,
)

@JsonClass(generateAdapter = true)
data class ChangedNamedSettings(
    @field:Json(name = "skipForward") val skipForward: NamedChangedSettingInt? = null,
    @field:Json(name = "skipBack") val skipBack: NamedChangedSettingInt? = null,
    @field:Json(name = "gridLayout") val gridLayout: NamedChangedSettingInt? = null,
    @field:Json(name = "gridOrder") val gridOrder: NamedChangedSettingInt? = null,
    @field:Json(name = "marketingOptIn") val marketingOptIn: NamedChangedSettingBool? = null,
    @field:Json(name = "freeGiftAcknowledgement") val freeGiftAcknowledgement: NamedChangedSettingBool? = null,
    @field:Json(name = "autoArchivePlayed") val autoArchiveAfterPlaying: NamedChangedSettingInt? = null,
    @field:Json(name = "autoArchiveInactive") val autoArchiveInactive: NamedChangedSettingInt? = null,
    @field:Json(name = "autoArchiveIncludesStarred") val autoArchiveIncludesStarred: NamedChangedSettingBool? = null,
    @field:Json(name = "rowAction") val rowAction: NamedChangedSettingInt? = null,
)

@JsonClass(generateAdapter = true)
data class NamedChangedSettingInt(
    @field:Json(name = "value") val value: Int,
    @field:Json(name = "modified_at") val modifiedAt: String,
)

@JsonClass(generateAdapter = true)
data class NamedChangedSettingBool(
    @field:Json(name = "value") val value: Boolean,
    @field:Json(name = "modified_at") val modifiedAt: String,
)

@Suppress("DEPRECATION")
@Deprecated("This class can be removed when the sync settings feature flag is removed")
@JsonClass(generateAdapter = true)
data class NamedSettingsRequest(
    @field:Json(name = "m") val m: String = "Android",
    @field:Json(name = "v") val v: Int = 1,
    @field:Json(name = "settings") val settings: NamedSettingsSettings,
)

@JsonClass(generateAdapter = true)
data class ChangedNamedSettingsRequest(
    @field:Json(name = "m") val m: String = "Android",
    @field:Json(name = "v") val v: Int = 1,
    @field:Json(name = "changed_settings") val changedSettings: ChangedNamedSettings?,
) {
    init {
        require(FeatureFlag.isEnabled(Feature.SETTINGS_SYNC)) {
            "This class should not be used unless settings sync feature is enabled"
        }
    }
}

typealias NamedSettingsResponse = Map<String, SettingResponse>
typealias ChangedNamedSettingsResponse = Map<String, ChangedSettingResponse>

@JsonClass(generateAdapter = true)
data class SettingResponse(
    @field:Json(name = "value") val value: Any,
    @field:Json(name = "changed") val changed: Boolean,
)

@JsonClass(generateAdapter = true)
data class ChangedSettingResponse(
    @field:Json(name = "value") val value: Any,
    @field:Json(name = "changed") val changed: Boolean,
    @field:Json(name = "modifiedAt") val modifiedAt: String? = null,
)

interface NamedSettingsCaller {
    @Deprecated("This method can be removed when the sync settings feature flag is removed")
    suspend fun namedSettings(
        @Suppress("DEPRECATION") request: NamedSettingsRequest,
    ): NamedSettingsResponse
    suspend fun changedNamedSettings(request: ChangedNamedSettingsRequest): ChangedNamedSettingsResponse
}
