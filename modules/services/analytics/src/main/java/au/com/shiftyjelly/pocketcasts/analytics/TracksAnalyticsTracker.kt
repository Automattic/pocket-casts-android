package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
    private val displayUtil: DisplayUtil,
    private val settings: Settings,
    private val accountStatusInfo: AccountStatusInfo,
) : Tracker,
    CoroutineScope {
    private val tracksClient: TracksClient? = TracksClient.getClient(appContext)

    private var predefinedEventProperties = emptyMap<String, Any>()

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    override val id get() = ID

    override fun shouldTrack(event: AnalyticsEvent): Boolean {
        return tracksClient != null && settings.collectAnalytics.value
    }

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>): TrackedEvent {
        if (tracksClient != null) {
            launch {
                val eventKey = event.key
                val userIds = accountStatusInfo.getUserIds()
                val userType = if (userIds.accountId != null) {
                    TracksClient.NosaraUserType.POCKETCASTS
                } else {
                    TracksClient.NosaraUserType.ANON
                }

                // Create the merged JSON Object of properties.
                // Properties defined by the user have precedence over the default ones pre-defined at "event level"
                val propertiesToJSON = JSONObject(properties)
                predefinedEventProperties.forEach { (key, value) ->
                    if (propertiesToJSON.has(key)) {
                        Timber.w("The user has defined a property named: '$key' that will override the same property pre-defined at event level. This may generate unexpected behavior!!")
                        Timber.w("User value: ${propertiesToJSON.get(key)} - pre-defined value: $value")
                    } else {
                        propertiesToJSON.put(key, value)
                    }
                }

                tracksClient.track(EVENTS_PREFIX + eventKey, propertiesToJSON, userIds.id, userType)
            }
        }

        val usedProperties = buildMap {
            putAll(properties)
            predefinedEventProperties.forEach { (key, value) ->
                putIfAbsent(key, value)
            }
        }
        return TrackedEvent(event, usedProperties)
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

    private var lastAliasedAnonId: String? = null

    override fun refreshMetadata() {
        val userIds = accountStatusInfo.getUserIds()
        if (!userIds.accountId.isNullOrEmpty()) {
            if (lastAliasedAnonId != userIds.anonId) {
                lastAliasedAnonId = userIds.anonId
                tracksClient?.trackAliasUser(userIds.accountId, userIds.anonId, TracksClient.NosaraUserType.POCKETCASTS)
            }
        }
        updatePredefinedEventProperties()
    }

    override fun flush() {
        tracksClient?.flush()
    }

    override fun clearAllData() {
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
        private const val ID = "Tracks"
        private const val EVENTS_PREFIX = "pcandroid_"
        const val INVALID_OR_NULL_VALUE = "none"
    }
}
