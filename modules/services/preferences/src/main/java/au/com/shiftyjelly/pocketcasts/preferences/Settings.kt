package au.com.shiftyjelly.pocketcasts.preferences

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.work.NetworkType
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.model.AppIconSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoAddUpNextLimitBehaviour
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeDefault
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForPodcast
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForProfile
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ThemeSetting
import io.reactivex.Observable
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

interface Settings {

    companion object {
        const val SERVER_MAIN_URL = BuildConfig.SERVER_MAIN_URL
        const val SERVER_API_URL = BuildConfig.SERVER_API_URL
        const val SERVER_CACHE_URL = BuildConfig.SERVER_CACHE_URL
        const val SERVER_STATIC_URL = BuildConfig.SERVER_STATIC_URL
        const val SERVER_SHARING_URL = BuildConfig.SERVER_SHARING_URL
        const val SERVER_SHORT_URL = BuildConfig.SERVER_SHORT_URL
        const val SERVER_LIST_URL = BuildConfig.SERVER_LIST_URL
        const val SERVER_LIST_HOST = BuildConfig.SERVER_LIST_HOST
        const val WP_COM_API_URL = "https://public-api.wordpress.com"

        const val SHARING_SERVER_SECRET = BuildConfig.SHARING_SERVER_SECRET
        val SETTINGS_ENCRYPT_SECRET = BuildConfig.SETTINGS_ENCRYPT_SECRET.toCharArray()

        const val GOOGLE_SIGN_IN_SERVER_CLIENT_ID = BuildConfig.GOOGLE_SIGN_IN_SERVER_CLIENT_ID

        const val INFO_LEARN_MORE_URL = "https://www.pocketcasts.com/plus/"
        const val INFO_TOS_URL = "https://support.pocketcasts.com/article/terms-of-use-overview/"
        const val INFO_PRIVACY_URL = "https://support.pocketcasts.com/article/privacy-policy/"
        const val INFO_CANCEL_URL = "https://support.pocketcasts.com/knowledge-base/how-to-cancel-a-subscription/"
        const val INFO_FAQ_URL = "https://support.pocketcasts.com/android/?device=android"

        const val CHROME_CAST_APP_ID = "2FA4D21B"

        const val WHATS_NEW_VERSION_CODE = 9258

        const val DEFAULT_MAX_AUTO_ADD_LIMIT = 100
        const val MAX_DOWNLOAD = 100

        const val PARSER_VERSION = "1.7"
        const val SERVER_DEVICE_TYPE = "2"
        const val SYNC_API_VERSION = 2
        const val SYNC_HISTORY_VERSION = 1
        const val SYNC_API_MODEL = "mobile"
        const val LAST_UPDATE_TIME = "LastUpdateTime"
        const val LAST_DISMISS_LOW_STORAGE_MODAL_TIME = "LastDismissLowStorageModalTime"
        const val LAST_DISMISS_LOW_STORAGE_BANNER_TIME = "LastDismissLowStorageBannerTime"
        const val PREFERENCE_SKIP_FORWARD = "skipForward"
        const val PREFERENCE_SKIP_BACKWARD = "skipBack"
        const val PREFERENCE_STORAGE_CHOICE = "storageChoice"
        const val PREFERENCE_STORAGE_CHOICE_NAME = "storageChoiceName"
        const val PREFERENCE_STORAGE_CUSTOM_FOLDER = "storageCustomFolder"
        const val PREFERENCE_SELECT_PODCAST_LIBRARY_SORT = "selectPodcastLibrarySort"
        const val PREFERENCE_WARN_WHEN_NOT_ON_WIFI = "warnWhenNotOnWifi"
        const val PREFERENCE_SYNC_ON_METERED = "SyncWhenOnMetered"
        const val PREFERENCE_LAST_MODIFIED = "lastModified"
        const val PREFERENCE_FIRST_SYNC_RUN = "firstSyncRun"
        const val PREFERENCE_GLOBAL_STREAMING_MODE = "globalStreamingMode"
        const val PREFERENCE_SELECTED_FILTER = "selectedFilter"
        const val PREFERENCE_CHAPTERS_EXPANDED = "chaptersExpanded"
        const val PREFERENCE_UPNEXT_EXPANDED = "upnextExpanded"
        const val INTELLIGENT_PLAYBACK_RESUMPTION = "intelligentPlaybackResumption"

        const val STORAGE_ON_CUSTOM_FOLDER = "custom_folder"

        const val GLOBAL_AUTO_DOWNLOAD_NONE = -1

        const val PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_EPISODE = "bookmarksSortTypeForEpisode"
        const val PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_PLAYER = "bookmarksSortTypeForPlayer"
        const val PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_PODCAST = "bookmarksSortTypeForPodcast"
        const val PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_PROFILE = "bookmarksSortTypeForProfile"

        val SUPPORTED_LANGUAGE_CODES = arrayOf("us", "se", "jp", "gb", "fr", "es", "de", "ca", "au", "it", "ru", "br", "no", "be", "cn", "dk", "sw", "ch", "ie", "pl", "kr", "nl")

        // legacy settings
        const val LEGACY_STORAGE_ON_PHONE = "phone"
        const val LEGACY_STORAGE_ON_SD_CARD = "external"

        const val AUTO_ARCHIVE_INCLUDE_STARRED = "autoArchiveIncludeStarred"

        const val INTENT_OPEN_APP_NEW_EPISODES = "INTENT_OPEN_APP_NEW_EPISODES"

        const val LOG_TAG_AUTO = "PocketCastsAuto"

        const val NOTIFICATIONS_DISABLED_MESSAGE_SHOWN = "notificationsDisabledMessageShown"

        const val APP_REVIEW_REQUESTED_DATES = "in_app_review_requested_dates"

        const val AUTOMOTIVE_CONNECTED_TO_MEDIA_SESSION = "automotive_connected_to_media_session"

        const val SHOW_REFERRALS_TOOLTIP = "show_referrals_tooltip"
    }

