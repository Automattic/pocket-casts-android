package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.DisplayUtil
import au.com.shiftyjelly.pocketcasts.utils.Util
import com.automattic.android.tracks.TracksClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class TracksAnalyticsTracker @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @PublicSharedPreferences preferences: SharedPreferences,
    private val displayUtil: DisplayUtil,
    private val settings: Settings,
    private val accountStatusInfo: AccountStatusInfo,
) : IdentifyingTracker(preferences),
    CoroutineScope {
    private val tracksClient: TracksClient? = TracksClient.getClient(appContext)
    override val anonIdPrefKey: String = TRACKS_ANON_ID
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    private var predefinedEventProperties = emptyMap<String, Any>()

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
        if (tracksClient == null) return

        launch {
            val eventKey = event.key
            val user = userId ?: anonID ?: generateNewAnonID()
            val userType = userId?.let {
                TracksClient.NosaraUserType.POCKETCASTS
            } ?: TracksClient.NosaraUserType.ANON

            /* Create the merged JSON Object of properties.
            Properties defined by the user have precedence over the default ones pre-defined at "event level" */
            val propertiesToJSON = JSONObject(properties)
            predefinedEventProperties.forEach { (key, value) ->
                if (propertiesToJSON.has(key)) {
                    Timber.w("The user has defined a property named: '$key' that will override the same property pre-defined at event level. This may generate unexpected behavior!!")
                    Timber.w("User value: ${propertiesToJSON.get(key)} - pre-defined value: $value")
                } else {
                    propertiesToJSON.put(key, value)
                }
            }

            tracksClient.track(EVENTS_PREFIX + eventKey, propertiesToJSON, user, userType)
            if (propertiesToJSON.length() > 0) {
                Timber.tag("Tracks").i("\uD83D\uDD35 Tracked: $eventKey, Properties: $propertiesToJSON")
            } else {
                Timber.tag("Tracks").i("\uD83D\uDD35 Tracked: $eventKey")
            }
        }
    }

    private fun updatePredefinedEventProperties() {
        val subscription = settings.cachedSubscription.value
        val isLoggedIn = accountStatusInfo.isLoggedIn()
        val hasSubscription = subscription != null
        val isPocketCastsChampion = subscription?.isChampion == true
        val subscriptionTier = subscription?.tier?.analyticsValue ?: INVALID_OR_NULL_VALUE
        val subscriptionPlatform = subscription?.platform?.analyticsValue ?: INVALID_OR_NULL_VALUE
        val subscriptionFrequency = subscription?.billingCycle?.analyticsValue ?: INVALID_OR_NULL_VALUE

        predefinedEventProperties = mapOf(
            PredefinedEventProperty.HAS_DYNAMIC_FONT_SIZE to displayUtil.hasDynamicFontSize(),
            PredefinedEventProperty.USER_IS_LOGGED_IN to isLoggedIn,
            PredefinedEventProperty.PLUS_HAS_SUBSCRIPTION to hasSubscription,
            PredefinedEventProperty.PLUS_HAS_LIFETIME to isPocketCastsChampion,
            PredefinedEventProperty.PLUS_SUBSCRIPTION_TIER to subscriptionTier,
            PredefinedEventProperty.PLUS_SUBSCRIPTION_PLATFORM to subscriptionPlatform,
            PredefinedEventProperty.PLUS_SUBSCRIPTION_FREQUENCY to subscriptionFrequency,
            PredefinedEventProperty.THEME_SELECTED to settings.theme.value.analyticsValue,
            PredefinedEventProperty.THEME_DARK_PREFERENCE to settings.darkThemePreference.value.analyticsValue,
            PredefinedEventProperty.THEME_LIGHT_PREFERENCE to settings.lightThemePreference.value.analyticsValue,
            PredefinedEventProperty.THEME_USE_SYSTEM_SETTINGS to settings.useSystemTheme.value,
            PredefinedEventProperty.PLATFORM to when (Util.getAppPlatform(appContext)) {
                AppPlatform.Automotive -> "automotive"
                AppPlatform.Phone -> "phone"
                AppPlatform.WearOs -> "watch"
            },
        ).mapKeys { it.key.analyticsKey }
    }

    override fun refreshMetadata() {
        val uuid = accountStatusInfo.getUuid()
        if (!uuid.isNullOrEmpty()) {
            userId = uuid
            // Re-unify the user
            if (anonID != null) {
                tracksClient?.trackAliasUser(userId, anonID, TracksClient.NosaraUserType.POCKETCASTS)
                clearAnonID()
            }
        } else {
            userId = null
            if (anonID == null) {
                generateNewAnonID()
            }
        }

        updatePredefinedEventProperties()
    }

    override fun flush() {
        tracksClient?.flush()
    }

    override fun clearAllData() {
        super.clearAllData()
        tracksClient?.clearUserProperties()
        tracksClient?.clearQueues()
    }

    enum class PredefinedEventProperty(val analyticsKey: String) {
        HAS_DYNAMIC_FONT_SIZE("has_dynamic_font_size"),
        USER_IS_LOGGED_IN("user_is_logged_in"),
        PLUS_HAS_SUBSCRIPTION("plus_has_subscription"),
        PLUS_HAS_LIFETIME("plus_has_lifetime"),
        PLUS_SUBSCRIPTION_TIER("plus_subscription_tier"),
        PLUS_SUBSCRIPTION_PLATFORM("plus_subscription_platform"),
        PLUS_SUBSCRIPTION_FREQUENCY("plus_subscription_frequency"),
        PLATFORM("platform"),
        THEME_SELECTED("theme_selected"),
        THEME_DARK_PREFERENCE("theme_dark_preference"),
        THEME_LIGHT_PREFERENCE("theme_light_preference"),
        THEME_USE_SYSTEM_SETTINGS("theme_use_system_settings"),
    }

    companion object {
        private const val TRACKS_ANON_ID = "nosara_tracks_anon_id"
        private const val EVENTS_PREFIX = "pcandroid_"
        const val INVALID_OR_NULL_VALUE = "none"
    }
}
