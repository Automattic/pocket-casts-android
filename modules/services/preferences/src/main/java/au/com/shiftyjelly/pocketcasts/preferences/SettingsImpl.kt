package au.com.shiftyjelly.pocketcasts.preferences

import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.DEFAULT_MAX_AUTO_ADD_LIMIT
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.NOTIFICATIONS_DISABLED_MESSAGE_SHOWN
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.SETTINGS_ENCRYPT_SECRET
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.preferences.Settings.NotificationChannel
import au.com.shiftyjelly.pocketcasts.preferences.Settings.NotificationId
import au.com.shiftyjelly.pocketcasts.preferences.di.PrivateSharedPreferences
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import au.com.shiftyjelly.pocketcasts.utils.extensions.isScreenReaderOn
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class SettingsImpl @Inject constructor(
    @PublicSharedPreferences private val sharedPreferences: SharedPreferences,
    @PrivateSharedPreferences private val privatePreferences: SharedPreferences,
    @ApplicationContext private val context: Context,
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
        private const val CUSTOM_MEDIA_ACTIONS_VISIBLE_KEY = "CustomMediaActionsVisibleKey"
    }

    private var languageCode: String? = null
    private var lastSignInErrorNotification: Long? = null

    private val firebaseRemoteConfig: FirebaseRemoteConfig by lazy { setupFirebaseConfig() }

    override val isLoggedInObservable = BehaviorRelay.create<Boolean>().apply { accept(isLoggedIn()) }
    override val podcastLayoutObservable = BehaviorRelay.create<Int>().apply { accept(getPodcastsLayout()) }
    override val skipForwardInSecsObservable = BehaviorRelay.create<Int>().apply { accept(getSkipForwardInSecs()) }
    override val skipBackwardInSecsObservable = BehaviorRelay.create<Int>().apply { accept(getSkipBackwardInSecs()) }
    override val podcastBadgeTypeObservable = BehaviorRelay.create<Settings.BadgeType>().apply { accept(getPodcastBadgeType()) }
    override val podcastSortTypeObservable = BehaviorRelay.create<PodcastsSortType>().apply { accept(getPodcastsSortType()) }
    override val selectPodcastSortTypeObservable = BehaviorRelay.create<PodcastsSortType>().apply { accept(getSelectPodcastsSortType()) }
    override val playbackEffectsObservable = BehaviorRelay.create<PlaybackEffects>().apply { accept(getGlobalPlaybackEffects()) }
    override val upNextSwipeActionObservable = BehaviorRelay.create<Settings.UpNextAction>().apply { accept(getUpNextSwipeAction()) }
    override val rowActionObservable = BehaviorRelay.create<Boolean>().apply { accept(streamingMode()) }
    override val marketingOptObservable = BehaviorRelay.create<Boolean>().apply { accept(getMarketingOptIn()) }
    override val isFirstSyncRunObservable = BehaviorRelay.create<Boolean>().apply { accept(isFirstSyncRun()) }
    override val shelfItemsObservable = BehaviorRelay.create<List<String>>().apply { accept(getShelfItems()) }
    override val multiSelectItemsObservable = BehaviorRelay.create<List<Int>>().apply { accept(getMultiSelectItems()) }
    override val autoAddUpNextLimitBehaviour = BehaviorRelay.create<Settings.AutoAddUpNextLimitBehaviour>().apply { accept(getAutoAddUpNextLimitBehaviour()) }
    override val autoAddUpNextLimit = BehaviorRelay.create<Int>().apply { accept(getAutoAddUpNextLimit()) }

    override val defaultPodcastGroupingFlow = MutableStateFlow(defaultPodcastGrouping())
    override val defaultMediaNotificationControlsFlow = MutableStateFlow(getMediaNotificationControlItems())
    override val defaultShowArchivedFlow = MutableStateFlow(defaultShowArchived())
    override val keepScreenAwakeFlow = MutableStateFlow(keepScreenAwake())
    override val openPlayerAutomaticallyFlow = MutableStateFlow(openPlayerAutomatically())
    override val intelligentPlaybackResumptionFlow = MutableStateFlow(getIntelligentPlaybackResumption())
    override val tapOnUpNextShouldPlayFlow = MutableStateFlow(getTapOnUpNextShouldPlay())
    override val customMediaActionsVisibilityFlow = MutableStateFlow(areCustomMediaActionsVisible())

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

    private fun setupFirebaseConfig(): FirebaseRemoteConfig {
        return FirebaseRemoteConfig.getInstance().apply {
            val config = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(2L * 60L * 60L)
                .build()
            setConfigSettingsAsync(config)
            setDefaultsAsync(FirebaseConfig.defaults)
            fetchAndActivate().addOnCompleteListener {
                if (!it.isSuccessful) {
                    Timber.e("Could not fetch remote config: ${it.exception?.message ?: "Unknown error"}")
                }
            }
        }
    }

    override fun isScreenReaderOn(): Boolean {
        return context.isScreenReaderOn()
    }

    override fun getSkipForwardInSecs(): Int {
        return getSkipAmountInSecs(key = Settings.PREFERENCE_SKIP_FORWARD, defaultValue = Settings.SKIP_FORWARD_DEFAULT)
    }

    private fun getSkipAmountInSecs(key: String, defaultValue: String): Int {
        val value = sharedPreferences.getString(key, defaultValue) ?: defaultValue
        return try {
            val valueInt = Integer.parseInt(value)
            if (valueInt <= 0) defaultValue.toInt() else valueInt
        } catch (nfe: NumberFormatException) {
            defaultValue.toInt()
        }
    }

    override fun getSkipForwardInMs(): Long {
        return getSkipForwardInSecs() * 1000L
    }

    override fun setSkipForwardInSec(value: Int) {
        setString(Settings.PREFERENCE_SKIP_FORWARD, if (value <= 0) Settings.SKIP_FORWARD_DEFAULT else value.toString())
        skipForwardInSecsObservable.accept(value)
    }

    override fun getSkipBackwardInSecs(): Int {
        return getSkipAmountInSecs(key = Settings.PREFERENCE_SKIP_BACKWARD, defaultValue = Settings.SKIP_BACKWARD_DEFAULT)
    }

    override fun getSkipBackwardInMs(): Long {
        return getSkipBackwardInSecs() * 1000L
    }

    override fun setSkipBackwardInSec(value: Int) {
        setString(Settings.PREFERENCE_SKIP_BACKWARD, if (value <= 0) Settings.SKIP_BACKWARD_DEFAULT else value.toString())
        skipBackwardInSecsObservable.accept(value)
    }

    override fun getSkipForwardNeedsSync(): Boolean {
        return getBoolean(Settings.PREFERENCE_SKIP_FORWARD_NEEDS_SYNC, false)
    }

    override fun setSkipForwardNeedsSync(value: Boolean) {
        setBoolean(Settings.PREFERENCE_SKIP_FORWARD_NEEDS_SYNC, value)
    }

    override fun getSkipBackNeedsSync(): Boolean {
        return getBoolean(Settings.PREFERENCE_SKIP_BACK_NEEDS_SYNC, false)
    }

    override fun setSkipBackNeedsSync(value: Boolean) {
        setBoolean(Settings.PREFERENCE_SKIP_BACK_NEEDS_SYNC, value)
    }

    override fun updateSkipValues() {
        skipBackwardInSecsObservable.accept(getSkipBackwardInSecs())
        skipForwardInSecsObservable.accept(getSkipForwardInSecs())
    }

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

    override fun refreshPodcastsAutomatically(): Boolean {
        return getBoolean("backgroundRefresh", true)
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

    override fun getNotificationVibrate(): Int {
        val value = sharedPreferences.getString(
            Settings.PREFERENCE_NOTIFICATION_VIBRATE,
            Settings.PREFERENCE_NOTIFICATION_VIBRATE_DEFAULT
        ) ?: Settings.PREFERENCE_NOTIFICATION_VIBRATE_DEFAULT
        return Integer.parseInt(value)
    }

    override fun getNotificationSound(): Uri? {
        val value = sharedPreferences.getString(Settings.PREFERENCE_NOTIFICATION_RINGTONE, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.path)
        return if (value.isNullOrBlank() || !isSoundOn()) null else Uri.parse(value)
    }

    override fun getNotificationSoundPath(): String {
        return getString(Settings.PREFERENCE_NOTIFICATION_RINGTONE, "DEFAULT_SOUND")
            ?: "DEFAULT_SOUND"
    }

    override fun setNotificationSoundPath(path: String) {
        setString(Settings.PREFERENCE_NOTIFICATION_RINGTONE, path)
    }

    override fun isSoundOn(): Boolean {
        return (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    override fun isNotificationVibrateOn(): Boolean {
        val vibrate = getNotificationVibrate()
        return vibrate == 2 || vibrate == 1 && !isSoundOn()
    }

    override fun oldNotifyRefreshPodcast(): Boolean {
        return sharedPreferences.getBoolean(
            Settings.OLD_PREFERENCE_EPISODE_NOTIFICATIONS_ON,
            Settings.PREFERENCE_EPISODE_NOTIFICATIONS_ON_DEFAULT
        )
    }

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

    override fun canDuckAudioWithNotifications(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_OVERRIDE_AUDIO, false)
    }

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

    override fun setSyncEmail(email: String) {
        val manager = AccountManager.get(context)
        val account = manager.pocketCastsAccount() ?: return
        manager.renameAccount(account, email, null, null)
        isLoggedInObservable.accept(isLoggedIn())
    }

    override fun setSyncPassword(password: String) {
        val manager = AccountManager.get(context)
        val account = manager.pocketCastsAccount() ?: return
        manager.setPassword(account, password)
        isLoggedInObservable.accept(isLoggedIn())
    }

    override fun getOldSyncDetails(): Pair<String?, String?> {
        val email = privatePreferences.getString("syncEmail", null)
        val password = decrypt(privatePreferences.getString("syncPassword", null))

        return Pair(email, password)
    }

    override fun clearEmailAndPassword() {
        val editor = privatePreferences.edit()
        editor.remove("syncEmail")
        editor.remove("syncPassword")
        editor.remove("syncToken")
        editor.remove("syncApiToken")
        editor.remove(Settings.PREFERENCE_FIRST_SYNC_RUN)
        editor.apply()
        isLoggedInObservable.accept(false)
    }

    override fun getSyncEmail(): String? {
        val manager = AccountManager.get(context)
        return manager.pocketCastsAccount()?.name
    }

    override fun getSyncPassword(): String? {
        val manager = AccountManager.get(context)
        val account = manager.pocketCastsAccount() ?: return null
        return manager.getPassword(account)
    }

    override fun getSyncUuid(): String? {
        val manager = AccountManager.get(context)
        val account = manager.pocketCastsAccount() ?: return null
        return manager.getUserData(account, AccountConstants.UUID)
    }

    private fun peekToken(): String? {
        val manager = AccountManager.get(context)
        val account = manager.pocketCastsAccount() ?: return null
        return manager.peekAuthToken(account, AccountConstants.TOKEN_TYPE)
    }

    override fun getSyncRefreshToken(): String? {
        return peekToken()
    }

    override fun getSyncToken(onTokenErrorUiShown: () -> Unit): String? = runBlocking {
        getSyncTokenSuspend(onTokenErrorUiShown)
    }

    override suspend fun getSyncTokenSuspend(onTokenErrorUiShown: () -> Unit): String? {
        val manager = AccountManager.get(context)
        val account = manager.pocketCastsAccount() ?: return null

        return withContext(Dispatchers.IO) {
            try {
                val resultFuture: AccountManagerFuture<Bundle> = manager.getAuthToken(
                    account,
                    AccountConstants.TOKEN_TYPE,
                    Bundle(),
                    false,
                    null,
                    null
                )
                val bundle: Bundle = resultFuture.result // This call will block until the result is available.
                val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                // Token failed to refresh
                if (token == null) {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        bundle.getParcelable(AccountManager.KEY_INTENT, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        bundle.getParcelable(AccountManager.KEY_INTENT) as? Intent
                    }
                    intent?.let { showSignInErrorNotification(it, onTokenErrorUiShown) }
                    throw SecurityException("Token could not be refreshed")
                } else {
                    token
                }
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Could not get token")
                throw e // Rethrow the exception so it carries on
            }
        }
    }

    private fun showSignInErrorNotification(intent: Intent, onTokenErrorUiShown: () -> Unit) {
        onShowSignInErrorNotificationDebounced(onTokenErrorUiShown)

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
        val notification = NotificationCompat.Builder(context, NotificationChannel.NOTIFICATION_CHANNEL_ID_SIGN_IN_ERROR.id)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(context.getString(LR.string.token_refresh_sign_in_error_title))
            .setContentText(context.getString(LR.string.token_refresh_sign_in_error_description))
            .setAutoCancel(true)
            .setSmallIcon(IR.drawable.ic_failedwarning)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context)
            .notify(NotificationId.SIGN_IN_ERROR.value, notification)
    }

    // Avoid invoking the passed function multiple times in a short period of time
    @Synchronized
    private fun onShowSignInErrorNotificationDebounced(onTokenErrorUiShown: () -> Unit) {
        val now = System.currentTimeMillis()
        // Do not invoke this method more than once every 2 seconds
        val shouldInvoke = lastSignInErrorNotification == null ||
            lastSignInErrorNotification!! < now - (2 * 1000)
        if (shouldInvoke) {
            onTokenErrorUiShown()
        }
        lastSignInErrorNotification = now
    }

    override fun invalidateToken() {
        val manager = AccountManager.get(context)
        val account = manager.pocketCastsAccount() ?: return
        val token = manager.peekAuthToken(account, AccountConstants.TOKEN_TYPE)
        manager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, token)
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

    override fun isLoggedIn(): Boolean {
        return getSyncEmail() != null && getSyncPassword() != null
    }

    override fun getUsedAccountManager(): Boolean {
        return getBoolean("accountmanager", false)
    }

    override fun setUsedAccountManager(value: Boolean) {
        setBoolean("accountmanager", value)
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

    override fun hideNotificationOnPause(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_HIDE_NOTIFICATION_ON_PAUSE, false)
    }

    override fun streamingMode(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_GLOBAL_STREAMING_MODE, true)
    }

    override fun setStreamingMode(newValue: Boolean) {
        setBoolean(Settings.PREFERENCE_GLOBAL_STREAMING_MODE, newValue)
        rowActionObservable.accept(newValue)
    }

    override fun keepScreenAwake(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_KEEP_SCREEN_AWAKE, false)
    }

    override fun setKeepScreenAwake(newValue: Boolean) {
        setBoolean(Settings.PREFERENCE_KEEP_SCREEN_AWAKE, newValue)
        keepScreenAwakeFlow.update { newValue }
    }

    override fun openPlayerAutomatically(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_OPEN_PLAYER_AUTOMATICALLY, false)
    }

    override fun setOpenPlayerAutomatically(newValue: Boolean) {
        setBoolean(Settings.PREFERENCE_OPEN_PLAYER_AUTOMATICALLY, newValue)
        openPlayerAutomaticallyFlow.update { newValue }
    }

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

    override fun getUseEmbeddedArtwork(): Boolean {
        return sharedPreferences.getBoolean(Settings.PREFERENCE_USE_EMBEDDED_ARTWORK, false)
    }

    override fun setUseEmbeddedArtwork(value: Boolean) {
        setBoolean(Settings.PREFERENCE_USE_EMBEDDED_ARTWORK, value)
    }

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

    override fun showArtworkOnLockScreen(): Boolean {
        return getBoolean("showArtworkOnLockScreen", true)
    }

    override fun setShowArtworkOnLockScreen(show: Boolean) {
        setBoolean("showArtworkOnLockScreen", show)
    }

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

    override fun getIntelligentPlaybackResumption(): Boolean {
        return getBoolean(Settings.INTELLIGENT_PLAYBACK_RESUMPTION, true)
    }

    override fun setIntelligentPlaybackResumption(value: Boolean) {
        setBoolean(Settings.INTELLIGENT_PLAYBACK_RESUMPTION, value)
        intelligentPlaybackResumptionFlow.update { value }
    }

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

    override fun getNewEpisodeNotificationActions(): String? {
        return sharedPreferences.getString("notification_actions", null)
    }

    override fun setNewEpisodeNotificationActions(actions: String) {
        sharedPreferences.edit {
            putString("notification_actions", actions)
        }
    }

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

    override fun getAutoPlayNextEpisodeOnEmpty(): Boolean {
        return getBoolean(Settings.PREFERENCE_AUTO_PLAY_ON_EMPTY, false)
    }

    override fun getAutoArchiveIncludeStarred(): Boolean {
        return getBoolean(Settings.AUTO_ARCHIVE_INCLUDE_STARRED, false)
    }

    override fun getAutoArchiveAfterPlaying(): Settings.AutoArchiveAfterPlaying {
        val value = getString(Settings.AUTO_ARCHIVE_PLAYED_EPISODES_AFTER, null)
        return Settings.AutoArchiveAfterPlaying.fromString(context, value)
    }

    override fun getAutoArchiveInactive(): Settings.AutoArchiveInactive {
        val value = getString(Settings.AUTO_ARCHIVE_INACTIVE, null)
        return Settings.AutoArchiveInactive.fromString(context, value)
    }

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

    override fun isFeatureFlagSearchImprovementsEnabled(): Boolean {
        return firebaseRemoteConfig.getBoolean(FirebaseConfig.FEATURE_FLAG_SEARCH_IMPROVEMENTS)
    }

    private fun getRemoteConfigLong(key: String): Long {
        val value = firebaseRemoteConfig.getLong(key)
        return if (value == 0L) (FirebaseConfig.defaults[key] as? Long ?: 0L) else value
    }

    override fun getUpNextSwipeAction(): Settings.UpNextAction {
        return Settings.UpNextAction.values()[getInt("up_next_action", Settings.UpNextAction.PLAY_NEXT.ordinal)]
    }

    override fun setUpNextSwipeAction(action: Settings.UpNextAction) {
        setInt("up_next_action", action.ordinal)
        upNextSwipeActionObservable.accept(action)
    }

    override fun getTapOnUpNextShouldPlay(): Boolean {
        return getBoolean("tap_on_up_next_should_play", false)
    }

    override fun setTapOnUpNextShouldPlay(value: Boolean) {
        setBoolean("tap_on_up_next_should_play", value)
        tapOnUpNextShouldPlayFlow.update { value }
    }

    override fun defaultShowArchived(): Boolean {
        return getBoolean("default_show_archived", false)
    }

    override fun setDefaultShowArchived(value: Boolean) {
        setBoolean("default_show_archived", value)
        defaultShowArchivedFlow.update { value }
    }

    override fun getMediaNotificationControlItems(): List<MediaNotificationControls> {
        var items = getStringList("media_notification_controls_action")

        if (items.isEmpty())
            items = MediaNotificationControls.All.map { it.key }
        return items.mapNotNull { MediaNotificationControls.itemForId(it) }
    }

    override fun setMediaNotificationControlItems(items: List<String>) {
        setStringList("media_notification_controls_action", items)
        defaultMediaNotificationControlsFlow.update { items.mapNotNull { MediaNotificationControls.itemForId(it) } }
    }

    override fun defaultPodcastGrouping(): PodcastGrouping {
        val index = getInt("default_podcast_grouping", 0)
        return PodcastGrouping.All.getOrNull(index) ?: PodcastGrouping.None
    }

    override fun setDefaultPodcastGrouping(podcastGrouping: PodcastGrouping) {
        setInt("default_podcast_grouping", PodcastGrouping.All.indexOf(podcastGrouping))
        defaultPodcastGroupingFlow.update { podcastGrouping }
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
        if (subscriptionStatus is SubscriptionStatus.Plus) {
            val adapter = moshi.adapter(SubscriptionStatus.Plus::class.java)
            val str = adapter.toJson(subscriptionStatus)
            privatePreferences.edit().putString("accountstatus", encrypt(str)).apply()
        } else {
            privatePreferences.edit().putString("accountstatus", null).apply()
        }
    }

    override fun getCachedSubscription(): SubscriptionStatus? {
        val str = decrypt(privatePreferences.getString("accountstatus", null)) ?: return null
        try {
            val adapter = moshi.adapter(SubscriptionStatus.Plus::class.java)
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

    override fun getAutoAddUpNextLimit(): Int {
        return getInt("auto_add_up_next_limit", DEFAULT_MAX_AUTO_ADD_LIMIT)
    }

    override fun setAutoAddUpNextLimit(limit: Int) {
        setInt("auto_add_up_next_limit", limit)
        autoAddUpNextLimit.accept(limit)
    }

    override fun setAutoAddUpNextLimitBehaviour(value: Settings.AutoAddUpNextLimitBehaviour) {
        setInt("auto_add_up_next_limit_reached", value.ordinal)
        autoAddUpNextLimitBehaviour.accept(value)
    }

    override fun getAutoAddUpNextLimitBehaviour(): Settings.AutoAddUpNextLimitBehaviour {
        val index = getInt("auto_add_up_next_limit_reached", 0)
        return Settings.AutoAddUpNextLimitBehaviour.values().getOrNull(index) ?: Settings.AutoAddUpNextLimitBehaviour.STOP_ADDING
    }

    override fun getMaxUpNextEpisodes(): Int {
        return max(DEFAULT_MAX_AUTO_ADD_LIMIT, getAutoAddUpNextLimit())
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

    override fun areCustomMediaActionsVisible() =
        getBoolean(CUSTOM_MEDIA_ACTIONS_VISIBLE_KEY, true)

    override fun setCustomMediaActionsVisible(value: Boolean) {
        setBoolean(CUSTOM_MEDIA_ACTIONS_VISIBLE_KEY, value)
        customMediaActionsVisibilityFlow.update { value }
    }

    override fun isNotificationsDisabledMessageShown() =
        getBoolean(NOTIFICATIONS_DISABLED_MESSAGE_SHOWN, false)

    override fun setNotificationsDisabledMessageShown(value: Boolean) {
        setBoolean(NOTIFICATIONS_DISABLED_MESSAGE_SHOWN, value)
    }
}