    enum class NotificationChannel(val id: String) {
        NOTIFICATION_CHANNEL_ID_PLAYBACK("playback"),
        NOTIFICATION_CHANNEL_ID_DOWNLOAD("download"),
        NOTIFICATION_CHANNEL_ID_EPISODE("episode"),
        NOTIFICATION_CHANNEL_ID_PLAYBACK_ERROR("playbackError"),
        NOTIFICATION_CHANNEL_ID_PODCAST("podcastImport"),
        NOTIFICATION_CHANNEL_ID_SIGN_IN_ERROR("signInError"),
        NOTIFICATION_CHANNEL_ID_BOOKMARK("bookmark"),
        NOTIFICATION_CHANNEL_ID_FIX_DOWNLOADS("fixDownloads"),
        NOTIFICATION_CHANNEL_ID_FIX_DOWNLOADS_COMPLETE("fixDownloadsComplete"),
        NOTIFICATION_CHANNEL_ID_DAILY_REMINDERS("dailyReminders"),
        NOTIFICATION_CHANNEL_ID_TRENDING_AND_RECOMMENDATIONS("trendingAndRecommendations"),
        NOTIFICATION_CHANNEL_ID_NEW_FEATURES_AND_TIPS("newFeaturesAndTips"),
        NOTIFICATION_CHANNEL_ID_OFFERS("offers"),
    }

    enum class NotificationId(val value: Int) {
        OPML(21483646),
        PLAYING(21483647),
        DOWNLOADING(21483648),
        SIGN_IN_ERROR(21483649),
        BOOKMARK(21483650),
        FIX_DOWNLOADS(21483651),
        FIX_DOWNLOADS_COMPLETE(21483652),
        ONBOARDING_SYNC(21483653),
        ONBOARDING_IMPORT(21483654),
        ONBOARDING_UPNEXT(21483655),
        ONBOARDING_FILTERS(21483656),
        ONBOARDING_THEMES(21483657),
        ONBOARDING_STAFF_PICKS(21483658),
        ONBOARDING_UPSELL(21483659),
        RE_ENGAGEMENT(21483660),
        CONTENT_RECOMMENDATIONS(21483661),
        FEATURES_AND_TIPS(21483662),
        OFFERS(21483663),
    }

    enum class UpNextAction(val serverId: Int) {
        PLAY_NEXT(serverId = 0),
        PLAY_LAST(serverId = 1),
        ;

        companion object {
            fun fromServerId(id: Int) = entries.find { it.serverId == id } ?: PLAY_NEXT
        }
    }

