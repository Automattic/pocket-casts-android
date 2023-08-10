package au.com.shiftyjelly.pocketcasts.preferences

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.work.NetworkType
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortTypeForPlayer
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortTypeForPodcast
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.preferences.model.AppIconSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoAddUpNextLimitBehaviour
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveAfterPlayingSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveInactiveSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.LastPlayedList
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationActionSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.preferences.model.ThemeSetting
import io.reactivex.Observable
import kotlinx.coroutines.flow.StateFlow
import java.util.Date
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
        const val INFO_CANCEL_URL = "https://support.pocketcasts.com/article/subscription-info/"

        const val USER_AGENT_POCKETCASTS_SERVER = "Pocket Casts/Android/" + BuildConfig.VERSION_NAME

        const val CHROME_CAST_APP_ID = "2FA4D21B"

        const val WHATS_NEW_VERSION_CODE = 9115

        const val DEFAULT_MAX_AUTO_ADD_LIMIT = 100
        const val MAX_DOWNLOAD = 100

        const val PARSER_VERSION = "1.7"
        const val SERVER_DEVICE_TYPE = "2"
        const val SYNC_API_VERSION = 2
        const val SYNC_HISTORY_VERSION = 1
        const val SYNC_API_MODEL = "mobile"
        const val LAST_UPDATE_TIME = "LastUpdateTime"
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

        const val PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_PLAYER = "bookmarksSortTypeForPlayer"
        const val PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_PODCAST = "bookmarksSortTypeForPodcast"

        val SUPPORTED_LANGUAGE_CODES = arrayOf("us", "se", "jp", "gb", "fr", "es", "de", "ca", "au", "it", "ru", "br", "no", "be", "cn", "dk", "sw", "ch", "ie", "pl", "kr", "nl")

        // legacy settings
        const val LEGACY_STORAGE_ON_PHONE = "phone"
        const val LEGACY_STORAGE_ON_SD_CARD = "external"

        const val AUTO_ARCHIVE_INCLUDE_STARRED = "autoArchiveIncludeStarred"

        const val INTENT_OPEN_APP_NEW_EPISODES = "INTENT_OPEN_APP_NEW_EPISODES"
        const val INTENT_OPEN_APP_DOWNLOADING = "INTENT_OPEN_APP_DOWNLOADING"
        const val INTENT_OPEN_APP_EPISODE_UUID = "INTENT_OPEN_APP_EPISODE_UUID"
        const val INTENT_LINK_CLOUD_FILES = "pktc://cloudfiles"
        const val INTENT_LINK_UPGRADE = "pktc://upgrade"
        const val INTENT_LINK_PROMO_CODE = "pktc://redeem/promo/"

        const val LOG_TAG_AUTO = "PocketCastsAuto"

        const val NOTIFICATIONS_DISABLED_MESSAGE_SHOWN = "notificationsDisabledMessageShown"
    }

    enum class NotificationChannel(val id: String) {
        NOTIFICATION_CHANNEL_ID_PLAYBACK("playback"),
        NOTIFICATION_CHANNEL_ID_DOWNLOAD("download"),
        NOTIFICATION_CHANNEL_ID_EPISODE("episode"),
        NOTIFICATION_CHANNEL_ID_PLAYBACK_ERROR("playbackError"),
        NOTIFICATION_CHANNEL_ID_PODCAST("podcastImport"),
        NOTIFICATION_CHANNEL_ID_SIGN_IN_ERROR("signInError"),
    }

    enum class NotificationId(val value: Int) {
        OPML(21483646),
        PLAYING(21483647),
        DOWNLOADING(21483648),
        SIGN_IN_ERROR(21483649),
    }

    enum class UpNextAction {
        PLAY_NEXT,
        PLAY_LAST
    }

    enum class CloudSortOrder {
        NEWEST_OLDEST,
        OLDEST_NEWEST,
        A_TO_Z,
        Z_TO_A,
        SHORT_LONG,
        LONG_SHORT
    }

    enum class HeadphoneAction {
        ADD_BOOKMARK,
        SKIP_BACK,
        SKIP_FORWARD,
        NEXT_CHAPTER,
        PREVIOUS_CHAPTER,
    }

    sealed class MediaNotificationControls(@StringRes val controlName: Int, @DrawableRes val iconRes: Int, val key: String) {

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
        }

        object Archive : MediaNotificationControls(LR.string.archive, IR.drawable.ic_archive, ARCHIVE_KEY)

        object MarkAsPlayed : MediaNotificationControls(LR.string.mark_as_played, IR.drawable.ic_markasplayed, MARK_AS_PLAYED_KEY)

        object PlayNext : MediaNotificationControls(LR.string.play_next, com.google.android.gms.cast.framework.R.drawable.cast_ic_mini_controller_skip_next, PLAY_NEXT_KEY)

        object PlaybackSpeed : MediaNotificationControls(LR.string.playback_speed, IR.drawable.auto_1x, PLAYBACK_SPEED_KEY)

        object Star : MediaNotificationControls(LR.string.star, IR.drawable.ic_star, STAR_KEY)
    }

    val selectPodcastSortTypeObservable: Observable<PodcastsSortType>
    val refreshStateObservable: Observable<RefreshState>
    val shelfItemsObservable: Observable<List<String>>
    val multiSelectItemsObservable: Observable<List<Int>>

    val headphonePreviousActionFlow: StateFlow<HeadphoneAction>
    val headphoneNextActionFlow: StateFlow<HeadphoneAction>
    val headphonePlayBookmarkConfirmationSoundFlow: StateFlow<Boolean>
    val bookmarkSortTypeForPlayerFlow: StateFlow<BookmarksSortTypeForPlayer>
    val bookmarkSortTypeForPodcastFlow: StateFlow<BookmarksSortTypeForPodcast>

    fun getVersion(): String
    fun getVersionCode(): Int

    fun getSentryDsn(): String

    val skipForwardInSecs: UserSetting<Int>
    val skipBackInSecs: UserSetting<Int>

    fun syncOnMeteredNetwork(): Boolean
    fun setSyncOnMeteredNetwork(shouldSyncOnMetered: Boolean)
    fun getWorkManagerNetworkTypeConstraint(): NetworkType
    fun refreshPodcastsOnResume(isUnmetered: Boolean): Boolean
    val backgroundRefreshPodcasts: UserSetting<Boolean>
    val podcastsSortType: UserSetting<PodcastsSortType>

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

    val discoverCountryCode: UserSetting<String>

    val warnOnMeteredNetwork: UserSetting<Boolean>

    val playOverNotification: UserSetting<PlayOverNotificationSetting>

    fun setLastModified(lastModified: String?)
    fun getLastModified(): String?

    fun isFirstSyncRun(): Boolean
    fun setFirstSyncRun(firstRun: Boolean)

    fun isRestoreFromBackup(): Boolean
    fun setRestoreFromBackupEnded()

    fun clearPlusPreferences()

    val hideNotificationOnPause: UserSetting<Boolean>

    val streamingMode: UserSetting<Boolean>
    val keepScreenAwake: UserSetting<Boolean>
    val openPlayerAutomatically: UserSetting<Boolean>

    val autoDownloadUnmeteredOnly: UserSetting<Boolean>
    val autoDownloadOnlyWhenCharging: UserSetting<Boolean>
    val autoDownloadUpNext: UserSetting<Boolean>

    val useEmbeddedArtwork: UserSetting<Boolean>

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
    fun getSleepTimerCustomMins(): Int

    fun setShowPlayedEpisodes(show: Boolean)
    fun showPlayedEpisodes(): Boolean

    val showArtworkOnLockScreen: UserSetting<Boolean>
    val newEpisodeNotificationActions: UserSetting<NewEpisodeNotificationActionSetting>

    val autoArchiveIncludeStarred: UserSetting<Boolean>
    val autoArchiveAfterPlaying: UserSetting<AutoArchiveAfterPlayingSetting>
    val autoArchiveInactive: UserSetting<AutoArchiveInactiveSetting>

    fun selectedFilter(): String?
    fun setSelectedFilter(filterUUID: String?)
    fun selectedTab(): Int?
    fun setSelectedTab(selected: Int?)

    fun contains(key: String): Boolean

    val upNextSwipe: UserSetting<UpNextAction>
    val tapOnUpNextShouldPlay: UserSetting<Boolean>

    fun setHeadphoneControlsNextAction(action: HeadphoneAction)
    fun setHeadphoneControlsPreviousAction(action: HeadphoneAction)
    fun setHeadphoneControlsPlayBookmarkConfirmationSound(value: Boolean)

    // Firebase remote config
    fun getPeriodicSaveTimeMs(): Long
    fun getPodcastSearchDebounceMs(): Long
    fun getEpisodeSearchDebounceMs(): Long
    val podcastGroupingDefault: UserSetting<PodcastGrouping>

    val marketingOptIn: UserSetting<Boolean>

    val freeGiftAcknowledged: UserSetting<Boolean>

    fun setCloudSortOrder(sortOrder: CloudSortOrder)
    fun getCloudSortOrder(): CloudSortOrder
    val cloudAddToUpNext: UserSetting<Boolean>
    val deleteLocalFileAfterPlaying: UserSetting<Boolean>
    val deleteCloudFileAfterPlaying: UserSetting<Boolean>
    val cloudAutoUpload: UserSetting<Boolean>
    val cloudAutoDownload: UserSetting<Boolean>
    val cloudDownloadOnlyOnWifi: UserSetting<Boolean>
    fun getCachedSubscription(): SubscriptionStatus?
    fun setCachedSubscription(subscriptionStatus: SubscriptionStatus?)

    fun setUpgradeClosedProfile(value: Boolean)
    fun getUpgradeClosedProfile(): Boolean
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
    fun setShelfItems(items: List<String>)
    fun getSeenPlayerTour(): Boolean
    fun setSeenPlayerTour(value: Boolean)
    fun setSeenUpNextTour(value: Boolean)
    fun getSeenUpNextTour(): Boolean
    fun setTrialFinishedSeen(seen: Boolean)
    fun getTrialFinishedSeen(): Boolean
    val autoSubscribeToPlayed: UserSetting<Boolean>
    val autoShowPlayed: UserSetting<Boolean>
    val autoPlayNextEpisodeOnEmpty: UserSetting<Boolean>
    val showArchivedDefault: UserSetting<Boolean>
    val mediaControlItems: UserSetting<List<MediaNotificationControls>>
    fun setMultiSelectItems(items: List<Int>)
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
    val sendCrashReports: UserSetting<Boolean>
    val linkCrashReportsToUser: UserSetting<Boolean>

    fun setEndOfYearShowBadge2022(value: Boolean)
    fun getEndOfYearShowBadge2022(): Boolean

    fun setEndOfYearModalHasBeenShown(value: Boolean)
    fun getEndOfYearModalHasBeenShown(): Boolean

    fun hasCompletedOnboarding(): Boolean
    fun setHasDoneInitialOnboarding()

    val customMediaActionsVisibility: UserSetting<Boolean>

    fun isNotificationsDisabledMessageShown(): Boolean
    fun setNotificationsDisabledMessageShown(value: Boolean)

    fun setLastSelectedSubscriptionTier(tier: Subscription.SubscriptionTier)
    fun getLastSelectedSubscriptionTier(): Subscription.SubscriptionTier?

    fun setLastSelectedSubscriptionFrequency(frequency: SubscriptionFrequency)
    fun getLastSelectedSubscriptionFrequency(): SubscriptionFrequency?

    // This boolean should be update to false when a user signs in and should be set to
    // true once a user signs out and that sign out has been fully handled
    // by the app. This field helps make sure the app fully handles signing a user
    // out even if they sign out from outside the app (i.e., from the Android OS's
    // account management settings).
    fun setFullySignedOut(boolean: Boolean)
    fun getFullySignedOut(): Boolean

    val lastLoadedFromPodcastOrFilterUuid: UserSetting<LastPlayedList>

    // It would be better to have this be a UserSetting<ThemeType>, but that
    // is not easy due to the way our modules are structured.
    val theme: UserSetting<ThemeSetting>
    val darkThemePreference: UserSetting<ThemeSetting>
    val lightThemePreference: UserSetting<ThemeSetting>
    val useSystemTheme: UserSetting<Boolean>

    // It would be better to have this be a UserSetting<AppIconType>, but that
    // is not easy due to the way our modules are structured.
    val appIcon: UserSetting<AppIconSetting>

    fun <T> setBookmarksSortType(sortType: T)
    fun getBookmarksSortTypeForPlayer(): BookmarksSortTypeForPlayer
    fun getBookmarksSortTypeForPodcast(): BookmarksSortTypeForPodcast
}
