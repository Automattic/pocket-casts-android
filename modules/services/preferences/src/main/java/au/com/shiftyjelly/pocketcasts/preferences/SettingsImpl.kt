package au.com.shiftyjelly.pocketcasts.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.util.Base64
import androidx.work.NetworkType
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.DEFAULT_MAX_AUTO_ADD_LIMIT
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.NOTIFICATIONS_DISABLED_MESSAGE_SHOWN
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.SETTINGS_ENCRYPT_SECRET
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.preferences.di.PrivateSharedPreferences
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import au.com.shiftyjelly.pocketcasts.preferences.model.AppIconSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoAddUpNextLimitBehaviour
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveAfterPlayingSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveInactiveSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationActionSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ThemeSetting
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.config.FirebaseConfig
import au.com.shiftyjelly.pocketcasts.utils.extensions.isScreenReaderOn
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.nio.charset.Charset
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec
import javax.inject.Inject
import kotlin.math.max

class SettingsImpl @Inject constructor(
    @PublicSharedPreferences private val sharedPreferences: SharedPreferences,
    @PrivateSharedPreferences private val privatePreferences: SharedPreferences,
    @ApplicationContext private val context: Context,
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val moshi: Moshi
) : Settings {

    companion object {
        private const val DEVICE_ID_KEY = "DeviceIdKey"
        private const val SHOWN_BATTERY_WARNING_KEY = "ShownBetteryWarningKey"
        private const val SEND_USAGE_STATS_KEY = "SendUsageStatsKey"
        private const val SEND_CRASH_REPORTS_KEY = "SendCrashReportsKey"
        private const val LINK_CRASH_REPORTS_TO_USER_KEY = "LinkCrashReportsToUserKey"
        private const val END_OF_YEAR_SHOW_BADGE_2022_KEY = "EndOfYearShowBadge2022Key"
        private const val END_OF_YEAR_MODAL_HAS_BEEN_SHOWN_KEY = "EndOfYearModalHasBeenShownKey"
        private const val DONE_INITIAL_ONBOARDING_KEY = "CompletedOnboardingKey"
        private const val LAST_SELECTED_SUBSCRIPTION_TIER_KEY = "LastSelectedSubscriptionTierKey"
        private const val LAST_SELECTED_SUBSCRIPTION_FREQUENCY_KEY = "LastSelectedSubscriptionFrequencyKey"
        private const val PROCESSED_SIGNOUT_KEY = "ProcessedSignout"
        private const val LAST_SELECTED_PODCAST_OR_FILTER_UUID = "LastSelectedPodcastOrFilterUuid"
    }

    private var languageCode: String? = null

    override val podcastLayoutObservable = BehaviorRelay.create<Int>().apply { accept(getPodcastsLayout()) }
    override val podcastBadgeTypeObservable = BehaviorRelay.create<Settings.BadgeType>().apply { accept(getPodcastBadgeType()) }
    override val podcastSortTypeObservable = BehaviorRelay.create<PodcastsSortType>().apply { accept(getPodcastsSortType()) }
    override val selectPodcastSortTypeObservable = BehaviorRelay.create<PodcastsSortType>().apply { accept(getSelectPodcastsSortType()) }
    override val playbackEffectsObservable = BehaviorRelay.create<PlaybackEffects>().apply { accept(getGlobalPlaybackEffects()) }
    override val marketingOptObservable = BehaviorRelay.create<Boolean>().apply { accept(getMarketingOptIn()) }
    override val isFirstSyncRunObservable = BehaviorRelay.create<Boolean>().apply { accept(isFirstSyncRun()) }
    override val shelfItemsObservable = BehaviorRelay.create<List<String>>().apply { accept(getShelfItems()) }
    override val multiSelectItemsObservable = BehaviorRelay.create<List<Int>>().apply { accept(getMultiSelectItems()) }

    override val headphonePreviousActionFlow = MutableStateFlow(getHeadphoneControlsPreviousAction())
    override val headphoneNextActionFlow = MutableStateFlow(getHeadphoneControlsNextAction())
    override val headphonePlayBookmarkConfirmationSoundFlow = MutableStateFlow(getHeadphoneControlsPlayBookmarkConfirmationSound())
    override val bookmarkSortTypeFlow = MutableStateFlow(getBookmarksSortType())

    override val refreshStateObservable = BehaviorRelay.create<RefreshState>().apply {
        val lastError = getLastRefreshError()
        val refreshDate = getLastRefreshDate()
        val state = when {
            lastError != null -> RefreshState.Failed(lastError)
            refreshDate != null -> RefreshState.Success(refreshDate)
            else -> RefreshState.Never
        }
        accept(state)
    }

    override fun getVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    override fun getGitHash(): String? {
        try {
            val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(context.packageName, PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            }
            return applicationInfo.metaData.getString("au.com.shiftyjelly.pocketcasts.gitHash")
        } catch (e: NameNotFoundException) {
            return null
        }
    }

    override fun getSentryDsn(): String {
        return try {
            val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(context.packageName, PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            }
            applicationInfo.metaData.getString("au.com.shiftyjelly.pocketcasts.sentryDsn", "")
        } catch (e: NameNotFoundException) {
            ""
        }
    }

    override fun isScreenReaderOn(): Boolean {
        return context.isScreenReaderOn()
    }

    override val skipBackInSecs = UserSetting.SkipAmountPref(
        sharedPrefKey = Settings.PREFERENCE_SKIP_BACKWARD,
        defaultValue = 10,
        sharedPrefs = sharedPreferences,
    )

    override val skipForwardInSecs = UserSetting.SkipAmountPref(
        sharedPrefKey = Settings.PREFERENCE_SKIP_FORWARD,
        defaultValue = 30,
        sharedPrefs = sharedPreferences,
    )

    override fun getMarketingOptIn(): Boolean {
        return getBoolean(Settings.PREFERENCE_MARKETING_OPT_IN, false)
    }

    override fun setMarketingOptIn(value: Boolean) {
        setBoolean(Settings.PREFERENCE_MARKETING_OPT_IN, value)
        marketingOptObservable.accept(value)
    }

    override fun getMarketingOptInNeedsSync(): Boolean {
        return getBoolean(Settings.PREFERENCE_MARKETING_OPT_IN_NEEDS_SYNC, false)
    }

    override fun setMarketingOptInNeedsSync(value: Boolean) {
        setBoolean(Settings.PREFERENCE_MARKETING_OPT_IN_NEEDS_SYNC, value)
    }

    override fun getFreeGiftAcknowledged(): Boolean {
        return getBoolean(Settings.PREFERENCE_FREE_GIFT_ACKNOWLEDGED, false)
    }

    override fun setFreeGiftAcknowledged(value: Boolean) {
        setBoolean(Settings.PREFERENCE_FREE_GIFT_ACKNOWLEDGED, value)
    }

    override fun getFreeGiftAcknowledgedNeedsSync(): Boolean {
        return getBoolean(Settings.PREFERENCE_FREE_GIFT_ACKNOWLEDGED_NEEDS_SYNC, false)
    }

    override fun setFreeGiftAcknowledgedNeedsSync(value: Boolean) {
        setBoolean(Settings.PREFERENCE_FREE_GIFT_ACKNOWLEDGED_NEEDS_SYNC, value)
    }

    override fun getCancelledAcknowledged(): Boolean {
        return getBoolean("cancelled_acknowledged", false)
    }

    override fun setCancelledAcknowledged(value: Boolean) {
        setBoolean("cancelled_acknowledged", value)
    }

    override fun getLastScreenOpened(): String? {
        return sharedPreferences.getString(Settings.LAST_MAIN_NAV_SCREEN_OPENED, null)
    }

    override fun setLastScreenOpened(screenId: String) {
        val editor = sharedPreferences.edit()
        editor.putString(Settings.LAST_MAIN_NAV_SCREEN_OPENED, screenId)
        editor.apply()
    }

    override fun syncOnMeteredNetwork(): Boolean {
        return getBoolean(Settings.PREFERENCE_SYNC_ON_METERED, true)
    }

    override fun setSyncOnMeteredNetwork(shouldSyncOnMetered: Boolean) {
        setBoolean(Settings.PREFERENCE_SYNC_ON_METERED, shouldSyncOnMetered)
    }

    override fun getWorkManagerNetworkTypeConstraint(): NetworkType =
        if (syncOnMeteredNetwork()) {
            NetworkType.CONNECTED
        } else {
            NetworkType.UNMETERED
        }

    override fun refreshPodcastsOnResume(isUnmetered: Boolean): Boolean =
        syncOnMeteredNetwork() || isUnmetered

    override fun refreshPodcastsAutomatically(): Boolean {
        val isWear = Util.isWearOs(context)
        return getBoolean("backgroundRefresh", !isWear)
    }

    override fun setRefreshPodcastsAutomatically(shouldRefresh: Boolean) {
        return setBoolean("backgroundRefresh", shouldRefresh)
    }

    override fun setPodcastsSortType(sortType: PodcastsSortType, sync: Boolean) {
        if (getPodcastsSortType() == sortType) {
            return
        }
        val editor = sharedPreferences.edit()
        editor.putString(Settings.PREFERENCE_PODCAST_LIBRARY_SORT, sortType.clientId.toString())
        editor.apply()
        podcastSortTypeObservable.accept(sortType)
        if (sync) {
            setPodcastsSortTypeNeedsSync(true)
        }
    }

    override fun setPodcastsSortTypeNeedsSync(value: Boolean) {
        setBoolean(Settings.PREFERENCE_PODCAST_LIBRARY_SORT_NEEDS_SYNC, value)
    }

    override fun getPodcastsSortTypeNeedsSync(): Boolean {
        return getBoolean(Settings.PREFERENCE_PODCAST_LIBRARY_SORT_NEEDS_SYNC, false)
    }

    override fun getPodcastsSortType(): PodcastsSortType {
        val default = PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST
        val defaultId = default.clientId.toString()
        val sortOrderId = Integer.parseInt(getString(Settings.PREFERENCE_PODCAST_LIBRARY_SORT, defaultId) ?: defaultId)
        return PodcastsSortType.values().find { it.clientId == sortOrderId } ?: default
    }

    override fun setSelectPodcastsSortType(sortType: PodcastsSortType) {
        sharedPreferences.edit().apply {
            putString(Settings.PREFERENCE_SELECT_PODCAST_LIBRARY_SORT, sortType.clientId.toString())
            apply()
        }
        selectPodcastSortTypeObservable.accept(sortType)
    }

    override fun getSelectPodcastsSortType(): PodcastsSortType {
        val default = PodcastsSortType.NAME_A_TO_Z
        val defaultId = default.clientId.toString()
        val sortOrderId = Integer.parseInt(getString(Settings.PREFERENCE_SELECT_PODCAST_LIBRARY_SORT, defaultId) ?: defaultId)
        return PodcastsSortType.values().find { it.clientId == sortOrderId } ?: default
    }

    override val notificationVibrate: UserSetting<NotificationVibrateSetting> = run {
        UserSetting.PrefFromString(
            sharedPrefKey = "notificationVibrate",
            defaultValue = NotificationVibrateSetting.DEFAULT,
            sharedPrefs = sharedPreferences,
            fromString = {
                try {
                    val intValue = Integer.parseInt(it)
                    NotificationVibrateSetting
                        .values()
                        .find { setting ->
                            setting.intValue == intValue
                        }
                } catch (e: NumberFormatException) {
                    null
                } ?: NotificationVibrateSetting.DEFAULT
            },
            toString = { it.intValue.toString() }
        )
    }

    override val notificationSound = UserSetting.PrefFromString(
        sharedPrefKey = "notificationRingtone",
        defaultValue = NotificationSound(context = context),
        sharedPrefs = sharedPreferences,
        fromString = { NotificationSound(it, context) },
        toString = { it.path }
    )

    override val notifyRefreshPodcast = UserSetting.BoolPref(
        sharedPrefKey = "episodeNotificationsOn",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override fun usingCustomFolderStorage(): Boolean {
        val storageChoice = getStorageChoice()
        return storageChoice != null && storageChoice == Settings.STORAGE_ON_CUSTOM_FOLDER
    }

    override fun getStorageChoice(): String? {
        return sharedPreferences.getString(Settings.PREFERENCE_STORAGE_CHOICE, null)
    }

    override fun getStorageChoiceName(): String? {
        return sharedPreferences.getString(Settings.PREFERENCE_STORAGE_CHOICE_NAME, null)
    }

    override fun setStorageChoice(choice: String?, name: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(Settings.PREFERENCE_STORAGE_CHOICE, choice)
        editor.putString(Settings.PREFERENCE_STORAGE_CHOICE_NAME, name)
        editor.apply()
    }

    override fun getStorageCustomFolder(): String {
        return sharedPreferences.getString(Settings.PREFERENCE_STORAGE_CUSTOM_FOLDER, "") ?: ""
    }

    override fun setStorageCustomFolder(folder: String) {
        val editor = sharedPreferences.edit()
        editor.putString(Settings.PREFERENCE_STORAGE_CUSTOM_FOLDER, folder)
        editor.putString(Settings.PREFERENCE_STORAGE_CHOICE, Settings.STORAGE_ON_CUSTOM_FOLDER)
        editor.apply()
    }

    override fun getLastRefreshTime(): Long {
        return sharedPreferences.getLong(Settings.LAST_UPDATE_TIME, 0)
    }

    override fun getLastRefreshDate(): Date? {
        val lastRefreshDate = getLastRefreshTime()
        return if (lastRefreshDate == 0L) null else Date(lastRefreshDate)
    }

    private fun setLastRefreshTime(lastUpdateTime: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(Settings.LAST_UPDATE_TIME, lastUpdateTime)
        editor.apply()
    }

    override fun setRefreshState(refreshState: RefreshState) {
        when (refreshState) {
            is RefreshState.Success -> setLastRefreshTime(refreshState.date.time)
            is RefreshState.Failed -> setLastRefreshError(refreshState.error)
            else -> {}
        }
        refreshStateObservable.accept(refreshState)
    }

    override fun getRefreshState(): RefreshState? {
        return refreshStateObservable.value
    }

    override fun getLastSuccessRefreshState(): RefreshState? {
        return getLastRefreshDate()?.let { RefreshState.Success(it) }
    }

    override fun getLastRefreshError(): String? {
        return getString("last_refresh_error", null)
    }

    private fun setLastRefreshError(error: String?) {
        setString("last_refresh_error", error, true)
    }

    override fun setLastSyncTime(lastSyncTime: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(Settings.LAST_SYNC_TIME, lastSyncTime)
        editor.apply()
    }

    override fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(Settings.LAST_SYNC_TIME, 0)
    }

    override fun getLongForKey(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    override fun setLongForKey(key: String, value: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    override fun getBooleanForKey(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    override fun setBooleanForKey(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    override fun getDiscoveryCountryCode(): String {
        val countryCode = sharedPreferences.getString(Settings.PREFERENCE_DISCOVERY_COUNTRY_CODE, null)
        return countryCode ?: getLanguageCode()
    }

    override fun setDiscoveryCountryCode(code: String) {
        val editor = sharedPreferences.edit()
        editor.putString(Settings.PREFERENCE_DISCOVERY_COUNTRY_CODE, code)
        editor.apply()
    }

    override fun warnOnMeteredNetwork(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_WARN_WHEN_NOT_ON_WIFI, false)
    }

    override fun setWarnOnMeteredNetwork(warn: Boolean) {
        setBoolean(Settings.PREFERENCE_WARN_WHEN_NOT_ON_WIFI, warn)
    }

    override fun getPopularPodcastCountryCode(): String {
        return sharedPreferences.getString(Settings.PREFERENCE_POPULAR_PODCAST_COUNTRY_CODE, "") ?: ""
    }

    override fun setPopularPodcastCountryCode(code: String) {
        val editor = sharedPreferences.edit()
        editor.putString(Settings.PREFERENCE_POPULAR_PODCAST_COUNTRY_CODE, code)
        editor.apply()
    }

    override fun getAutoSubscribeToPlayed(): Boolean {
        return getBoolean(Settings.PREFERENCE_AUTO_SUBSCRIBE_ON_PLAY, false)
    }

    override fun getAutoShowPlayed(): Boolean {
        return getBoolean(Settings.PREFERENCE_AUTO_SHOW_PLAYED, false)
    }

    override val playOverNotification = UserSetting.PrefFromString<PlayOverNotificationSetting>(
        sharedPrefKey = "overrideNotificationAudio",
        defaultValue = if (sharedPreferences.getBoolean("overrideAudioInterruption", false)) {
            // default to ALWAYS because legacy override audio interruption was set to true
            PlayOverNotificationSetting.ALWAYS
        } else {
            PlayOverNotificationSetting.NEVER
        },
        sharedPrefs = sharedPreferences,
        fromString = { PlayOverNotificationSetting.fromPreferenceString(it) },
        toString = { it.preferenceInt.toString() }
    )

    override fun hasBlockAlreadyRun(label: String): Boolean {
        return sharedPreferences.getBoolean("blockAlreadyRun$label", false)
    }

    override fun setBlockAlreadyRun(label: String, hasRun: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("blockAlreadyRun$label", hasRun)
        editor.apply()
    }

    override fun setLastModified(lastModified: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(Settings.PREFERENCE_LAST_MODIFIED, lastModified)
        editor.apply()
    }

    override fun getLastModified(): String? {
        return sharedPreferences.getString(Settings.PREFERENCE_LAST_MODIFIED, null)
    }

    override fun isFirstSyncRun(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_FIRST_SYNC_RUN, true)
    }

    override fun setFirstSyncRun(firstRun: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(Settings.PREFERENCE_FIRST_SYNC_RUN, firstRun)
        editor.apply()
        isFirstSyncRunObservable.accept(firstRun)
    }

    override fun isRestoreFromBackup(): Boolean {
        val preferenceDeviceUuid = getString("deviceUuid", null)
        val currentDeviceUuid = getUniqueDeviceId()
        if (preferenceDeviceUuid.isNullOrBlank()) {
            setRestoreFromBackupEnded()
            return false
        }
        return preferenceDeviceUuid != currentDeviceUuid
    }

    override fun setRestoreFromBackupEnded() {
        setString("deviceUuid", getUniqueDeviceId())
    }

    @SuppressLint("HardwareIds")
    @Suppress("DEPRECATION")
    private fun encrypt(value: String?): String {
        try {
            val bytes = value?.toByteArray(charset("utf-8")) ?: ByteArray(0)
            val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
            val key = keyFactory.generateSecret(PBEKeySpec(SETTINGS_ENCRYPT_SECRET))
            val pbeCipher = Cipher.getInstance("PBEWithMD5AndDES")
            pbeCipher.init(
                Cipher.ENCRYPT_MODE, key,
                PBEParameterSpec(
                    android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.System.ANDROID_ID
                    ).toByteArray(charset("utf-8")),
                    20
                )
            )
            return String(
                Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP),
                Charset.forName("utf-8")
            )
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    @SuppressLint("HardwareIds")
    @Suppress("DEPRECATION")
    private fun decrypt(value: String?): String? {
        if (value == null) {
            return null
        }
        try {
            val bytes = Base64.decode(value, Base64.DEFAULT)
            val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
            val key = keyFactory.generateSecret(PBEKeySpec(SETTINGS_ENCRYPT_SECRET))
            val pbeCipher = Cipher.getInstance("PBEWithMD5AndDES")
            pbeCipher.init(Cipher.DECRYPT_MODE, key, PBEParameterSpec(android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.System.ANDROID_ID).toByteArray(charset("utf-8")), 20))
            return String(pbeCipher.doFinal(bytes), Charset.forName("utf-8"))
        } catch (e: Exception) {
            Timber.e(e)
            return null
        }
    }

    override fun clearPlusPreferences() {
        setDeleteCloudFileAfterPlaying(false)
        setCloudAutoUpload(false)
        setCloudAutoDownload(false)
        setCloudOnlyWifi(false)
        setCancelledAcknowledged(false)
    }

    override fun getLanguageCode(): String {
        val languageCode = languageCode
        if (languageCode != null) {
            return languageCode
        }

        val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        val language = locale.country.lowercase(Locale.US) // e.g. de

        for (i in Settings.SUPPORTED_LANGUAGE_CODES.indices) {
            if (Settings.SUPPORTED_LANGUAGE_CODES[i] == language) {
                this.languageCode = language
                return language
            }
        }

        this.languageCode = "us"
        return "us"
    }

    override val hideNotificationOnPause = UserSetting.BoolPref(
        sharedPrefKey = "hideNotificationOnPause",
        defaultValue = false,
        sharedPrefs = sharedPreferences
    )

    override val streamingMode: UserSetting<Boolean> = UserSetting.BoolPref(
        sharedPrefKey = Settings.PREFERENCE_GLOBAL_STREAMING_MODE,
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val keepScreenAwake = UserSetting.BoolPref(
        sharedPrefKey = "keepScreenAwake4", // Yes, there is a 4 at the end. I don't know why.
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val openPlayerAutomatically = UserSetting.BoolPref(
        sharedPrefKey = "openPlayerAutomatically",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override fun isPodcastAutoDownloadUnmeteredOnly(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_PODCAST_AUTO_DOWNLOAD_ON_UNMETERED, true)
    }

    override fun isUpNextAutoDownloaded(): Boolean {
        return getBoolean("autoDownloadUpNext", false)
    }

    override fun setUpNextAutoDownloaded(autoDownload: Boolean) {
        setBoolean("autoDownloadUpNext", autoDownload)
    }

    override fun isPodcastAutoDownloadPowerOnly(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_PODCAST_AUTO_DOWNLOAD_WHEN_CHARGING, false)
    }

    override val useEmbeddedArtwork = UserSetting.BoolPref(
        sharedPrefKey = "useEmbeddedArtwork",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override fun getGlobalPlaybackEffects(): PlaybackEffects {
        val effects = PlaybackEffects()
        effects.playbackSpeed = getGlobalPlaybackSpeed()
        effects.trimMode = getGlobalAudioEffectRemoveSilence()
        effects.isVolumeBoosted = getGlobalAudioEffectVolumeBoost()
        return effects
    }

    override fun getGlobalPlaybackSpeed(): Double {
        return java.lang.Double.valueOf("" + sharedPreferences.getFloat("globalPlaybackSpeed", 1f))
    }

    override fun getGlobalAudioEffectRemoveSilence(): TrimMode {
        val oldDefault = sharedPreferences.getBoolean("globalAudioEffectRemoveSilence", false)
        val default = if (oldDefault) 1 else 0

        val trimModeIndex = sharedPreferences.getInt("globalTrimMode", default)
        return TrimMode.values().getOrNull(trimModeIndex) ?: TrimMode.OFF
    }

    override fun getGlobalAudioEffectVolumeBoost(): Boolean {
        return sharedPreferences.getBoolean("globalAudioEffectVolumeBoost", false)
    }

    override fun setGlobalAudioEffects(playbackSpeed: Double, trimMode: TrimMode, isVolumeBoosted: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putFloat("globalPlaybackSpeed", playbackSpeed.toFloat())
        editor.putBoolean("globalAudioEffectRemoveSilence", trimMode != TrimMode.OFF)
        editor.putInt("globalTrimMode", trimMode.ordinal)
        editor.putBoolean("globalAudioEffectVolumeBoost", isVolumeBoosted)
        editor.apply()
        val effects = PlaybackEffects()
        effects.playbackSpeed = playbackSpeed
        effects.trimMode = trimMode
        effects.isVolumeBoosted = isVolumeBoosted
        playbackEffectsObservable.accept(effects)
    }

    override fun allowOtherAppsAccessToEpisodes(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_ALLOW_OTHER_APPS_ACCESS, false)
    }

    override fun setHideSyncSetupMenu(hide: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(Settings.PREFERENCE_HIDE_SYNC_SETUP_MENU, hide)
        editor.apply()
    }

    override fun isSyncSetupMenuHidden(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_HIDE_SYNC_SETUP_MENU, false)
    }

    override fun getMigratedVersionCode(): Int {
        return getInt("MIGRATED_VERSION_CODE", 0)
    }

    override fun setMigratedVersionCode(versionCode: Int) {
        setInt("MIGRATED_VERSION_CODE", versionCode)
    }

    override fun getPodcastBadgeType(): Settings.BadgeType {
        return Settings.BadgeType.values()[getInt("PODCAST_BADGE_TYPE", Settings.BadgeType.OFF.ordinal)]
    }

    override fun setPodcastBadgeType(badgeType: Settings.BadgeType) {
        setInt("PODCAST_BADGE_TYPE", badgeType.ordinal)
        podcastBadgeTypeObservable.accept(badgeType)
    }

    override fun setPodcastsLayout(layout: Int) {
        setInt("PODCAST_GRID_LAYOUT", layout)
        podcastLayoutObservable.accept(layout)
    }

    override fun getNotificationLastSeen(): Date? {
        return getDate("NOTIFICATION_LAST_SEEN")
    }

    override fun setNotificationLastSeen(lastSeen: Date?) {
        setDate("NOTIFICATION_LAST_SEEN", lastSeen)
    }

    override fun setNotificationLastSeenToNow() {
        setNotificationLastSeen(Date())
    }

    override fun isShowNotesImagesOn(): Boolean {
        return getBoolean(Settings.PREFERENCE_SHOW_NOTE_IMAGES_ON, true)
    }

    override fun setUpNextServerModified(timeMs: Long) {
        setLong("upNextModified", timeMs)
    }

    override fun getUpNextServerModified(): Long {
        return getLong("upNextModified", 0)
    }

    override fun getHistoryServerModified(): Long {
        return getLong("historyModified", 0)
    }

    override fun setHistoryServerModified(timeMs: Long) {
        setLong("historyModified", timeMs)
    }

    override fun setClearHistoryTime(timeMs: Long) {
        setLong("clearHistoryTime", timeMs)
    }

    override fun setClearHistoryTimeNow() {
        setClearHistoryTime(Date().time)
    }

    override fun getClearHistoryTime(): Long {
        return getLong("clearHistoryTime", 0)
    }

    override fun setSleepTimerCustomMins(minutes: Int) {
        setInt("sleepTimerCustomMins", minutes)
    }

    override fun getSleepTimerCustomMins(): Int {
        return getInt("sleepTimerCustomMins", 5)
    }

    override fun getImageSignature(): String {
        var signature = getString("imageSignature", null)
        if (signature == null) {
            signature = changeImageSignature()
        }
        return signature
    }

    override fun changeImageSignature(): String {
        val signature = System.currentTimeMillis().toString()
        setString("imageSignature", signature)
        return signature
    }

    override fun setShowPlayedEpisodes(show: Boolean) {
        setBoolean("showPlayedEpisodes", show)
    }

    override fun showPlayedEpisodes(): Boolean {
        return getBoolean("showPlayedEpisodes", true)
    }

    override val showArtworkOnLockScreen = UserSetting.BoolPref(
        sharedPrefKey = "showArtworkOnLockScreen",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override fun selectedFilter(): String? {
        return getString(Settings.PREFERENCE_SELECTED_FILTER, null)
    }

    override fun setSelectedFilter(filterUUID: String?) {
        setString(Settings.PREFERENCE_SELECTED_FILTER, filterUUID, now = true)
    }

    override fun setSelectedTab(selected: Int?) {
        if (selected != null) {
            setInt("selected_tab", selected)
        } else {
            sharedPreferences.edit().remove("selected_tab").commit()
        }
    }

    override fun selectedTab(): Int? {
        if (contains("selected_tab")) {
            return getInt("selected_tab", 0)
        }

        return null
    }

    override fun setTrialFinishedSeen(seen: Boolean) {
        setBoolean("trial_finished_seen", seen)
    }

    override fun getTrialFinishedSeen(): Boolean {
        return getBoolean("trial_finished_seen", defaultValue = true)
    }

    override fun setLastPauseTime(date: Date) {
        setDate("last_pause_time", date)
    }

    override fun getLastPauseTime(): Date? {
        return getDate("last_pause_time")
    }

    override fun setLastPausedUUID(uuid: String) {
        setString("last_paused_uuid", uuid)
    }

    override fun getLastPausedUUID(): String? {
        return getString("last_paused_uuid")
    }

    override fun setLastPausedAt(pausedAt: Int) {
        setInt("last_paused_at", pausedAt)
    }

    override fun getLastPausedAt(): Int? {
        val lastPausedAt = getInt("last_paused_at", 0)
        return if (lastPausedAt != 0) lastPausedAt else null
    }

    override val intelligentPlaybackResumption = UserSetting.BoolPref(
        sharedPrefKey = Settings.INTELLIGENT_PLAYBACK_RESUMPTION,
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    private fun setDate(preference: String, date: Date?) {
        val editor = sharedPreferences.edit()
        if (date == null) {
            editor.remove(preference)
        } else {
            editor.putLong(preference, date.time)
        }
        editor.apply()
    }

    private fun getDate(preference: String): Date? {
        val dateLong = sharedPreferences.getLong(preference, 0)
        return if (dateLong == 0L) null else Date(dateLong)
    }

    private fun setLong(preference: String, value: Long?) {
        val editor = sharedPreferences.edit()
        if (value == null) {
            editor.remove(preference)
        } else {
            editor.putLong(preference, value)
        }
        editor.apply()
    }

    private fun getLong(preference: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(preference, defaultValue)
    }

    private fun setInt(preference: String, value: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(preference, value)
        editor.apply()
    }

    private fun getInt(preference: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(preference, defaultValue)
    }

    private fun setBoolean(preference: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(preference, value)
        editor.apply()
    }

    private fun getBoolean(preference: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(preference, defaultValue)
    }

    private fun setString(preference: String, value: String?, now: Boolean = false) {
        val editor = sharedPreferences.edit()
        editor.putString(preference, value)
        if (now) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    private fun getString(preference: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(preference, defaultValue) ?: defaultValue
    }

    override val newEpisodeNotificationActions = UserSetting.PrefFromString<NewEpisodeNotificationActionSetting>(
        sharedPrefKey = "notification_actions",
        defaultValue = NewEpisodeNotificationActionSetting.Default,
        sharedPrefs = sharedPreferences,
        fromString = {
            when (it) {
                NewEpisodeNotificationActionSetting.Default.stringValue -> NewEpisodeNotificationActionSetting.Default
                else -> NewEpisodeNotificationActionSetting.ValueOf(it)
            }
        },
        toString = {
            when (it) {
                NewEpisodeNotificationActionSetting.Default -> NewEpisodeNotificationActionSetting.Default.stringValue
                is NewEpisodeNotificationActionSetting.ValueOf -> it.value
            }
        },
    )

    override fun getPodcastsLayout(): Int {
        return getInt("PODCAST_GRID_LAYOUT", Settings.PodcastGridLayoutType.LARGE_ARTWORK.id)
    }

    override fun isPodcastsLayoutListView(): Boolean {
        return getPodcastsLayout() == Settings.PodcastGridLayoutType.LIST_VIEW.id
    }

    override fun getAutoArchiveExcludedPodcasts(): List<String> {
        return sharedPreferences.getStringSet(Settings.AUTO_ARCHIVE_EXCLUDED_PODCASTS, null)?.toList() ?: emptyList()
    }

    override fun setAutoArchiveExcludedPodcasts(excluded: List<String>) {
        sharedPreferences.edit().putStringSet(Settings.AUTO_ARCHIVE_EXCLUDED_PODCASTS, excluded.toSet()).apply()
    }

    override val autoPlayNextEpisodeOnEmpty = UserSetting.BoolPref(
        sharedPrefKey = "autoUpNextEmpty",
        defaultValue = when (Util.getAppPlatform(context)) {
            AppPlatform.Automotive -> true
            AppPlatform.Phone,
            AppPlatform.WearOs -> false
        },
        sharedPrefs = sharedPreferences,
    )

    override val autoArchiveIncludeStarred = UserSetting.BoolPref(
        sharedPrefKey = Settings.AUTO_ARCHIVE_INCLUDE_STARRED,
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val autoArchiveAfterPlaying = UserSetting.PrefFromInt(
        sharedPrefKey = "autoArchivePlayedEpisodesIndex",
        defaultValue = getString("autoArchivePlayedEpisodes")?.let {
            // Use the old String setting if it exists before falling back to the default value
            AutoArchiveAfterPlayingSetting.fromString(it, context)
        } ?: AutoArchiveAfterPlayingSetting.defaultValue(context),
        sharedPrefs = sharedPreferences,
        fromInt = { AutoArchiveAfterPlayingSetting.fromIndex(it) },
        toInt = { it.toIndex() },
    )

    override val autoArchiveInactive = UserSetting.PrefFromInt(
        sharedPrefKey = "autoArchiveInactiveIndex",
        defaultValue = getString("autoArchiveInactiveEpisodes")?.let {
            // Use the old String setting if it exists before falling back to the default value
            AutoArchiveInactiveSetting.fromString(it, context)
        } ?: AutoArchiveInactiveSetting.default,
        sharedPrefs = sharedPreferences,
        fromInt = { AutoArchiveInactiveSetting.fromIndex(it) },
        toInt = { it.toIndex() },
    )

    override fun getCustomStorageLimitGb(): Long {
        return getRemoteConfigLong(FirebaseConfig.CLOUD_STORAGE_LIMIT)
    }

    override fun getPeriodicSaveTimeMs(): Long {
        return getRemoteConfigLong(FirebaseConfig.PERIODIC_SAVE_TIME_MS)
    }

    override fun getPodcastSearchDebounceMs(): Long {
        return getRemoteConfigLong(FirebaseConfig.PODCAST_SEARCH_DEBOUNCE_MS)
    }

    override fun getEpisodeSearchDebounceMs(): Long {
        return getRemoteConfigLong(FirebaseConfig.EPISODE_SEARCH_DEBOUNCE_MS)
    }

    private fun getRemoteConfigLong(key: String): Long {
        val value = firebaseRemoteConfig.getLong(key)
        return if (value == 0L) (FirebaseConfig.defaults[key] as? Long ?: 0L) else value
    }

    override val upNextSwipe = UserSetting.PrefFromInt(
        sharedPrefKey = "up_next_action",
        defaultValue = Settings.UpNextAction.PLAY_NEXT,
        sharedPrefs = sharedPreferences,
        fromInt = { Settings.UpNextAction.values()[it] },
        toInt = { it.ordinal }
    )

    override val tapOnUpNextShouldPlay = UserSetting.BoolPref(
        sharedPrefKey = "tap_on_up_next_should_play",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override fun getHeadphoneControlsNextAction(): Settings.HeadphoneAction {
        return Settings.HeadphoneAction.values()[getInt("headphone_controls_next_action", Settings.HeadphoneAction.SKIP_FORWARD.ordinal)]
    }

    override fun setHeadphoneControlsNextAction(action: Settings.HeadphoneAction) {
        setInt("headphone_controls_next_action", action.ordinal)
        headphoneNextActionFlow.update { action }
    }

    override fun getHeadphoneControlsPreviousAction(): Settings.HeadphoneAction {
        return Settings.HeadphoneAction.values()[getInt("headphone_controls_previous_action", Settings.HeadphoneAction.SKIP_BACK.ordinal)]
    }

    override fun setHeadphoneControlsPreviousAction(action: Settings.HeadphoneAction) {
        setInt("headphone_controls_previous_action", action.ordinal)
        headphonePreviousActionFlow.update { action }
    }

    override fun getHeadphoneControlsPlayBookmarkConfirmationSound(): Boolean {
        return getBoolean("headphone_controls_play_bookmark_confirmation_sound", true)
    }

    override fun setHeadphoneControlsPlayBookmarkConfirmationSound(value: Boolean) {
        setBoolean("headphone_controls_play_bookmark_confirmation_sound", value)
        headphonePlayBookmarkConfirmationSoundFlow.update { value }
    }

    override val showArchivedDefault = UserSetting.BoolPref(
        sharedPrefKey = "default_show_archived",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val mediaControlItems = UserSetting.PrefListFromString<MediaNotificationControls>(
        sharedPrefKey = "media_notification_controls_action",
        sharedPrefs = sharedPreferences,
        defaultValue = MediaNotificationControls.All,
        fromString = { MediaNotificationControls.itemForId(it) },
        toString = { it.key }
    )

    override val podcastGroupingDefault = run {
        val default = PodcastGrouping.None
        UserSetting.PrefFromInt<PodcastGrouping>(
            sharedPrefKey = "default_podcast_grouping",
            defaultValue = default,
            sharedPrefs = sharedPreferences,
            fromInt = { PodcastGrouping.All.getOrNull(it) ?: default },
            toInt = { PodcastGrouping.All.indexOf(it) }
        )
    }

    override fun setCloudSortOrder(sortOrder: Settings.CloudSortOrder) {
        setInt("cloud_sort_order", sortOrder.ordinal)
    }

    override fun getCloudSortOrder(): Settings.CloudSortOrder {
        return Settings.CloudSortOrder.values().getOrNull(getInt("cloud_sort_order", 0)) ?: Settings.CloudSortOrder.NEWEST_OLDEST
    }

    override fun getCloudAddToUpNext(): Boolean {
        return getBoolean("cloudUpNext", false)
    }

    override fun setCloudAddToUpNext(value: Boolean) {
        setBoolean("cloudUpNext", value)
    }

    override fun getDeleteLocalFileAfterPlaying(): Boolean {
        return getBoolean("cloudDeleteAfterPlaying", false)
    }

    override fun setDeleteLocalFileAfterPlaying(value: Boolean) {
        setBoolean("cloudDeleteAfterPlaying", value)
    }

    override fun getDeleteCloudFileAfterPlaying(): Boolean {
        return getBoolean("cloudDeleteCloudAfterPlaying", false)
    }

    override fun setDeleteCloudFileAfterPlaying(value: Boolean) {
        setBoolean("cloudDeleteCloudAfterPlaying", value)
    }

    override fun getCloudAutoUpload(): Boolean {
        return getBoolean("cloudAutoUpload", false)
    }

    override fun setCloudAutoUpload(value: Boolean) {
        setBoolean("cloudAutoUpload", value)
    }

    override fun getCloudAutoDownload(): Boolean {
        return getBoolean("cloudAutoDownload", false)
    }

    override fun setCloudAutoDownload(value: Boolean) {
        setBoolean("cloudAutoDownload", value)
    }

    override fun getCloudOnlyWifi(): Boolean {
        return getBoolean("cloudOnlyWifi", true)
    }

    override fun setCloudOnlyWifi(value: Boolean) {
        setBoolean("cloudOnlyWifi", value)
    }

    override fun getAppIconId(): String? {
        return getString("appIconId", "Default")
    }

    override fun setAppIconId(value: String) {
        setString("appIconId", value)
    }

    override fun getUpgradeClosedProfile(): Boolean {
        return getBoolean("upgradeClosedProfile", false)
    }

    override fun setUpgradeClosedProfile(value: Boolean) {
        setBoolean("upgradeClosedProfile", value)
    }

    override fun getUpgradeClosedAddFile(): Boolean {
        return getBoolean("upgradeClosedAddFile", false)
    }

    override fun setUpgradeClosedAddFile(value: Boolean) {
        setBoolean("upgradeClosedAddFile", value)
    }

    override fun getUpgradeClosedCloudSettings(): Boolean {
        return getBoolean("upgradeClosedCloudSettings", false)
    }

    override fun setUpgradeClosedCloudSettings(value: Boolean) {
        setBoolean("upgradeClosedCloudSettings", value)
    }

    override fun getUpgradeClosedAppearSettings(): Boolean {
        return getBoolean("upgradeClosedAppearSettings", false)
    }

    override fun setUpgradeClosedAppearSettings(value: Boolean) {
        setBoolean("upgradeClosedAppearSettings", value)
    }

    override fun getWhatsNewVersionCode(): Int? {
        return getInt("WhatsNewVersionCode", 0)
    }

    override fun setWhatsNewVersionCode(value: Int) {
        setInt("WhatsNewVersionCode", value)
    }

    override fun setCachedSubscription(subscriptionStatus: SubscriptionStatus?) {
        if (subscriptionStatus is SubscriptionStatus.Paid) {
            val adapter = moshi.adapter(SubscriptionStatus.Paid::class.java)
            val str = adapter.toJson(subscriptionStatus)
            privatePreferences.edit().putString("accountstatus", encrypt(str)).apply()
        } else {
            privatePreferences.edit().putString("accountstatus", null).apply()
        }
    }

    override fun getCachedSubscription(): SubscriptionStatus? {
        val str = decrypt(privatePreferences.getString("accountstatus", null)) ?: return null
        try {
            val adapter = moshi.adapter(SubscriptionStatus.Paid::class.java)
            return adapter.fromJson(str)
        } catch (e: Exception) {
            return null
        }
    }

    override fun getShelfItems(): List<String> {
        return getStringList("shelfItems")
    }

    override fun setShelfItems(items: List<String>) {
        setStringList("shelfItems", items)
        shelfItemsObservable.accept(items)
    }

    override fun getMultiSelectItems(): List<Int> {
        return getStringList("multi_select_items").map { it.toInt() }
    }

    override fun setMultiSelectItems(items: List<Int>) {
        setStringList("multi_select_items", items.map { it.toString() })
        multiSelectItemsObservable.accept(items)
    }

    override fun getSeenPlayerTour(): Boolean {
        return getBoolean("player_tour_shown", false)
    }

    override fun setSeenPlayerTour(value: Boolean) {
        setBoolean("player_tour_shown", value)
    }

    override fun getSeenUpNextTour(): Boolean {
        return getBoolean("upnext_tour_shown", false)
    }

    override fun setSeenUpNextTour(value: Boolean) {
        setBoolean("upnext_tour_shown", value)
    }

    override val autoAddUpNextLimit = UserSetting.IntPref(
        sharedPrefKey = "auto_add_up_next_limit",
        defaultValue = DEFAULT_MAX_AUTO_ADD_LIMIT,
        sharedPrefs = sharedPreferences,
    )

    override val autoAddUpNextLimitBehaviour = run {
        val default = AutoAddUpNextLimitBehaviour.STOP_ADDING
        UserSetting.PrefFromInt<AutoAddUpNextLimitBehaviour>(
            sharedPrefKey = "auto_add_up_next_limit_reached",
            defaultValue = default,
            sharedPrefs = sharedPreferences,
            fromInt = {
                AutoAddUpNextLimitBehaviour.values().getOrNull(it)
                    ?: default
            },
            toInt = { it.ordinal }
        )
    }

    override fun getMaxUpNextEpisodes(): Int {
        return max(DEFAULT_MAX_AUTO_ADD_LIMIT, autoAddUpNextLimit.flow.value)
    }

    private fun setStringList(key: String, array: List<String>) {
        val str = array.joinToString(",")
        setString(key, str, true)
    }

    private fun getStringList(key: String): List<String> {
        val str = getString(key, "")!!
        return if (str.isEmpty()) {
            emptyList()
        } else {
            str.split(",")
        }
    }

    /**
     * Returns a unique device ID (stored once it's created once).
     */
    override fun getUniqueDeviceId(): String {
        // if we've saved on previously, return that
        val sharedPrefs = context.getSharedPreferences("Global", 0)
        val storedDeviceId = sharedPrefs.getString(DEVICE_ID_KEY, null)
        if (storedDeviceId != null)
            return storedDeviceId

        // otherwise create one
        val deviceId = UUID.randomUUID().toString()
        val editor = sharedPrefs.edit()
        editor.putString(DEVICE_ID_KEY, deviceId)
        editor.apply()
        return deviceId
    }

    override fun setHomeGridNeedsRefresh(value: Boolean) {
        setBoolean("home_grid_needs_refresh", value)
    }

    override fun getHomeGridNeedsRefresh(): Boolean {
        return getBoolean("home_grid_needs_refresh", false)
    }

    override fun setTimesToShowBatteryWarning(value: Int) {
        setInt(SHOWN_BATTERY_WARNING_KEY, max(0, value))
    }

    override fun getTimesToShowBatteryWarning(): Int =
        getInt(SHOWN_BATTERY_WARNING_KEY, 4)

    override fun setSendUsageStats(value: Boolean) {
        setBoolean(SEND_USAGE_STATS_KEY, value)
    }

    override fun getSendUsageStats(): Boolean =
        getBoolean(SEND_USAGE_STATS_KEY, true)

    override fun setSendCrashReports(value: Boolean) {
        setBoolean(SEND_CRASH_REPORTS_KEY, value)
    }

    override fun getSendCrashReports(): Boolean =
        getBoolean(SEND_CRASH_REPORTS_KEY, true)

    override fun setLinkCrashReportsToUser(value: Boolean) {
        setBoolean(LINK_CRASH_REPORTS_TO_USER_KEY, value)
    }

    override fun getLinkCrashReportsToUser(): Boolean =
        getBoolean(LINK_CRASH_REPORTS_TO_USER_KEY, false)

    override fun setEndOfYearShowBadge2022(value: Boolean) {
        setBoolean(END_OF_YEAR_SHOW_BADGE_2022_KEY, value)
    }

    override fun getEndOfYearShowBadge2022(): Boolean =
        getBoolean(END_OF_YEAR_SHOW_BADGE_2022_KEY, true)

    override fun setEndOfYearModalHasBeenShown(value: Boolean) {
        setBoolean(END_OF_YEAR_MODAL_HAS_BEEN_SHOWN_KEY, value)
    }

    override fun getEndOfYearModalHasBeenShown(): Boolean =
        getBoolean(END_OF_YEAR_MODAL_HAS_BEEN_SHOWN_KEY, false)

    override fun hasCompletedOnboarding() = getBoolean(DONE_INITIAL_ONBOARDING_KEY, false)

    override fun setHasDoneInitialOnboarding() {
        setBoolean(DONE_INITIAL_ONBOARDING_KEY, true)
    }

    override val customMediaActionsVisibility = UserSetting.BoolPref(
        sharedPrefKey = "CustomMediaActionsVisibleKey",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override fun isNotificationsDisabledMessageShown() =
        getBoolean(NOTIFICATIONS_DISABLED_MESSAGE_SHOWN, false)

    override fun setNotificationsDisabledMessageShown(value: Boolean) {
        setBoolean(NOTIFICATIONS_DISABLED_MESSAGE_SHOWN, value)
    }

    override fun setLastSelectedSubscriptionTier(tier: Subscription.SubscriptionTier) {
        setString(LAST_SELECTED_SUBSCRIPTION_TIER_KEY, tier.name)
    }

    override fun getLastSelectedSubscriptionTier() =
        getString(LAST_SELECTED_SUBSCRIPTION_TIER_KEY)?.let {
            Subscription.SubscriptionTier.valueOf(it)
        }

    override fun setLastSelectedSubscriptionFrequency(frequency: SubscriptionFrequency) {
        setString(LAST_SELECTED_SUBSCRIPTION_FREQUENCY_KEY, frequency.name)
    }

    override fun getLastSelectedSubscriptionFrequency() =
        getString(LAST_SELECTED_SUBSCRIPTION_FREQUENCY_KEY)?.let {
            SubscriptionFrequency.valueOf(it)
        }

    override fun setFullySignedOut(boolean: Boolean) {
        setBoolean(PROCESSED_SIGNOUT_KEY, boolean)
    }

    override fun getFullySignedOut(): Boolean =
        getBoolean(PROCESSED_SIGNOUT_KEY, true)

    override fun setlastLoadedFromPodcastOrFilterUuid(uuid: String?) {
        setString(LAST_SELECTED_PODCAST_OR_FILTER_UUID, uuid)
    }

    override fun getlastLoadedFromPodcastOrFilterUuid(): String? =
        getString(LAST_SELECTED_PODCAST_OR_FILTER_UUID)

    override fun setBookmarksSortType(sortType: BookmarksSortType) {
        setInt(Settings.PREFERENCE_BOOKMARKS_SORT, sortType.ordinal)
        bookmarkSortTypeFlow.update { sortType }
    }

    override fun getBookmarksSortType() =
        BookmarksSortType.values()[
            getInt(
                Settings.PREFERENCE_BOOKMARKS_SORT,
                BookmarksSortType.DATE_ADDED_NEWEST_TO_OLDEST.ordinal
            )
        ]

    override val theme = ThemeSetting.UserSettingPref(
        sharedPrefKey = "pocketCastsTheme",
        defaultValue = ThemeSetting.DARK,
        sharedPrefs = sharedPreferences,
    )

    override val darkThemePreference = ThemeSetting.UserSettingPref(
        sharedPrefKey = "PreferredDarkTheme",
        defaultValue = ThemeSetting.DARK,
        sharedPrefs = sharedPreferences,
    )

    override val lightThemePreference = ThemeSetting.UserSettingPref(
        sharedPrefKey = "PreferredLightTheme",
        defaultValue = ThemeSetting.LIGHT,
        sharedPrefs = sharedPreferences,
    )

    override val useSystemTheme = UserSetting.BoolPref(
        sharedPrefKey = "useSystemTheme",
        defaultValue = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q, // Only default on Android 10+
        sharedPrefs = sharedPreferences
    )

    override val appIcon = UserSetting.PrefFromString(
        sharedPrefKey = "pocketCastsAppIcon",
        defaultValue = AppIconSetting.DEFAULT,
        sharedPrefs = sharedPreferences,
        fromString = { str ->
            AppIconSetting.values().find { it.id == str }
                ?: AppIconSetting.DEFAULT
        },
        toString = { it.id }
    )
}