    enum class CloudSortOrder(
        val analyticsValue: String,
        val serverId: Int,
    ) {
        NEWEST_OLDEST(
            analyticsValue = "newest_to_oldest",
            serverId = 0,
        ),
        OLDEST_NEWEST(
            analyticsValue = "oldest_to_newest",
            serverId = 1,
        ),
        A_TO_Z(
            analyticsValue = "title_a_to_z",
            serverId = 2,
        ),
        Z_TO_A(
            analyticsValue = "title_z_to_a",
            serverId = 3,
        ),
        SHORT_LONG(
            analyticsValue = "shortest_to_longest",
            serverId = 4,
        ),
        LONG_SHORT(
            analyticsValue = "longest_to_shortest",
            serverId = 5,
        ),
        ;

        companion object {
            fun fromServerId(id: Int) = entries.find { it.serverId == id }
        }
    }

    sealed class MediaNotificationControls(
        @StringRes val controlName: Int,
        @DrawableRes val iconRes: Int,
        val key: String,
        val serverId: String,
    ) {
        companion object {
            val All
                get() = listOf(PlaybackSpeed, Star, MarkAsPlayed, PlayNext, Archive)
            val items = All.associateBy { it.key }

            const val MAX_VISIBLE_OPTIONS = 3

            private const val ARCHIVE_KEY = "default_media_control_archive"
            private const val MARK_AS_PLAYED_KEY = "default_media_control_mark_as_played"
            private const val PLAY_NEXT_KEY = "default_media_control_play_next_key"
            private const val PLAYBACK_SPEED_KEY = "default_media_control_playback_speed_key"
            private const val STAR_KEY = "default_media_control_star_key"

            fun itemForId(id: String): MediaNotificationControls? {
                return items[id]
            }

            fun fromServerId(id: String) = All.find { it.serverId == id }
        }

        init {
            // We use comma as a delimiter when syncing list of these settings
            require(!serverId.contains(',')) {
                "Media notification control server ID cannot contain a comma"
            }
        }

        data object Archive : MediaNotificationControls(
            controlName = LR.string.archive,
            iconRes = IR.drawable.ic_archive,
            key = ARCHIVE_KEY,
            serverId = "archive",
        )

        data object MarkAsPlayed : MediaNotificationControls(
            controlName = LR.string.mark_as_played,
            iconRes = IR.drawable.ic_markasplayed,
            key = MARK_AS_PLAYED_KEY,
            serverId = "mark_as_played",
        )

        data object PlayNext : MediaNotificationControls(
            controlName = LR.string.play_next,
            iconRes = IR.drawable.ic_skip_next,
            key = PLAY_NEXT_KEY,
            serverId = "play_next",
        )

        data object PlaybackSpeed : MediaNotificationControls(
            controlName = LR.string.playback_speed,
            iconRes = IR.drawable.ic_speed_number,
            key = PLAYBACK_SPEED_KEY,
            serverId = "playback_speed",
        )

        data object Star : MediaNotificationControls(
            controlName = LR.string.star,
            iconRes = IR.drawable.ic_star,
            key = STAR_KEY,
            serverId = "star",
        )
    }

    val selectPodcastSortTypeObservable: Observable<PodcastsSortType>
    val multiSelectItemsObservable: Observable<List<String>>
    val refreshStateFlow: StateFlow<RefreshState>

    val shelfItems: UserSetting<List<ShelfItem>>

    val upNextShuffle: UserSetting<Boolean>

    fun getVersion(): String
    fun getVersionCode(): Int

    val skipForwardInSecs: UserSetting<Int>
    val skipBackInSecs: UserSetting<Int>

    fun syncOnMeteredNetwork(): Boolean
    fun setSyncOnMeteredNetwork(shouldSyncOnMetered: Boolean)
    fun getWorkManagerNetworkTypeConstraint(): NetworkType
    fun refreshPodcastsOnResume(isUnmetered: Boolean): Boolean
    val backgroundRefreshPodcasts: UserSetting<Boolean>
    val podcastsSortType: UserSetting<PodcastsSortType>
    val prioritizeSeekAccuracy: UserSetting<Boolean>
    val cacheEntirePlayingEpisode: UserSetting<Boolean>

    fun setSelectPodcastsSortType(sortType: PodcastsSortType)
    fun getSelectPodcastsSortType(): PodcastsSortType

    val notificationVibrate: UserSetting<NotificationVibrateSetting>
    val notificationSound: UserSetting<NotificationSound>
    val notifyRefreshPodcast: UserSetting<Boolean>

    fun usingCustomFolderStorage(): Boolean

    fun getStorageChoice(): String?
    fun getStorageChoiceName(): String?
    fun setStorageChoice(choice: String?, name: String?)
    fun getStorageCustomFolder(): String
    fun setStorageCustomFolder(folder: String)

