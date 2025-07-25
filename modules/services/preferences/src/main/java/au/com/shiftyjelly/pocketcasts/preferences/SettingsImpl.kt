package au.com.shiftyjelly.pocketcasts.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import androidx.core.content.edit
import androidx.work.NetworkType
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.DEFAULT_MAX_AUTO_ADD_LIMIT
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.GLOBAL_AUTO_DOWNLOAD_NONE
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.NOTIFICATIONS_DISABLED_MESSAGE_SHOWN
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.SETTINGS_ENCRYPT_SECRET
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.preferences.di.PrivateSharedPreferences
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import au.com.shiftyjelly.pocketcasts.preferences.model.AppIconSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoAddUpNextLimitBehaviour
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeDefault
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForPodcast
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForProfile
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneActionUserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ThemeSetting
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.config.FirebaseConfig
import au.com.shiftyjelly.pocketcasts.utils.extensions.getString
import au.com.shiftyjelly.pocketcasts.utils.extensions.splitIgnoreEmpty
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.Charset
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec
import javax.inject.Inject
import kotlin.math.max
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

class SettingsImpl @Inject constructor(
    @PublicSharedPreferences private val sharedPreferences: SharedPreferences,
    @PrivateSharedPreferences private val privatePreferences: SharedPreferences,
    @ApplicationContext private val context: Context,
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val moshi: Moshi,
) : Settings {

    companion object {
        private const val DEVICE_ID_KEY = "DeviceIdKey"
        private const val SHOWN_BATTERY_WARNING_KEY = "ShownBetteryWarningKey"
        private const val END_OF_YEAR_SHOW_BADGE_2023_KEY = "EndOfYearShowBadge2023Key"
        private const val END_OF_YEAR_SHOW_MODAL_2023_KEY = "EndOfYearModalShowModal2023Key"
        private const val DONE_INITIAL_ONBOARDING_KEY = "CompletedOnboardingKey"
        private const val PROCESSED_SIGNOUT_KEY = "ProcessedSignout"
    }

    private var languageCode: String? = null

    override val selectPodcastSortTypeObservable = BehaviorRelay.create<PodcastsSortType>().apply { accept(getSelectPodcastsSortType()) }
    override val multiSelectItemsObservable = BehaviorRelay.create<List<String>>().apply { accept(getMultiSelectItems()) }

    override val shelfItems = UserSetting.PrefFromString(
        sharedPrefKey = "shelfItems",
        defaultValue = ShelfItem.entries.toList(),
        sharedPrefs = sharedPreferences,
        fromString = { itemIdsString ->
            val decodedItems = itemIdsString.split(',').mapNotNull(ShelfItem::fromId)
            val missingItems = ShelfItem.entries - decodedItems
            if (missingItems.contains(ShelfItem.Transcript)) {
                // Add new item Transcript to the list of items at 4th position
                return@PrefFromString decodedItems.toMutableList()
                    .also { it.add(3, ShelfItem.Transcript) } + (missingItems - ShelfItem.Transcript)
            }
            decodedItems + missingItems
        },
        toString = { items ->
            val allItems = items.distinct() + (ShelfItem.entries - items)
            allItems.joinToString(separator = ",", transform = ShelfItem::id)
        },
    )

    override val upNextShuffle: UserSetting<Boolean> = UserSetting.BoolPref(
        sharedPrefKey = "upNextShuffle",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val refreshStateFlow = MutableStateFlow<RefreshState>(
        run {
            val lastError = getLastRefreshError()
            val refreshDate = getLastRefreshDate()
            when {
                lastError != null -> RefreshState.Failed(lastError)
                refreshDate != null -> RefreshState.Success(refreshDate)
                else -> RefreshState.Never
            }
        },
    )

    override fun getVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
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

    override val marketingOptIn = UserSetting.BoolPref(
        sharedPrefKey = "marketingOptIn",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val freeGiftAcknowledged = UserSetting.BoolPref(
        sharedPrefKey = "freeGiftAck",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override fun getCancelledAcknowledged(): Boolean {
        return getBoolean("cancelled_acknowledged", false)
    }

    override fun setCancelledAcknowledged(value: Boolean) {
        setBoolean("cancelled_acknowledged", value)
    }

    override fun syncOnMeteredNetwork(): Boolean {
        return getBoolean(Settings.PREFERENCE_SYNC_ON_METERED, true)
    }

    override fun setSyncOnMeteredNetwork(shouldSyncOnMetered: Boolean) {
        setBoolean(Settings.PREFERENCE_SYNC_ON_METERED, shouldSyncOnMetered)
    }

    override fun getWorkManagerNetworkTypeConstraint(): NetworkType = if (syncOnMeteredNetwork()) {
        NetworkType.CONNECTED
    } else {
        NetworkType.UNMETERED
    }

    override fun refreshPodcastsOnResume(isUnmetered: Boolean): Boolean = syncOnMeteredNetwork() || isUnmetered

    override val backgroundRefreshPodcasts = UserSetting.BoolPref(
        sharedPrefKey = "backgroundRefresh",
        defaultValue = !Util.isWearOs(context),
        sharedPrefs = sharedPreferences,
    )

    override val podcastsSortType = UserSetting.PrefFromString(
        sharedPrefKey = "podcastLibrarySort",
        defaultValue = PodcastsSortType.default,
        sharedPrefs = sharedPreferences,
        fromString = { PodcastsSortType.fromClientIdString(it) },
        toString = { it.clientId.toString() },
    )

    override val prioritizeSeekAccuracy = UserSetting.BoolPref(
        sharedPrefKey = "prioritizeSeekAccuracy",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val cacheEntirePlayingEpisode = UserSetting.BoolPref(
        sharedPrefKey = "cacheEntirePlayingEpisode",
        defaultValue = firebaseRemoteConfig.getBoolean(FirebaseConfig.EXOPLAYER_CACHE_ENTIRE_PLAYING_EPISODE_SETTING_DEFAULT),
        sharedPrefs = sharedPreferences,
    )

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
            toString = { it.intValue.toString() },
        )
    }

    override val notificationSound = UserSetting.PrefFromString(
        sharedPrefKey = "notificationRingtone",
        defaultValue = NotificationSound(context = context),
        sharedPrefs = sharedPreferences,
        fromString = { NotificationSound(it, context) },
        toString = { it.path },
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
        refreshStateFlow.value = refreshState
    }

    override fun setDismissLowStorageModalTime(lastUpdateTime: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(Settings.LAST_DISMISS_LOW_STORAGE_MODAL_TIME, lastUpdateTime)
        editor.apply()
    }

    override fun shouldShowLowStorageModalAfterSnooze(): Boolean {
        val lastSnoozeTime = sharedPreferences.getLong(Settings.LAST_DISMISS_LOW_STORAGE_MODAL_TIME, 0)

        if (lastSnoozeTime == 0L) return true

        val timeSinceDismiss = (System.currentTimeMillis() - lastSnoozeTime).milliseconds

        return timeSinceDismiss >= 7.days
    }

    override fun setDismissLowStorageBannerTime(lastUpdateTime: Long) {
        sharedPreferences.edit {
            putLong(Settings.LAST_DISMISS_LOW_STORAGE_BANNER_TIME, lastUpdateTime)
        }
    }

    override fun shouldShowLowStorageBannerAfterSnooze(): Boolean {
        val lastSnoozeTime = sharedPreferences.getLong(Settings.LAST_DISMISS_LOW_STORAGE_BANNER_TIME, 0)

        if (lastSnoozeTime == 0L) return true

        val timeSinceDismiss = (System.currentTimeMillis() - lastSnoozeTime).milliseconds

        return timeSinceDismiss >= 14.days
    }

    override fun getRefreshState(): RefreshState? {
        return refreshStateFlow.value
    }

    override fun getLastSuccessRefreshState(): RefreshState? {
        return getLastRefreshDate()?.let { RefreshState.Success(it) }
    }

    private fun getLastRefreshError(): String? {
        return getString("last_refresh_error", null)
    }

    private fun setLastRefreshError(error: String?) {
        setString("last_refresh_error", error, true)
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

    override fun getStringForKey(key: String, isPrivate: Boolean): String? {
        return if (isPrivate) {
            decrypt(privatePreferences.getString(key))
        } else {
            sharedPreferences.getString(key)
        }
    }

    override fun deleteKey(key: String, isPrivate: Boolean) {
        val preferences = if (isPrivate) privatePreferences else sharedPreferences
        preferences.edit { remove(key) }
    }

    override val discoverCountryCode = UserSetting.StringPref(
        sharedPrefKey = "discovery_country_code",
        defaultValue = getDefaultCountryCode(),
        sharedPrefs = sharedPreferences,
    )

    override val warnOnMeteredNetwork = UserSetting.BoolPref(
        sharedPrefKey = Settings.PREFERENCE_WARN_WHEN_NOT_ON_WIFI,
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val autoSubscribeToPlayed = UserSetting.BoolPref(
        sharedPrefKey = "autoSubscribeToPlayed",
        defaultValue = Util.isAutomotive(context),
        sharedPrefs = sharedPreferences,
    )

    override val autoShowPlayed = UserSetting.BoolPref(
        sharedPrefKey = "autoShowPlayed",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

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
        toString = { it.preferenceInt.toString() },
    )

    override val autoDownloadLimit = UserSetting.PrefFromString(
        sharedPrefKey = "autoDownloadLimit",
        defaultValue = AutoDownloadLimitSetting.TWO_LATEST_EPISODE,
        sharedPrefs = sharedPreferences,
        fromString = { AutoDownloadLimitSetting.fromPreferenceString(it) ?: AutoDownloadLimitSetting.TWO_LATEST_EPISODE },
        toString = { it.id.toString() },
    )

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
                Cipher.ENCRYPT_MODE,
                key,
                PBEParameterSpec(
                    android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.System.ANDROID_ID,
                    ).toByteArray(charset("utf-8")),
                    20,
                ),
            )
            return String(
                Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP),
                Charset.forName("utf-8"),
            )
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun contains(key: String, isPrivate: Boolean): Boolean {
        val preferences = if (isPrivate) privatePreferences else sharedPreferences
        return preferences.contains(key)
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
        deleteCloudFileAfterPlaying.set(false, updateModifiedAt = false)
        cloudAutoUpload.set(false, updateModifiedAt = false)
        cloudAutoDownload.set(false, updateModifiedAt = false)
        cloudDownloadOnlyOnWifi.set(false, updateModifiedAt = false)
        setCancelledAcknowledged(false)
    }

    private fun getDefaultCountryCode(): String {
        val languageCode = languageCode
        if (languageCode != null) {
            return languageCode
        }

        val locale: Locale = context.resources.configuration.locales[0]
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
        sharedPrefs = sharedPreferences,
    )

    override val dailyRemindersNotification = UserSetting.BoolPref(
        sharedPrefKey = "dailyRemindersNotification",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val recommendationsNotification = UserSetting.BoolPref(
        sharedPrefKey = "trendingAndRecommendationsNotification",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val newFeaturesNotification = UserSetting.BoolPref(
        sharedPrefKey = "newFeaturesAndTipsNotification",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val offersNotification = UserSetting.BoolPref(
        sharedPrefKey = "offersNotification",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val notificationsPromptAcknowledged = UserSetting.BoolPref(
        sharedPrefKey = "notificationPromptAcknowledged",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
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

    override val autoDownloadUnmeteredOnly = UserSetting.BoolPref(
        sharedPrefKey = "autoDownloadOnlyDownloadOnWifi",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val autoDownloadUpNext = UserSetting.BoolPref(
        sharedPrefKey = "autoDownloadUpNext",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val autoDownloadOnFollowPodcast = UserSetting.BoolPref(
        sharedPrefKey = "autoDownloadOnFollowPodcast",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val autoDownloadNewEpisodes = UserSetting.IntPref(
        sharedPrefKey = "globalAutoDownloadNewEpisodes",
        defaultValue = GLOBAL_AUTO_DOWNLOAD_NONE,
        sharedPrefs = sharedPreferences,
    )

    override val autoDownloadOnlyWhenCharging = UserSetting.BoolPref(
        sharedPrefKey = "autoDownloadOnlyDownloadWhenCharging",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val artworkConfiguration = object : UserSetting<ArtworkConfiguration>(
        sharedPrefKey = "artworkConfiguration",
        sharedPrefs = sharedPreferences,
    ) {
        override fun get(): ArtworkConfiguration {
            return sharedPreferences.getString(sharedPrefKey)?.split(",")?.let { stringValues ->
                val isEnabled = stringValues.getOrNull(0)?.toBooleanStrictOrNull() ?: return@let null
                val elements = stringValues.drop(1).mapNotNullTo(mutableSetOf(), ArtworkConfiguration.Element::fromKey)
                ArtworkConfiguration(isEnabled, elements)
            } ?: ArtworkConfiguration(false)
        }

        override fun persist(value: ArtworkConfiguration, commit: Boolean) {
            val stringValue = buildList {
                add(value.useEpisodeArtwork.toString())
                addAll(value.enabledElements.map(ArtworkConfiguration.Element::key))
            }.joinToString(",")
            sharedPrefs.edit(commit) {
                putString(sharedPrefKey, stringValue)
            }
        }
    }

    override val globalPlaybackEffects = object : UserSetting<PlaybackEffects>(
        sharedPrefKey = "globalPlaybackEffects",
        sharedPrefs = sharedPreferences,
    ) {
        override fun get(): PlaybackEffects = PlaybackEffects().apply {
            playbackSpeed = globalPlaybackSpeed.value
            trimMode = globalAudioEffectRemoveSilence.value
            isVolumeBoosted = globalAudioEffectVolumeBoost.value
        }

        override fun persist(value: PlaybackEffects, commit: Boolean) {
            globalPlaybackSpeed.set(value.playbackSpeed, updateModifiedAt = false)
            globalAudioEffectRemoveSilence.set(value.trimMode, updateModifiedAt = false)
            globalAudioEffectVolumeBoost.set(value.isVolumeBoosted, updateModifiedAt = false)
        }
    }

    private val globalPlaybackSpeed = UserSetting.PrefFromFloat(
        sharedPrefKey = "globalPlaybackSpeed",
        defaultValue = 1.0,
        sharedPrefs = sharedPreferences,
        fromFloat = { it.toDouble() },
        toFloat = { it.toFloat() },
    )

    private val globalAudioEffectRemoveSilence = run {
        val oldDefault = sharedPreferences.getBoolean("globalAudioEffectRemoveSilence", false)
        val default = if (oldDefault) TrimMode.LOW else TrimMode.OFF
        UserSetting.PrefFromInt(
            sharedPrefKey = "globalTrimMode",
            defaultValue = default,
            sharedPrefs = sharedPreferences,
            fromInt = { TrimMode.values().getOrNull(it) ?: default },
            toInt = { it.ordinal },
        )
    }

    private val globalAudioEffectVolumeBoost = UserSetting.BoolPref(
        sharedPrefKey = "globalAudioEffectVolumeBoost",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override fun getMigratedVersionCode(): Int {
        return getInt("MIGRATED_VERSION_CODE", 0)
    }

    override fun setMigratedVersionCode(versionCode: Int) {
        setInt("MIGRATED_VERSION_CODE", versionCode)
    }

    override val podcastBadgeType = UserSetting.PrefFromInt<BadgeType>(
        sharedPrefKey = "PODCAST_BADGE_TYPE",
        defaultValue = BadgeType.defaultValue,
        sharedPrefs = sharedPreferences,
        fromInt = { BadgeType.fromPersistedInt(it) },
        toInt = { it.persistedInt },
    )

    override val podcastGridLayout = UserSetting.PrefFromInt<PodcastGridLayoutType>(
        sharedPrefKey = "PODCAST_GRID_LAYOUT",
        defaultValue = PodcastGridLayoutType.default,
        sharedPrefs = sharedPreferences,
        fromInt = { PodcastGridLayoutType.fromLayoutId(it) },
        toInt = { it.id },
    )

    override fun getNotificationLastSeen(): Date? {
        return getDate("NOTIFICATION_LAST_SEEN")
    }

    override fun setNotificationLastSeen(lastSeen: Date?) {
        setDate("NOTIFICATION_LAST_SEEN", lastSeen)
    }

    override fun setNotificationLastSeenToNow() {
        setNotificationLastSeen(Date())
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

    override fun setSleepEndOfEpisodes(episodes: Int) {
        setInt("sleepEndOfEpisodes", episodes)
    }

    override fun setlastSleepEndOfEpisodes(episodes: Int) {
        setInt("lastSleepEndOfEpisodes", episodes)
    }

    override fun setlastSleepEndOfChapters(chapters: Int) {
        setInt("lastSleepEndOfChapters", chapters)
    }

    override fun setSleepEndOfChapters(chapters: Int) {
        setInt("sleepEndOfChapters", chapters)
    }

    override fun getSleepTimerCustomMins(): Int {
        return getInt("sleepTimerCustomMins", 5)
    }

    override fun getSleepEndOfEpisodes(): Int {
        return getInt("sleepEndOfEpisodes", 1)
    }

    override fun getSleepEndOfChapters(): Int {
        return getInt("sleepEndOfChapters", 1)
    }

    override fun getlastSleepEndOfEpisodes(): Int {
        return getInt("lastSleepEndOfEpisodes", 0)
    }

    override fun getlastSleepEndOfChapter(): Int {
        return getInt("lastSleepEndOfChapters", 0)
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

    override val newEpisodeNotificationActions = UserSetting.PrefFromString(
        sharedPrefKey = "notification_actions",
        defaultValue = NewEpisodeNotificationAction.DefaultValues,
        sharedPrefs = sharedPreferences,
        fromString = { actionIdsString ->
            when (actionIdsString) {
                "" -> NewEpisodeNotificationAction.DefaultValues
                else -> actionIdsString.splitIgnoreEmpty(",").mapNotNull { actionIdString ->
                    NewEpisodeNotificationAction.entries.find { action ->
                        action.id.toString() == actionIdString
                    }
                }
            }
        },
        toString = { actions ->
            when (actions) {
                NewEpisodeNotificationAction.DefaultValues -> ""
                else -> actions.joinToString(separator = ",") { it.id.toString() }
            }
        },
    )

    override val autoPlayNextEpisodeOnEmpty = UserSetting.BoolPref(
        sharedPrefKey = "autoUpNextEmpty",
        defaultValue = when (Util.getAppPlatform(context)) {
            AppPlatform.Automotive -> true
            AppPlatform.Phone,
            AppPlatform.WearOs,
            -> false
        },
        sharedPrefs = sharedPreferences,
    )

    override val autoArchiveIncludesStarred = UserSetting.BoolPref(
        sharedPrefKey = Settings.AUTO_ARCHIVE_INCLUDE_STARRED,
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val autoArchiveAfterPlaying = UserSetting.PrefFromInt(
        sharedPrefKey = "autoArchivePlayedEpisodesIndex",
        defaultValue = getString("autoArchivePlayedEpisodes")?.let {
            // Use the old String setting if it exists before falling back to the default value
            AutoArchiveAfterPlaying.fromString(it, context)
        } ?: AutoArchiveAfterPlaying.defaultValue(context),
        sharedPrefs = sharedPreferences,
        fromInt = { AutoArchiveAfterPlaying.fromIndex(it) ?: AutoArchiveAfterPlaying.defaultValue(context) },
        toInt = { it.index },
    )

    override val autoArchiveInactive = UserSetting.PrefFromInt(
        sharedPrefKey = "autoArchiveInactiveIndex",
        defaultValue = getString("autoArchiveInactiveEpisodes")?.let {
            // Use the old String setting if it exists before falling back to the default value
            AutoArchiveInactive.fromString(it, context)
        } ?: AutoArchiveInactive.Default,
        sharedPrefs = sharedPreferences,
        fromInt = { AutoArchiveInactive.fromIndex(it) ?: AutoArchiveInactive.Default },
        toInt = { it.index },
    )

    override fun getCustomStorageLimitGb(): Long {
        return getRemoteConfigLong(FirebaseConfig.CLOUD_STORAGE_LIMIT)
    }

    override fun getPeriodicSaveTimeMs(): Long {
        return getRemoteConfigLong(FirebaseConfig.PERIODIC_SAVE_TIME_MS)
    }

    override fun getPlayerReleaseTimeOutMs(): Long {
        return getRemoteConfigLong(FirebaseConfig.PLAYER_RELEASE_TIME_OUT_MS)
    }

    override fun getPodcastSearchDebounceMs(): Long {
        return getRemoteConfigLong(FirebaseConfig.PODCAST_SEARCH_DEBOUNCE_MS)
    }

    override fun getEpisodeSearchDebounceMs(): Long {
        return getRemoteConfigLong(FirebaseConfig.EPISODE_SEARCH_DEBOUNCE_MS)
    }

    override fun getSlumberStudiosPromoCode(): String {
        return firebaseRemoteConfig.getString(FirebaseConfig.SLUMBER_STUDIOS_YEARLY_PROMO_CODE)
    }

    override fun getSleepTimerDeviceShakeThreshold(): Long {
        return getRemoteConfigLong(FirebaseConfig.SLEEP_TIMER_DEVICE_SHAKE_THRESHOLD)
    }

    override fun getRefreshPodcastsBatchSize(): Long {
        return getRemoteConfigLong(FirebaseConfig.REFRESH_PODCASTS_BATCH_SIZE)
    }

    override fun getExoPlayerCacheSizeInMB(): Long {
        return firebaseRemoteConfig.getLong(FirebaseConfig.EXOPLAYER_CACHE_SIZE_IN_MB)
    }

    override fun getExoPlayerCacheEntirePlayingEpisodeSizeInMB(): Long {
        return firebaseRemoteConfig.getLong(FirebaseConfig.EXOPLAYER_CACHE_ENTIRE_PLAYING_EPISODE_SIZE_IN_MB)
    }

    override fun getPlaybackEpisodePositionChangedOnSyncThresholdSecs(): Long {
        return firebaseRemoteConfig.getLong(FirebaseConfig.PLAYBACK_EPISODE_POSITION_CHANGED_ON_SYNC_THRESHOLD_SECS)
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
        toInt = { it.ordinal },
    )

    override val tapOnUpNextShouldPlay = UserSetting.BoolPref(
        sharedPrefKey = "tap_on_up_next_should_play",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    private val subscriptionAdapter = moshi.adapter(Subscription::class.java)

    override val cachedSubscription = UserSetting.PrefFromString<Subscription?>(
        sharedPrefKey = "user_subscription",
        defaultValue = null,
        sharedPrefs = privatePreferences,
        fromString = { value ->
            value
                .takeIf(String::isNotEmpty)
                ?.let(::decrypt)
                ?.let { decryptedValue -> runCatching { subscriptionAdapter.fromJson(decryptedValue) }.getOrNull() }
        },
        toString = { value ->
            value
                ?.let(subscriptionAdapter::toJson)
                ?.let(::encrypt)
                .orEmpty()
        },
    )

    override val headphoneControlsNextAction = HeadphoneActionUserSetting(
        sharedPrefKey = "headphone_controls_next_action",
        defaultAction = HeadphoneAction.SKIP_FORWARD,
        sharedPrefs = sharedPreferences,
        subscriptionFlow = cachedSubscription.flow,
    )

    override val headphoneControlsPreviousAction = HeadphoneActionUserSetting(
        sharedPrefKey = "headphone_controls_previous_action",
        defaultAction = HeadphoneAction.SKIP_BACK,
        sharedPrefs = sharedPreferences,
        subscriptionFlow = cachedSubscription.flow,
    )

    override val headphoneControlsPlayBookmarkConfirmationSound = UserSetting.BoolPref(
        sharedPrefKey = "headphone_controls_play_bookmark_confirmation_sound",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val showArchivedDefault = UserSetting.BoolPref(
        sharedPrefKey = "default_show_archived",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val shakeToResetSleepTimer = UserSetting.BoolPref(
        sharedPrefKey = "shake_to_reset_sleep_timer",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val autoSleepTimerRestart = UserSetting.BoolPref(
        sharedPrefKey = "auto_sleep_timer_restart",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val mediaControlItems = UserSetting.PrefListFromString<MediaNotificationControls>(
        sharedPrefKey = "media_notification_controls_action",
        sharedPrefs = sharedPreferences,
        defaultValue = MediaNotificationControls.All,
        fromString = { MediaNotificationControls.itemForId(it) },
        toString = { it.key },
    )

    override val podcastGroupingDefault = run {
        val default = PodcastGrouping.None
        UserSetting.PrefFromInt<PodcastGrouping>(
            sharedPrefKey = "default_podcast_grouping",
            defaultValue = default,
            sharedPrefs = sharedPreferences,
            fromInt = { PodcastGrouping.fromIndex(it) ?: default },
            toInt = { it.index },
        )
    }

    override val cloudSortOrder = UserSetting.PrefFromInt(
        sharedPrefKey = "cloud_sort_order",
        defaultValue = Settings.CloudSortOrder.NEWEST_OLDEST,
        sharedPrefs = sharedPreferences,
        fromInt = { ordinal -> Settings.CloudSortOrder.entries.find { it.ordinal == ordinal } ?: Settings.CloudSortOrder.NEWEST_OLDEST },
        toInt = Settings.CloudSortOrder::ordinal,
    )

    override val cloudAddToUpNext = UserSetting.BoolPref(
        sharedPrefKey = "cloudUpNext",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val deleteLocalFileAfterPlaying = UserSetting.BoolPref(
        sharedPrefKey = "deleteLocalFileAfterPlaying",
        // Use value stored under previous key if it exists
        defaultValue = getBoolean("cloudDeleteAfterPlaying", false),
        sharedPrefs = sharedPreferences,
    )

    override val deleteCloudFileAfterPlaying = UserSetting.BoolPref(
        sharedPrefKey = "deleteCloudFileAfterPlaying",
        // Use value stored under previous key if it exists
        defaultValue = sharedPreferences.getBoolean("cloudDeleteCloudAfterPlaying", false),
        sharedPrefs = sharedPreferences,

    )

    override val cloudAutoUpload = UserSetting.BoolPref(
        sharedPrefKey = "cloudAutoUpload",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val cloudAutoDownload = UserSetting.BoolPref(
        sharedPrefKey = "cloudAutoDownload",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val cloudDownloadOnlyOnWifi = UserSetting.BoolPref(
        sharedPrefKey = "cloudOnlyWifi",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val upgradeProfileClosed = UserSetting.BoolPref(
        sharedPrefKey = "upgradeClosedProfile",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

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

    override fun getMultiSelectItems(): List<String> {
        return getStringList("multi_select_items")
    }

    override fun setMultiSelectItems(items: List<String>) {
        setStringList("multi_select_items", items)
        multiSelectItemsObservable.accept(items)
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
            toInt = { it.ordinal },
        )
    }

    override fun getMaxUpNextEpisodes(): Int {
        return max(DEFAULT_MAX_AUTO_ADD_LIMIT, autoAddUpNextLimit.value)
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
        if (storedDeviceId != null) {
            return storedDeviceId
        }

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

    override fun getTimesToShowBatteryWarning(): Int = getInt(SHOWN_BATTERY_WARNING_KEY, 4)

    override val collectAnalytics = UserSetting.BoolPref(
        sharedPrefKey = "SendUsageStatsKey",
        defaultValue = BuildConfig.DATA_COLLECTION_DEFAULT_VALUE ?: true,
        sharedPrefs = sharedPreferences,
    )

    override val collectAnalyticsThirdParty = UserSetting.BoolPref(
        sharedPrefKey = "SendUsageStatsThirdPartyKey",
        defaultValue = false, // Ask for consent before sending third party analytics
        sharedPrefs = sharedPreferences,
    )

    override val sendCrashReports = UserSetting.BoolPref(
        sharedPrefKey = "SendCrashReportsKey",
        defaultValue = BuildConfig.DATA_COLLECTION_DEFAULT_VALUE ?: true,
        sharedPrefs = sharedPreferences,
    )

    override val linkCrashReportsToUser = UserSetting.BoolPref(
        sharedPrefKey = "LinkCrashReportsToUserKey",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override fun setEndOfYearShowBadge2023(value: Boolean) {
        setBoolean(END_OF_YEAR_SHOW_BADGE_2023_KEY, value)
    }

    override fun getEndOfYearShowBadge2023(): Boolean = getBoolean(END_OF_YEAR_SHOW_BADGE_2023_KEY, true)

    override fun setEndOfYearShowModal(value: Boolean) {
        setBoolean(END_OF_YEAR_SHOW_MODAL_2023_KEY, value)
    }

    override fun getEndOfYearShowModal(): Boolean = getBoolean(END_OF_YEAR_SHOW_MODAL_2023_KEY, true)

    override fun hasCompletedOnboarding() = getBoolean(DONE_INITIAL_ONBOARDING_KEY, false)

    override fun setHasDoneInitialOnboarding() {
        setBoolean(DONE_INITIAL_ONBOARDING_KEY, true)
    }

    override val customMediaActionsVisibility = UserSetting.BoolPref(
        sharedPrefKey = "CustomMediaActionsVisibleKey",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val nextPreviousTrackSkipButtons = UserSetting.BoolPref(
        sharedPrefKey = "NextPreviousTrackSkipButtonsKey",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override fun isNotificationsDisabledMessageShown() = getBoolean(NOTIFICATIONS_DISABLED_MESSAGE_SHOWN, false)

    override fun setNotificationsDisabledMessageShown(value: Boolean) {
        setBoolean(NOTIFICATIONS_DISABLED_MESSAGE_SHOWN, value)
    }

    override fun setFullySignedOut(boolean: Boolean) {
        setBoolean(PROCESSED_SIGNOUT_KEY, boolean)
    }

    override fun getFullySignedOut(): Boolean = getBoolean(PROCESSED_SIGNOUT_KEY, true)

    override val lastAutoPlaySource = UserSetting.PrefFromString(
        sharedPrefKey = "LastSelectedPodcastOrFilterUuid", // legacy name
        defaultValue = AutoPlaySource.Predefined.None,
        sharedPrefs = sharedPreferences,
        fromString = AutoPlaySource::fromId,
        toString = AutoPlaySource::id,
    )

    override val trackingAutoPlaySource = UserSetting.PrefFromString(
        sharedPrefKey = "localAutoPlaySource",
        defaultValue = AutoPlaySource.Predefined.None,
        sharedPrefs = sharedPreferences,
        fromString = AutoPlaySource::fromId,
        toString = AutoPlaySource::id,
    )

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
        sharedPrefs = sharedPreferences,
    )

    override val appIcon = UserSetting.PrefFromString(
        sharedPrefKey = "pocketCastsAppIcon",
        defaultValue = AppIconSetting.DEFAULT,
        sharedPrefs = sharedPreferences,
        fromString = { str ->
            AppIconSetting.values().find { it.id == str }
                ?: AppIconSetting.DEFAULT
        },
        toString = { it.id },
    )

    override val episodeBookmarksSortType = BookmarksSortTypeDefault.UserSettingPref(
        sharedPrefKey = Settings.PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_EPISODE,
        defaultValue = BookmarksSortTypeDefault.DATE_ADDED_NEWEST_TO_OLDEST,
        sharedPrefs = sharedPreferences,
    )

    override val playerBookmarksSortType = BookmarksSortTypeDefault.UserSettingPref(
        sharedPrefKey = Settings.PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_PLAYER,
        defaultValue = BookmarksSortTypeDefault.DATE_ADDED_NEWEST_TO_OLDEST,
        sharedPrefs = sharedPreferences,
    )

    override val podcastBookmarksSortType = BookmarksSortTypeForPodcast.UserSettingPref(
        sharedPrefKey = Settings.PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_PODCAST,
        defaultValue = BookmarksSortTypeForPodcast.DATE_ADDED_NEWEST_TO_OLDEST,
        sharedPrefs = sharedPreferences,
    )

    override val profileBookmarksSortType = BookmarksSortTypeForProfile.UserSettingPref(
        sharedPrefKey = Settings.PREFERENCE_BOOKMARKS_SORT_TYPE_FOR_PROFILE,
        defaultValue = BookmarksSortTypeForProfile.DATE_ADDED_NEWEST_TO_OLDEST,
        sharedPrefs = sharedPreferences,
    )

    override fun addReviewRequestedDate() {
        val dates = getReviewRequestedDates().toMutableList()
        dates.add(Date().toString())
        sharedPreferences.edit().putStringSet(Settings.APP_REVIEW_REQUESTED_DATES, dates.toSet()).apply()
    }

    override fun getReviewRequestedDates(): List<String> {
        return sharedPreferences.getStringSet(Settings.APP_REVIEW_REQUESTED_DATES, emptySet())?.toList() ?: emptyList()
    }

    override val useDarkUpNextTheme: UserSetting<Boolean> = UserSetting.BoolPref(
        sharedPrefKey = "useDarkUpNextTheme",
        defaultValue = true, // This default is overridden for new installs
        sharedPrefs = sharedPreferences,
    )

    override val useDynamicColorsForWidget: UserSetting<Boolean> = object : UserSetting<Boolean>(
        sharedPrefKey = "useDynamicColorsForWidget",
        sharedPrefs = sharedPreferences,
    ) {
        override fun get(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sharedPrefs.getBoolean(sharedPrefKey, true)
            } else {
                false
            }
        }

        override fun persist(value: Boolean, commit: Boolean) {
            sharedPrefs.edit(commit) {
                putBoolean(sharedPrefKey, value)
            }
        }
    }

    private val _themeReconfigurationEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)
    override val themeReconfigurationEvents: Flow<Unit>
        get() = _themeReconfigurationEvents

    override fun requestThemeReconfiguration() {
        _themeReconfigurationEvents.tryEmit(Unit)
    }

    private val _bottomInset = MutableStateFlow(0)
    override val bottomInset: Flow<Int>
        get() = _bottomInset.asStateFlow()

    override fun updateBottomInset(height: Int) {
        _bottomInset.tryEmit(height)
    }

    override fun automotiveConnectedToMediaSession(): Boolean {
        return sharedPreferences.getBoolean(Settings.AUTOMOTIVE_CONNECTED_TO_MEDIA_SESSION, false)
    }

    override fun setAutomotiveConnectedToMediaSession(isLoaded: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(Settings.AUTOMOTIVE_CONNECTED_TO_MEDIA_SESSION, isLoaded)
        editor.apply()
    }

    override val showReferralsTooltip: UserSetting<Boolean> = UserSetting.BoolPref(
        sharedPrefKey = Settings.SHOW_REFERRALS_TOOLTIP,
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val referralClaimCode = UserSetting.StringPref(
        sharedPrefKey = "referralCode",
        defaultValue = "",
        sharedPrefs = sharedPreferences,
    )

    override val showReferralWelcome = UserSetting.BoolPref(
        sharedPrefKey = "showReferralWelcome",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    private val _playerOrUpNextBottomSheetState = MutableSharedFlow<Int>(onBufferOverflow = BufferOverflow.DROP_OLDEST, replay = 1)
    override val playerOrUpNextBottomSheetState: Flow<Int>
        get() = _playerOrUpNextBottomSheetState.asSharedFlow().distinctUntilChanged()

    override fun updatePlayerOrUpNextBottomSheetState(state: Int) {
        _playerOrUpNextBottomSheetState.tryEmit(state)
    }

    override val lastEoySyncTimestamp = UserSetting.PrefFromString<Instant>(
        sharedPrefKey = "eoy_sync_timestamp",
        defaultValue = Instant.EPOCH,
        sharedPrefs = sharedPreferences,
        fromString = { value -> runCatching { Instant.parse(value) }.getOrDefault(Instant.EPOCH) },
        toString = { value -> value.toString() },
    )

    override val useRealTimeForPlaybackRemaingTime = UserSetting.BoolPref(
        sharedPrefKey = "use_real_time_for_playback_remaining_time",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val showEmptyFiltersListTooltip: UserSetting<Boolean> = UserSetting.BoolPref(
        sharedPrefKey = "show_empty_filters_list_tooltip",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val showPodcastsRecentlyPlayedSortOrderTooltip: UserSetting<Boolean> = UserSetting.BoolPref(
        sharedPrefKey = "show_podcasts_recently_played_sort_order_tooltip",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val suggestedFoldersDismissTimestamp = UserSetting.PrefFromString<Instant?>(
        sharedPrefKey = "suggested_folders_dismiss_timestamp",
        defaultValue = null,
        fromString = { value -> runCatching { Instant.parse(value) }.getOrNull() },
        toString = { value -> value.toString() },
        sharedPrefs = sharedPreferences,
    )

    override val suggestedFoldersDismissCount = UserSetting.IntPref(
        sharedPrefKey = "suggested_folders_dismiss_count",
        defaultValue = 0,
        sharedPrefs = sharedPreferences,
    )

    override val suggestedFoldersFollowedHash = UserSetting.StringPref(
        sharedPrefKey = "suggested_folders_followed_hash",
        defaultValue = "",
        sharedPrefs = sharedPreferences,
    )

    override val isTrackingConsentRequired = UserSetting.BoolPref(
        sharedPrefKey = "tracking_consent_required",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val isFreeAccountProfileBannerDismissed = UserSetting.BoolPref(
        sharedPrefKey = "free_account_banner_dismissed_profile",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val isFreeAccountFiltersBannerDismissed = UserSetting.BoolPref(
        sharedPrefKey = "free_account_banner_dismissed_filters",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val isFreeAccountHistoryBannerDismissed = UserSetting.BoolPref(
        sharedPrefKey = "free_account_banner_dismissed_history",
        defaultValue = false,
        sharedPrefs = sharedPreferences,
    )

    override val showFreeAccountEncouragement = UserSetting.BoolPref(
        sharedPrefKey = "show_free_account_encouragement",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )

    override val showPlaylistsOnboarding = UserSetting.BoolPref(
        sharedPrefKey = "show_playlists_onboarding",
        defaultValue = true,
        sharedPrefs = sharedPreferences,
    )
}
