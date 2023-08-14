package au.com.shiftyjelly.pocketcasts.preferences

import android.content.Context
import android.net.Uri
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
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.utils.Util
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
        const val INFO_FAQ_URL = "https://support.pocketcasts.com/android/?device=android"

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
        const val LAST_SYNC_TIME = "LastSyncTime"
        const val PREFERENCE_SKIP_FORWARD = "skipForward"
        const val PREFERENCE_SKIP_BACKWARD = "skipBack"
        const val PREFERENCE_SKIP_FORWARD_NEEDS_SYNC = "skipForwardNeedsSync"
        const val PREFERENCE_SKIP_BACK_NEEDS_SYNC = "skipBackNeedsSync"

        const val PREFERENCE_MARKETING_OPT_IN = "marketingOptIn"
        const val PREFERENCE_MARKETING_OPT_IN_NEEDS_SYNC = "marketingOptInNeedsSync"
        const val PREFERENCE_FREE_GIFT_ACKNOWLEDGED = "freeGiftAck"
        const val PREFERENCE_FREE_GIFT_ACKNOWLEDGED_NEEDS_SYNC = "freeGiftAckNeedsSync"

        const val PREFERENCE_STORAGE_CHOICE = "storageChoice"
        const val PREFERENCE_STORAGE_CHOICE_NAME = "storageChoiceName"
        const val PREFERENCE_STORAGE_CUSTOM_FOLDER = "storageCustomFolder"
        const val OLD_PREFERENCE_EPISODE_NOTIFICATIONS_ON = "episodeNotificationsOn"
        const val PREFERENCE_EPISODE_NOTIFICATIONS_ON_DEFAULT = false
        const val PREFERENCE_NOTIFICATION_RINGTONE = "notificationRingtone"
        const val PREFERENCE_NOTIFICATION_VIBRATE = "notificationVibrate"
        const val PREFERENCE_NOTIFICATION_VIBRATE_DEFAULT = "2"
        const val PREFERENCE_PODCAST_LIBRARY_SORT = "podcastLibrarySort"
        const val PREFERENCE_PODCAST_LIBRARY_SORT_NEEDS_SYNC = "podcastLibrarySortNeedsSync"
        const val PREFERENCE_SELECT_PODCAST_LIBRARY_SORT = "selectPodcastLibrarySort"
        const val PREFERENCE_WARN_WHEN_NOT_ON_WIFI = "warnWhenNotOnWifi"
        const val PREFERENCE_SYNC_ON_METERED = "SyncWhenOnMetered"
        const val PREFERENCE_OVERRIDE_AUDIO_LEGACY = "overrideAudioInterruption"
        const val PREFERENCE_OVERRIDE_NOTIFICATION_AUDIO = "overrideNotificationAudio"
        const val PREFERENCE_USE_EMBEDDED_ARTWORK = "useEmbeddedArtwork"
        const val PREFERENCE_LAST_MODIFIED = "lastModified"
        const val PREFERENCE_FIRST_SYNC_RUN = "firstSyncRun"
        const val PREFERENCE_HIDE_NOTIFICATION_ON_PAUSE = "hideNotificationOnPause"
        const val PREFERENCE_GLOBAL_STREAMING_MODE = "globalStreamingMode"
        const val PREFERENCE_PODCAST_AUTO_DOWNLOAD_ON_UNMETERED = "autoDownloadOnlyDownloadOnWifi"
        const val PREFERENCE_PODCAST_AUTO_DOWNLOAD_WHEN_CHARGING = "autoDownloadOnlyDownloadWhenCharging"
        const val PREFERENCE_ALLOW_OTHER_APPS_ACCESS = "allowOtherAppsAccess"
        const val PREFERENCE_HIDE_SYNC_SETUP_MENU = "hideSyncSetupMenu"
        const val PREFERENCE_KEEP_SCREEN_AWAKE = "keepScreenAwake4"
        const val PREFERENCE_OPEN_PLAYER_AUTOMATICALLY = "openPlayerAutomatically"
        const val PREFERENCE_SHOW_NOTE_IMAGES_ON = "showNotesImagesOn"
        const val PREFERENCE_SELECTED_FILTER = "selectedFilter"
        const val PREFERENCE_CHAPTERS_EXPANDED = "chaptersExpanded"
        const val PREFERENCE_UPNEXT_EXPANDED = "upnextExpanded"
        const val INTELLIGENT_PLAYBACK_RESUMPTION = "intelligentPlaybackResumption"

        const val PREFERENCE_AUTO_PLAY_ON_EMPTY = "autoUpNextEmpty"
        const val PREFERENCE_AUTO_SUBSCRIBE_ON_PLAY = "autoSubscribeToPlayed"
        const val PREFERENCE_AUTO_SHOW_PLAYED = "autoShowPlayed"

        const val PREFERENCE_DISCOVERY_COUNTRY_CODE = "discovery_country_code"
        const val PREFERENCE_POPULAR_PODCAST_COUNTRY_CODE = "popular_podcast_country_code"
        const val STORAGE_ON_CUSTOM_FOLDER = "custom_folder"

        const val PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_PLAYER = "bookmarksSortTypeForPlayer"
        const val PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_PODCAST = "bookmarksSortTypeForPodcast"

        val SUPPORTED_LANGUAGE_CODES = arrayOf("us", "se", "jp", "gb", "fr", "es", "de", "ca", "au", "it", "ru", "br", "no", "be", "cn", "dk", "sw", "ch", "ie", "pl", "kr", "nl")

        // legacy settings
        const val LEGACY_STORAGE_ON_PHONE = "phone"
        const val LEGACY_STORAGE_ON_SD_CARD = "external"

        const val SKIP_FORWARD_DEFAULT = "30"
        const val SKIP_BACKWARD_DEFAULT = "10"

        const val LAST_MAIN_NAV_SCREEN_OPENED = "last_main_screen"

        const val AUTO_ARCHIVE_EXCLUDED_PODCASTS = "autoArchiveExcludedPodcasts"
        const val AUTO_ARCHIVE_INCLUDE_STARRED = "autoArchiveIncludeStarred"
        const val AUTO_ARCHIVE_PLAYED_EPISODES_AFTER = "autoArchivePlayedEpisodes"
        const val AUTO_ARCHIVE_INACTIVE = "autoArchiveInactiveEpisodes"

        const val INTENT_OPEN_APP_NEW_EPISODES = "INTENT_OPEN_APP_NEW_EPISODES"
        const val INTENT_OPEN_APP_DOWNLOADING = "INTENT_OPEN_APP_DOWNLOADING"
        const val INTENT_OPEN_APP_EPISODE_UUID = "INTENT_OPEN_APP_EPISODE_UUID"
        const val INTENT_OPEN_APP_ADD_BOOKMARK = "INTENT_OPEN_APP_ADD_BOOKMARK"
        const val INTENT_OPEN_APP_VIEW_BOOKMARKS = "INTENT_OPEN_APP_VIEW_BOOKMARKS"
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
        NOTIFICATION_CHANNEL_ID_BOOKMARK("bookmark"),
    }

    enum class NotificationId(val value: Int) {
        OPML(21483646),
        PLAYING(21483647),
        DOWNLOADING(21483648),
        SIGN_IN_ERROR(21483649),
        BOOKMARK(21483650),
    }

    enum class BadgeType(val labelId: Int, val analyticsValue: String) {
        OFF(labelId = LR.string.podcasts_badges_off, analyticsValue = "off"),
        LATEST_EPISODE(labelId = LR.string.podcasts_badges_only_latest_episode, analyticsValue = "only_latest_episode"),
        ALL_UNFINISHED(labelId = LR.string.podcasts_badges_all_unfinished, analyticsValue = "unfinished_episodes")
    }

    enum class PodcastGridLayoutType(val id: Int, val analyticsValue: String) {
        LARGE_ARTWORK(id = 0, analyticsValue = "large_artwork"),
        SMALL_ARTWORK(id = 1, analyticsValue = "small_artwork"),
        LIST_VIEW(id = 2, analyticsValue = "list");
        companion object {
            fun fromLayoutId(id: Int) =
                PodcastGridLayoutType.values().find { it.id == id } ?: LARGE_ARTWORK
        }
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

    enum class AutoAddUpNextLimitBehaviour {
        STOP_ADDING,
        ONLY_ADD_TO_TOP
    }

    enum class HeadphoneAction {
        ADD_BOOKMARK,
        SKIP_BACK,
        SKIP_FORWARD,
        NEXT_CHAPTER,
        PREVIOUS_CHAPTER,
    }

    sealed class AutoArchiveAfterPlaying(val timeSeconds: Int) {
        companion object {
            fun fromString(context: Context, value: String?): AutoArchiveAfterPlaying {
                val isAutomotive = Util.isAutomotive(context)
                val defaultValue = if (isAutomotive) Never else AfterPlaying

                return when (value) {
                    context.getString(LR.string.settings_auto_archive_played_never) -> Never
                    context.getString(LR.string.settings_auto_archive_played_after_playing) -> AfterPlaying
                    context.getString(LR.string.settings_auto_archive_played_after_24_hours) -> Hours24
                    context.getString(LR.string.settings_auto_archive_played_after_2_days) -> Days2
                    context.getString(LR.string.settings_auto_archive_played_after_1_week) -> Weeks1
                    else -> defaultValue
                }
            }

            val options
                get() = listOf(Never, AfterPlaying, Hours24, Days2, Weeks1)

            fun fromIndex(index: Int) = options[index]
        }

        object Never : AutoArchiveAfterPlaying(-1)
        object AfterPlaying : AutoArchiveAfterPlaying(0)
        object Hours24 : AutoArchiveAfterPlaying(24 * 60 * 60)
        object Days2 : AutoArchiveAfterPlaying(2 * 24 * 60 * 60)
        object Weeks1 : AutoArchiveAfterPlaying(7 * 24 * 60 * 60)

        fun toIndex(): Int = options.indexOf(this)
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

    sealed class AutoArchiveInactive(val timeSeconds: Int) {
        object Never : AutoArchiveInactive(-1)
        object Hours24 : AutoArchiveInactive(24 * 60 * 60)
        object Days2 : AutoArchiveInactive(2 * 24 * 60 * 60)
        object Weeks1 : AutoArchiveInactive(7 * 24 * 60 * 60)
        object Weeks2 : AutoArchiveInactive(14 * 24 * 60 * 60)
        object Days30 : AutoArchiveInactive(30 * 24 * 60 * 60)
        object Days90 : AutoArchiveInactive(90 * 24 * 60 * 60)

        companion object {
            fun fromString(context: Context, value: String?): AutoArchiveInactive {
                return when (value) {
                    context.getString(LR.string.settings_auto_archive_inactive_never) -> Never
                    context.getString(LR.string.settings_auto_archive_inactive_24_hours) -> Hours24
                    context.getString(LR.string.settings_auto_archive_inactive_2_days) -> Days2
                    context.getString(LR.string.settings_auto_archive_inactive_1_week) -> Weeks1
                    context.getString(LR.string.settings_auto_archive_inactive_2_weeks) -> Weeks2
                    context.getString(LR.string.settings_auto_archive_inactive_30_days) -> Days30
                    context.getString(LR.string.settings_auto_archive_inactive_3_months) -> Days90
                    else -> Never
                }
            }

            val options
                get() = listOf(Never, Hours24, Days2, Weeks1, Weeks2, Days30, Days90)

            fun fromIndex(index: Int) = options[index]
        }

        fun toIndex(): Int = options.indexOf(this)
    }

    val podcastLayoutObservable: Observable<Int>
    val podcastBadgeTypeObservable: Observable<BadgeType>
    val podcastSortTypeObservable: Observable<PodcastsSortType>
    val selectPodcastSortTypeObservable: Observable<PodcastsSortType>
    val skipForwardInSecsObservable: Observable<Int>
    val skipBackwardInSecsObservable: Observable<Int>
    val playbackEffectsObservable: Observable<PlaybackEffects>
    val refreshStateObservable: Observable<RefreshState>
    val upNextSwipeActionObservable: Observable<UpNextAction>
    val rowActionObservable: Observable<Boolean>
    val marketingOptObservable: Observable<Boolean>
    val isFirstSyncRunObservable: Observable<Boolean>
    val shelfItemsObservable: Observable<List<String>>
    val multiSelectItemsObservable: Observable<List<Int>>
    val autoAddUpNextLimitBehaviour: Observable<AutoAddUpNextLimitBehaviour>
    val autoAddUpNextLimit: Observable<Int>

    val defaultPodcastGroupingFlow: StateFlow<PodcastGrouping>
    val defaultMediaNotificationControlsFlow: StateFlow<List<MediaNotificationControls>>
    val defaultShowArchivedFlow: StateFlow<Boolean>
    val intelligentPlaybackResumptionFlow: StateFlow<Boolean>
    val keepScreenAwakeFlow: StateFlow<Boolean>
    val openPlayerAutomaticallyFlow: StateFlow<Boolean>
    val tapOnUpNextShouldPlayFlow: StateFlow<Boolean>
    val customMediaActionsVisibilityFlow: StateFlow<Boolean>
    val autoPlayNextEpisodeOnEmptyFlow: StateFlow<Boolean>
    val headphonePreviousActionFlow: StateFlow<HeadphoneAction>
    val headphoneNextActionFlow: StateFlow<HeadphoneAction>
    val headphonePlayBookmarkConfirmationSoundFlow: StateFlow<Boolean>
    val bookmarkSortTypeForPlayerFlow: StateFlow<BookmarksSortTypeForPlayer>
    val bookmarkSortTypeForPodcastFlow: StateFlow<BookmarksSortTypeForPodcast>

    fun getVersion(): String
    fun getVersionCode(): Int

    fun getGitHash(): String?

    fun getSentryDsn(): String

    fun isScreenReaderOn(): Boolean

    fun getSkipForwardInSecs(): Int
    fun getSkipForwardInMs(): Long
    fun getSkipBackwardInSecs(): Int
    fun getSkipBackwardInMs(): Long

    fun getLastScreenOpened(): String?
    fun setLastScreenOpened(screenId: String)

    fun syncOnMeteredNetwork(): Boolean
    fun setSyncOnMeteredNetwork(shouldSyncOnMetered: Boolean)
    fun getWorkManagerNetworkTypeConstraint(): NetworkType
    fun refreshPodcastsOnResume(isUnmetered: Boolean): Boolean
    fun refreshPodcastsAutomatically(): Boolean
    fun setRefreshPodcastsAutomatically(shouldRefresh: Boolean)
    fun setPodcastsSortType(sortType: PodcastsSortType, sync: Boolean)
    fun setPodcastsSortTypeNeedsSync(value: Boolean)
    fun getPodcastsSortTypeNeedsSync(): Boolean
    fun getPodcastsSortType(): PodcastsSortType

    fun setSelectPodcastsSortType(sortType: PodcastsSortType)
    fun getSelectPodcastsSortType(): PodcastsSortType

    fun getNotificationVibrate(): Int
    fun getNotificationSound(): Uri?
    fun getNotificationSoundPath(): String
    fun setNotificationSoundPath(path: String)

    fun isSoundOn(): Boolean

    fun isNotificationVibrateOn(): Boolean

    fun oldNotifyRefreshPodcast(): Boolean

    fun usingCustomFolderStorage(): Boolean

    fun getStorageChoice(): String?
    fun getStorageChoiceName(): String?
    fun setStorageChoice(choice: String?, name: String?)
    fun getStorageCustomFolder(): String
    fun setStorageCustomFolder(folder: String)

    fun getLastRefreshTime(): Long
    fun getLastRefreshDate(): Date?
    fun setLastSyncTime(lastSyncTime: Long)
    fun getLastSyncTime(): Long
    fun setRefreshState(refreshState: RefreshState)
    fun getRefreshState(): RefreshState?
    fun getLastSuccessRefreshState(): RefreshState?

    fun getLongForKey(key: String, defaultValue: Long): Long
    fun setLongForKey(key: String, value: Long)
    fun getBooleanForKey(key: String, defaultValue: Boolean): Boolean
    fun setBooleanForKey(key: String, value: Boolean)

    fun getDiscoveryCountryCode(): String
    fun setDiscoveryCountryCode(code: String)

    fun warnOnMeteredNetwork(): Boolean
    fun setWarnOnMeteredNetwork(warn: Boolean)

    fun getPopularPodcastCountryCode(): String

    fun setPopularPodcastCountryCode(code: String)

    fun getPlayOverNotification(): PlayOverNotificationSetting

    fun hasBlockAlreadyRun(label: String): Boolean
    fun setBlockAlreadyRun(label: String, hasRun: Boolean)

    fun setLastModified(lastModified: String?)
    fun getLastModified(): String?

    fun isFirstSyncRun(): Boolean
    fun setFirstSyncRun(firstRun: Boolean)

    fun isRestoreFromBackup(): Boolean
    fun setRestoreFromBackupEnded()

    fun clearPlusPreferences()

    fun getLanguageCode(): String

    fun hideNotificationOnPause(): Boolean

    fun streamingMode(): Boolean
    fun setStreamingMode(newValue: Boolean)

    fun keepScreenAwake(): Boolean
    fun setKeepScreenAwake(newValue: Boolean)

    fun openPlayerAutomatically(): Boolean
    fun setOpenPlayerAutomatically(newValue: Boolean)

    fun isPodcastAutoDownloadUnmeteredOnly(): Boolean
    fun isPodcastAutoDownloadPowerOnly(): Boolean
    fun isUpNextAutoDownloaded(): Boolean
    fun setUpNextAutoDownloaded(autoDownload: Boolean)

    fun getUseEmbeddedArtwork(): Boolean
    fun setUseEmbeddedArtwork(value: Boolean)

    fun getGlobalPlaybackEffects(): PlaybackEffects
    fun getGlobalPlaybackSpeed(): Double
    fun getGlobalAudioEffectRemoveSilence(): TrimMode
    fun getGlobalAudioEffectVolumeBoost(): Boolean

    fun setGlobalAudioEffects(playbackSpeed: Double, trimMode: TrimMode, isVolumeBoosted: Boolean)

    fun allowOtherAppsAccessToEpisodes(): Boolean

    fun setHideSyncSetupMenu(hide: Boolean)

    fun isSyncSetupMenuHidden(): Boolean

    fun getMigratedVersionCode(): Int

    fun setMigratedVersionCode(versionCode: Int)

    fun getPodcastBadgeType(): BadgeType
    fun setPodcastBadgeType(badgeType: BadgeType)
    fun setPodcastsLayout(layout: Int)
    fun getPodcastsLayout(): Int
    fun isPodcastsLayoutListView(): Boolean

    fun getNotificationLastSeen(): Date?
    fun setNotificationLastSeen(lastSeen: Date?)
    fun setNotificationLastSeenToNow()

    fun isShowNotesImagesOn(): Boolean

    fun setUpNextServerModified(timeMs: Long)
    fun getUpNextServerModified(): Long
    fun setHistoryServerModified(timeMs: Long)
    fun getHistoryServerModified(): Long
    fun setClearHistoryTime(timeMs: Long)
    fun setClearHistoryTimeNow()
    fun getClearHistoryTime(): Long

    fun setSleepTimerCustomMins(minutes: Int)
    fun getSleepTimerCustomMins(): Int

    fun getImageSignature(): String
    fun changeImageSignature(): String

    fun setShowPlayedEpisodes(show: Boolean)
    fun showPlayedEpisodes(): Boolean

    fun setShowArtworkOnLockScreen(show: Boolean)
    fun showArtworkOnLockScreen(): Boolean

    fun getNewEpisodeNotificationActions(): String?
    fun setNewEpisodeNotificationActions(actions: String)

    fun getAutoArchiveExcludedPodcasts(): List<String>
    fun setAutoArchiveExcludedPodcasts(excluded: List<String>)
    fun getAutoArchiveIncludeStarred(): Boolean
    fun getAutoArchiveAfterPlaying(): AutoArchiveAfterPlaying
    fun getAutoArchiveInactive(): AutoArchiveInactive

    fun selectedFilter(): String?
    fun setSelectedFilter(filterUUID: String?)
    fun selectedTab(): Int?
    fun setSelectedTab(selected: Int?)

    fun contains(key: String): Boolean
    fun getLastRefreshError(): String?

    fun getUpNextSwipeAction(): UpNextAction
    fun setUpNextSwipeAction(action: UpNextAction)
    fun getTapOnUpNextShouldPlay(): Boolean
    fun setTapOnUpNextShouldPlay(value: Boolean)

    fun getHeadphoneControlsNextAction(): HeadphoneAction
    fun setHeadphoneControlsNextAction(action: HeadphoneAction)
    fun getHeadphoneControlsPreviousAction(): HeadphoneAction
    fun setHeadphoneControlsPreviousAction(action: HeadphoneAction)
    fun getHeadphoneControlsPlayBookmarkConfirmationSound(): Boolean
    fun setHeadphoneControlsPlayBookmarkConfirmationSound(value: Boolean)

    // Firebase remote config
    fun getPeriodicSaveTimeMs(): Long
    fun getPodcastSearchDebounceMs(): Long
    fun getEpisodeSearchDebounceMs(): Long
    fun defaultPodcastGrouping(): PodcastGrouping
    fun setDefaultPodcastGrouping(podcastGrouping: PodcastGrouping)
    fun setSkipForwardInSec(value: Int)
    fun setSkipBackwardInSec(value: Int)
    fun setSkipForwardNeedsSync(value: Boolean)
    fun setSkipBackNeedsSync(value: Boolean)
    fun getSkipForwardNeedsSync(): Boolean
    fun getSkipBackNeedsSync(): Boolean

    fun getMarketingOptIn(): Boolean
    fun setMarketingOptIn(value: Boolean)
    fun getMarketingOptInNeedsSync(): Boolean
    fun setMarketingOptInNeedsSync(value: Boolean)

    fun getFreeGiftAcknowledged(): Boolean
    fun setFreeGiftAcknowledged(value: Boolean)
    fun getFreeGiftAcknowledgedNeedsSync(): Boolean
    fun setFreeGiftAcknowledgedNeedsSync(value: Boolean)

    fun setCloudSortOrder(sortOrder: CloudSortOrder)
    fun getCloudSortOrder(): CloudSortOrder
    fun getCloudAddToUpNext(): Boolean
    fun setCloudAddToUpNext(value: Boolean)
    fun getDeleteLocalFileAfterPlaying(): Boolean
    fun setDeleteLocalFileAfterPlaying(value: Boolean)
    fun getDeleteCloudFileAfterPlaying(): Boolean
    fun setDeleteCloudFileAfterPlaying(value: Boolean)
    fun getCloudAutoUpload(): Boolean
    fun setCloudAutoUpload(value: Boolean)
    fun getCloudAutoDownload(): Boolean
    fun setCloudAutoDownload(value: Boolean)
    fun getCachedSubscription(): SubscriptionStatus?
    fun setCachedSubscription(subscriptionStatus: SubscriptionStatus?)
    fun getCloudOnlyWifi(): Boolean
    fun setCloudOnlyWifi(value: Boolean)
    fun getAppIconId(): String?
    fun setAppIconId(value: String)

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
    fun getShelfItems(): List<String>
    fun setShelfItems(items: List<String>)
    fun getSeenPlayerTour(): Boolean
    fun setSeenPlayerTour(value: Boolean)
    fun setSeenUpNextTour(value: Boolean)
    fun getSeenUpNextTour(): Boolean
    fun setTrialFinishedSeen(seen: Boolean)
    fun getTrialFinishedSeen(): Boolean
    fun getAutoSubscribeToPlayed(): Boolean
    fun getAutoShowPlayed(): Boolean
    fun getAutoPlayNextEpisodeOnEmpty(): Boolean
    fun setAutoPlayNextEpisodeOnEmpty(enabled: Boolean)
    fun defaultShowArchived(): Boolean
    fun setDefaultShowArchived(value: Boolean)
    fun getMediaNotificationControlItems(): List<MediaNotificationControls>
    fun setMediaNotificationControlItems(items: List<String>)
    fun setMultiSelectItems(items: List<Int>)
    fun getMultiSelectItems(): List<Int>
    fun setLastPauseTime(date: Date)
    fun getLastPauseTime(): Date?
    fun setLastPausedUUID(uuid: String)
    fun getLastPausedUUID(): String?
    fun setLastPausedAt(pausedAt: Int)
    fun getLastPausedAt(): Int?
    fun getIntelligentPlaybackResumption(): Boolean
    fun setIntelligentPlaybackResumption(value: Boolean)
    fun getAutoAddUpNextLimit(): Int
    fun setAutoAddUpNextLimit(limit: Int)
    fun setAutoAddUpNextLimitBehaviour(value: AutoAddUpNextLimitBehaviour)
    fun getAutoAddUpNextLimitBehaviour(): AutoAddUpNextLimitBehaviour
    fun getMaxUpNextEpisodes(): Int
    fun getUniqueDeviceId(): String
    fun setHomeGridNeedsRefresh(value: Boolean)
    fun getHomeGridNeedsRefresh(): Boolean

    fun setTimesToShowBatteryWarning(value: Int)
    fun getTimesToShowBatteryWarning(): Int

    // Only the AnalyticsTracker object should update or retrieve SendUsageState directly. Everything else
    // should update/access this setting through the AnalyticsTracker.
    fun setSendUsageStats(value: Boolean)
    fun getSendUsageStats(): Boolean

    fun setSendCrashReports(value: Boolean)
    fun getSendCrashReports(): Boolean

    fun setLinkCrashReportsToUser(value: Boolean)
    fun getLinkCrashReportsToUser(): Boolean

    fun setEndOfYearShowBadge2022(value: Boolean)
    fun getEndOfYearShowBadge2022(): Boolean

    fun setEndOfYearModalHasBeenShown(value: Boolean)
    fun getEndOfYearModalHasBeenShown(): Boolean

    fun hasCompletedOnboarding(): Boolean
    fun setHasDoneInitialOnboarding()

    fun areCustomMediaActionsVisible(): Boolean
    fun setCustomMediaActionsVisible(value: Boolean)

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

    fun getlastLoadedFromPodcastOrFilterUuid(): String?
    fun setlastLoadedFromPodcastOrFilterUuid(uuid: String?)

    fun <T> setBookmarksSortType(sortType: T)
    fun getBookmarksSortTypeForPlayer(): BookmarksSortTypeForPlayer
    fun getBookmarksSortTypeForPodcast(): BookmarksSortTypeForPodcast
}