    fun getLastRefreshTime(): Long
    fun getLastRefreshDate(): Date?
    fun setRefreshState(refreshState: RefreshState)
    fun getRefreshState(): RefreshState?
    fun getLastSuccessRefreshState(): RefreshState?

    fun getLongForKey(key: String, defaultValue: Long): Long
    fun setLongForKey(key: String, value: Long)
    fun getBooleanForKey(key: String, defaultValue: Boolean): Boolean
    fun setBooleanForKey(key: String, value: Boolean)
    fun getStringForKey(key: String, isPrivate: Boolean = false): String?
    fun deleteKey(key: String, isPrivate: Boolean = false)

    val discoverCountryCode: UserSetting<String>

    val warnOnMeteredNetwork: UserSetting<Boolean>

    val playOverNotification: UserSetting<PlayOverNotificationSetting>

    val autoDownloadLimit: UserSetting<AutoDownloadLimitSetting>

    fun setLastModified(lastModified: String?)
    fun getLastModified(): String?

    fun isFirstSyncRun(): Boolean
    fun setFirstSyncRun(firstRun: Boolean)

    fun isRestoreFromBackup(): Boolean
    fun setRestoreFromBackupEnded()

    fun clearPlusPreferences()

    fun setDismissLowStorageModalTime(lastUpdateTime: Long)
    fun shouldShowLowStorageModalAfterSnooze(): Boolean

    fun setDismissLowStorageBannerTime(lastUpdateTime: Long)
    fun shouldShowLowStorageBannerAfterSnooze(): Boolean

    val hideNotificationOnPause: UserSetting<Boolean>
    val dailyRemindersNotification: UserSetting<Boolean>
    val recommendationsNotification: UserSetting<Boolean>
    val newFeaturesNotification: UserSetting<Boolean>
    val offersNotification: UserSetting<Boolean>
    val notificationsPromptAcknowledged: UserSetting<Boolean>

    val streamingMode: UserSetting<Boolean>
    val keepScreenAwake: UserSetting<Boolean>
    val openPlayerAutomatically: UserSetting<Boolean>

    val autoDownloadUnmeteredOnly: UserSetting<Boolean>
    val autoDownloadOnlyWhenCharging: UserSetting<Boolean>
    val autoDownloadUpNext: UserSetting<Boolean>
    val autoDownloadOnFollowPodcast: UserSetting<Boolean>
    val autoDownloadNewEpisodes: UserSetting<Int>

    val artworkConfiguration: UserSetting<ArtworkConfiguration>

    val globalPlaybackEffects: UserSetting<PlaybackEffects>

    fun getMigratedVersionCode(): Int
    fun setMigratedVersionCode(versionCode: Int)

    val podcastBadgeType: UserSetting<BadgeType>
    val podcastGridLayout: UserSetting<PodcastGridLayoutType>

    fun getNotificationLastSeen(): Date?
    fun setNotificationLastSeen(lastSeen: Date?)
    fun setNotificationLastSeenToNow()

    fun setUpNextServerModified(timeMs: Long)
    fun getUpNextServerModified(): Long
    fun setHistoryServerModified(timeMs: Long)
    fun getHistoryServerModified(): Long
    fun setClearHistoryTime(timeMs: Long)
    fun setClearHistoryTimeNow()
    fun getClearHistoryTime(): Long

    fun setSleepTimerCustomMins(minutes: Int)
    fun setSleepEndOfEpisodes(episodes: Int)
    fun setSleepEndOfChapters(chapters: Int)
    fun setlastSleepEndOfEpisodes(episodes: Int)
    fun setlastSleepEndOfChapters(chapters: Int)
    fun getSleepTimerCustomMins(): Int
    fun getSleepEndOfEpisodes(): Int
    fun getSleepEndOfChapters(): Int
    fun getlastSleepEndOfEpisodes(): Int
    fun getlastSleepEndOfChapter(): Int

    fun setShowPlayedEpisodes(show: Boolean)
    fun showPlayedEpisodes(): Boolean

    val showArtworkOnLockScreen: UserSetting<Boolean>
    val newEpisodeNotificationActions: UserSetting<List<NewEpisodeNotificationAction>>

    val autoArchiveIncludesStarred: UserSetting<Boolean>
    val autoArchiveAfterPlaying: UserSetting<AutoArchiveAfterPlaying>
    val autoArchiveInactive: UserSetting<AutoArchiveInactive>

