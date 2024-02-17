package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

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
    @field:Json(name = "playbackSpeed") val playbackSpeed: NamedChangedSettingDouble? = null,
    @field:Json(name = "trimSilence") val trimSilence: NamedChangedSettingInt? = null,
    @field:Json(name = "volumeBoost") val volumeBoost: NamedChangedSettingBool? = null,
    @field:Json(name = "rowAction") val rowAction: NamedChangedSettingInt? = null,
    @field:Json(name = "upNextSwipe") val upNextSwipe: NamedChangedSettingInt? = null,
    @field:Json(name = "mediaActions") val showCustomMediaActions: NamedChangedSettingBool? = null,
    @field:Json(name = "mediaActionsOrder") val mediaActionsOrder: NamedChangedSettingString? = null,
    @field:Json(name = "episodeGrouping") val episodeGrouping: NamedChangedSettingInt? = null,
    @field:Json(name = "keepScreenAwake") val keepScreenAwake: NamedChangedSettingBool? = null,
    @field:Json(name = "openPlayer") val openPlayerAutomatically: NamedChangedSettingBool? = null,
    @field:Json(name = "showArchived") val showArchived: NamedChangedSettingBool? = null,
    @field:Json(name = "intelligentResumption") val intelligentResumption: NamedChangedSettingBool? = null,
    @field:Json(name = "autoPlayEnabled") val autoPlayEnabled: NamedChangedSettingBool? = null,
    @field:Json(name = "hideNotificationOnPause") val hideNotificationOnPause: NamedChangedSettingBool? = null,
    @field:Json(name = "playUpNextOnTap") val playUpNextOnTap: NamedChangedSettingBool? = null,
    @field:Json(name = "playOverNotifications") val playOverNotifications: NamedChangedSettingInt? = null,
    @field:Json(name = "autoUpNextLimit") val autoUpNextLimit: NamedChangedSettingInt? = null,
    @field:Json(name = "autoUpNextLimitReached") val autoUpNextLimitReached: NamedChangedSettingInt? = null,
    @field:Json(name = "warnDataUsage") val warnDataUsage: NamedChangedSettingBool? = null,
    @field:Json(name = "notifications") val showPodcastNotifications: NamedChangedSettingBool? = null,
    @field:Json(name = "privacyAnalytics") val collectAnalytics: NamedChangedSettingBool? = null,
    @field:Json(name = "privacyCrashReports") val sendCrashReports: NamedChangedSettingBool? = null,
    @field:Json(name = "privacyLinkAccount") val linkCrashReportsToUser: NamedChangedSettingBool? = null,
    @field:Json(name = "filesAutoUpNext") val addFileToUpNextAutomatically: NamedChangedSettingBool? = null,
    @field:Json(name = "theme") val theme: NamedChangedSettingInt? = null,
    @field:Json(name = "badges") val podcastBadges: NamedChangedSettingInt? = null,
    @field:Json(name = "autoSubscribeToPlayed") val autoSubscribeToPlayed: NamedChangedSettingBool? = null,
    @field:Json(name = "autoShowPlayed") val autoShowPlayed: NamedChangedSettingBool? = null,
    @field:Json(name = "autoPlayLastListUuid") val autoPlayLastSource: NamedChangedSettingString? = null,
    @field:Json(name = "useEmbeddedArtwork") val useEmbeddedArtwork: NamedChangedSettingBool? = null,
    @field:Json(name = "useEpisodeArtowork") val useEpisodeArtwork: NamedChangedSettingBool? = null,
    @field:Json(name = "notificationActions") val notificationSettingActions: NamedChangedSettingString? = null,
    @field:Json(name = "playerShelf") val playerShelfItems: NamedChangedSettingString? = null,
    @field:Json(name = "showArtworkOnLockScreen") val showArtworkOnLockScreen: NamedChangedSettingBool? = null,
    @field:Json(name = "headphoneControlsNextAction") val headphoneControlsNextAction: NamedChangedSettingInt? = null,
    @field:Json(name = "headphoneControlsPreviousAction") val headphoneControlsPreviousAction: NamedChangedSettingInt? = null,
    @field:Json(name = "headphoneControlsPlayBookmarkConfirmationSound") val headphoneControlsPlayBookmarkConfirmationSound: NamedChangedSettingBool? = null,
    @field:Json(name = "episodeBookmarksSortType") val episodeBookmarksSortType: NamedChangedSettingInt? = null,
    @field:Json(name = "playerBookmarksSortType") val playerBookmarksSortType: NamedChangedSettingInt? = null,
    @field:Json(name = "podcastBookmarksSortType") val podcastBookmarksSortType: NamedChangedSettingInt? = null,
    @field:Json(name = "useDarkUpNextTheme") val useDarkUpNextTheme: NamedChangedSettingBool? = null,
    @field:Json(name = "useDynamicColorsForWidget") val useDynamicColorsForWidget: NamedChangedSettingBool? = null,
    @field:Json(name = "filesSortOrder") val filesSortOrder: NamedChangedSettingInt? = null,
    @field:Json(name = "darkThemePreference") val darkThemePreference: NamedChangedSettingInt? = null,
    @field:Json(name = "lightThemePreference") val lightThemePreference: NamedChangedSettingInt? = null,
    @field:Json(name = "useSystemTheme") val useSystemTheme: NamedChangedSettingBool? = null,
)

@JsonClass(generateAdapter = true)
data class NamedChangedSettingInt(
    @field:Json(name = "value") val value: Int,
    @field:Json(name = "modified_at") val modifiedAt: Instant,
)

@JsonClass(generateAdapter = true)
data class NamedChangedSettingBool(
    @field:Json(name = "value") val value: Boolean,
    @field:Json(name = "modified_at") val modifiedAt: Instant,
)

@JsonClass(generateAdapter = true)
data class NamedChangedSettingDouble(
    @field:Json(name = "value") val value: Double,
    @field:Json(name = "modified_at") val modifiedAt: Instant,
)

@JsonClass(generateAdapter = true)
data class NamedChangedSettingString(
    @field:Json(name = "value") val value: String,
    @field:Json(name = "modified_at") val modifiedAt: Instant,
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