    fun selectedFilter(): String?
    fun setSelectedFilter(filterUUID: String?)
    fun selectedTab(): Int?
    fun setSelectedTab(selected: Int?)

    fun contains(key: String, isPrivate: Boolean = false): Boolean

    val upNextSwipe: UserSetting<UpNextAction>
    val tapOnUpNextShouldPlay: UserSetting<Boolean>

    val headphoneControlsNextAction: UserSetting<HeadphoneAction>
    val headphoneControlsPreviousAction: UserSetting<HeadphoneAction>
    val headphoneControlsPlayBookmarkConfirmationSound: UserSetting<Boolean>

    // Firebase remote config
    fun getPeriodicSaveTimeMs(): Long
    fun getPlayerReleaseTimeOutMs(): Long
    fun getPodcastSearchDebounceMs(): Long
    fun getEpisodeSearchDebounceMs(): Long
    fun getSlumberStudiosPromoCode(): String
    fun getSleepTimerDeviceShakeThreshold(): Long
    fun getRefreshPodcastsBatchSize(): Long
    fun getExoPlayerCacheSizeInMB(): Long
    fun getExoPlayerCacheEntirePlayingEpisodeSizeInMB(): Long
    fun getPlaybackEpisodePositionChangedOnSyncThresholdSecs(): Long

    val podcastGroupingDefault: UserSetting<PodcastGrouping>

    val marketingOptIn: UserSetting<Boolean>

    val freeGiftAcknowledged: UserSetting<Boolean>

    val cloudSortOrder: UserSetting<CloudSortOrder>
    val cloudAddToUpNext: UserSetting<Boolean>
    val deleteLocalFileAfterPlaying: UserSetting<Boolean>
    val deleteCloudFileAfterPlaying: UserSetting<Boolean>
    val cloudAutoUpload: UserSetting<Boolean>
    val cloudAutoDownload: UserSetting<Boolean>
    val cloudDownloadOnlyOnWifi: UserSetting<Boolean>
    val cachedSubscription: UserSetting<Subscription?>

    val upgradeProfileClosed: UserSetting<Boolean>
    fun getUpgradeClosedAddFile(): Boolean
    fun setUpgradeClosedAddFile(value: Boolean)
    fun getUpgradeClosedCloudSettings(): Boolean
    fun setUpgradeClosedCloudSettings(value: Boolean)
    fun getUpgradeClosedAppearSettings(): Boolean
    fun setUpgradeClosedAppearSettings(value: Boolean)
    fun getWhatsNewVersionCode(): Int?
    fun setWhatsNewVersionCode(value: Int)
    fun getCustomStorageLimitGb(): Long
    fun getCancelledAcknowledged(): Boolean
    fun setCancelledAcknowledged(value: Boolean)
    fun setTrialFinishedSeen(seen: Boolean)
    fun getTrialFinishedSeen(): Boolean
    val autoSubscribeToPlayed: UserSetting<Boolean>
    val autoShowPlayed: UserSetting<Boolean>
    val autoPlayNextEpisodeOnEmpty: UserSetting<Boolean>
    val showArchivedDefault: UserSetting<Boolean>
    val mediaControlItems: UserSetting<List<MediaNotificationControls>>
    val shakeToResetSleepTimer: UserSetting<Boolean>
    val autoSleepTimerRestart: UserSetting<Boolean>
    fun getMultiSelectItems(): List<String>
    fun setMultiSelectItems(items: List<String>)
    fun setLastPauseTime(date: Date)
    fun getLastPauseTime(): Date?
    fun setLastPausedUUID(uuid: String)
    fun getLastPausedUUID(): String?
    fun setLastPausedAt(pausedAt: Int)
    fun getLastPausedAt(): Int?
    val intelligentPlaybackResumption: UserSetting<Boolean>
    val autoAddUpNextLimit: UserSetting<Int>
    val autoAddUpNextLimitBehaviour: UserSetting<AutoAddUpNextLimitBehaviour>
    fun getMaxUpNextEpisodes(): Int
    fun getUniqueDeviceId(): String
    fun setHomeGridNeedsRefresh(value: Boolean)
    fun getHomeGridNeedsRefresh(): Boolean

    fun setTimesToShowBatteryWarning(value: Int)
    fun getTimesToShowBatteryWarning(): Int

    // Only the AnalyticsTracker object should update SendUsageState directly. Everything else
    // should update this setting through the AnalyticsTracker.
    val collectAnalytics: UserSetting<Boolean>
    val collectAnalyticsThirdParty: UserSetting<Boolean>
    val sendCrashReports: UserSetting<Boolean>
    val linkCrashReportsToUser: UserSetting<Boolean>

    fun setEndOfYearShowBadge2023(value: Boolean)
    fun getEndOfYearShowBadge2023(): Boolean

    fun setEndOfYearShowModal(value: Boolean)
    fun getEndOfYearShowModal(): Boolean

    fun hasCompletedOnboarding(): Boolean
    fun setHasDoneInitialOnboarding()

    val customMediaActionsVisibility: UserSetting<Boolean>
    val nextPreviousTrackSkipButtons: UserSetting<Boolean>

    fun isNotificationsDisabledMessageShown(): Boolean
    fun setNotificationsDisabledMessageShown(value: Boolean)

    // This boolean should be update to false when a user signs in and should be set to
    // true once a user signs out and that sign out has been fully handled
    // by the app. This field helps make sure the app fully handles signing a user
    // out even if they sign out from outside the app (i.e., from the Android OS's
    // account management settings).
    fun setFullySignedOut(boolean: Boolean)
    fun getFullySignedOut(): Boolean

    val lastAutoPlaySource: UserSetting<AutoPlaySource>

    // This property is used for determining which source should be used for auto play
    // We don't want to always change the auto play source but we want to always track it
    // in case we need to change it.
    val trackingAutoPlaySource: UserSetting<AutoPlaySource>

    // It would be better to have this be a UserSetting<ThemeType>, but that
    // is not easy due to the way our modules are structured.
    val theme: UserSetting<ThemeSetting>
    val darkThemePreference: UserSetting<ThemeSetting>
    val lightThemePreference: UserSetting<ThemeSetting>
    val useSystemTheme: UserSetting<Boolean>

    // It would be better to have this be a UserSetting<AppIconType>, but that
    // is not easy due to the way our modules are structured.
    val appIcon: UserSetting<AppIconSetting>

    val episodeBookmarksSortType: UserSetting<BookmarksSortTypeDefault>
    val playerBookmarksSortType: UserSetting<BookmarksSortTypeDefault>
    val podcastBookmarksSortType: UserSetting<BookmarksSortTypeForPodcast>
    val profileBookmarksSortType: UserSetting<BookmarksSortTypeForProfile>

    fun addReviewRequestedDate()
    fun getReviewRequestedDates(): List<String>

    val useDarkUpNextTheme: UserSetting<Boolean>

    val useDynamicColorsForWidget: UserSetting<Boolean>

    // We need to have a trigger for requesting theme changes.
    // It is needed because during the sync we apply updates
    // to individual theme settings one by one.
    //
    // If we were to react to them this way and not as a whole
    // it might lead to side effects such us resetting following
    // system dark mode.
    val themeReconfigurationEvents: Flow<Unit>
    fun requestThemeReconfiguration()

    val bottomInset: Flow<Int>
    fun updateBottomInset(height: Int)

    fun automotiveConnectedToMediaSession(): Boolean
    fun setAutomotiveConnectedToMediaSession(isLoaded: Boolean)

    val showReferralsTooltip: UserSetting<Boolean>

    val playerOrUpNextBottomSheetState: Flow<Int>
    fun updatePlayerOrUpNextBottomSheetState(state: Int)

    val referralClaimCode: UserSetting<String>
    val showReferralWelcome: UserSetting<Boolean>

    val lastEoySyncTimestamp: UserSetting<Instant>

    val useRealTimeForPlaybackRemaingTime: UserSetting<Boolean>

    val showPodcastsRecentlyPlayedSortOrderTooltip: UserSetting<Boolean>

    val showEmptyFiltersListTooltip: UserSetting<Boolean>

    val suggestedFoldersDismissTimestamp: UserSetting<Instant?>
    val suggestedFoldersDismissCount: UserSetting<Int>
    val suggestedFoldersFollowedHash: UserSetting<String>

    val isTrackingConsentRequired: UserSetting<Boolean>

    val isFreeAccountProfileBannerDismissed: UserSetting<Boolean>
    val isFreeAccountFiltersBannerDismissed: UserSetting<Boolean>
    val isFreeAccountHistoryBannerDismissed: UserSetting<Boolean>
    val showFreeAccountEncouragement: UserSetting<Boolean>

    val showPlaylistsOnboarding: UserSetting<Boolean>
}
