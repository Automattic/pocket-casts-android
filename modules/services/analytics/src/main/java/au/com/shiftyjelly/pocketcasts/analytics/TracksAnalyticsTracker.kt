package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import au.com.shiftyjelly.pocketcasts.utils.DisplayUtil
import com.automattic.android.tracks.TracksClient
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

class TracksAnalyticsTracker @Inject constructor(
    @ApplicationContext appContext: Context,
    @PublicSharedPreferences preferences: SharedPreferences,
    private val displayUtil: DisplayUtil,
    private val settings: Settings,
) : Tracker(preferences) {
    private val tracksClient: TracksClient? = TracksClient.getClient(appContext)
    override val anonIdPrefKey: String = TRACKS_ANON_ID
    private val plusSubscription: SubscriptionStatus.Plus?
        get() = settings.getCachedSubscription() as? SubscriptionStatus.Plus

    private val predefinedEventProperties: Map<String, Any?>
        get() = mapOf(
            PredefinedEventProperty.HAS_DYNAMIC_FONT_SIZE.key to displayUtil.hasDynamicFontSize(),
            PredefinedEventProperty.PLUS_HAS_SUBSCRIPTION.key to (plusSubscription != null),
            PredefinedEventProperty.PLUS_HAS_LIFETIME.key to plusSubscription?.isLifetimePlus,
            PredefinedEventProperty.PLUS_SUBSCRIPTION_TYPE.key to plusSubscription?.type?.toString(),
            PredefinedEventProperty.PLUS_SUBSCRIPTION_PLATFORM.key to plusSubscription?.platform?.toString(),
            PredefinedEventProperty.PLUS_SUBSCRIPTION_FREQUENCY.key to plusSubscription?.frequency?.toString(),
        )

    override fun track(event: AnalyticsEvent, properties: Map<String, *>) {
        super.track(event, properties)
        if (tracksClient == null) return

        val eventKey = event.key
        val user = anonID ?: generateNewAnonID()
        val userType = TracksClient.NosaraUserType.ANON

        /* Create the merged JSON Object of properties.
        Properties defined by the user have precedence over the default ones pre-defined at "event level" */
        val propertiesToJSON = JSONObject(properties)
        predefinedEventProperties.keys.forEach { key ->
            if (propertiesToJSON.has(key)) {
                Timber.w("The user has defined a property named: '$key' that will override the same property pre-defined at event level. This may generate unexpected behavior!!")
                Timber.w("User value: " + propertiesToJSON.get(key).toString() + " - pre-defined value: " + predefinedEventProperties[key].toString())
            } else {
                propertiesToJSON.put(key, predefinedEventProperties[key])
            }
        }

        tracksClient.track(EVENTS_PREFIX + eventKey, propertiesToJSON, user, userType)
        if (propertiesToJSON.length() > 0) {
            Timber.i("\uD83D\uDD35 Tracked: $eventKey, Properties: $propertiesToJSON")
        } else {
            Timber.i("\uD83D\uDD35 Tracked: $eventKey")
        }
    }

    override fun flush() {
        tracksClient?.flush()
    }

    override fun clearAllData() {
        super.clearAllData()
        tracksClient?.clearUserProperties()
        tracksClient?.clearQueues()
    }

    enum class PredefinedEventProperty(val key: String) {
        HAS_DYNAMIC_FONT_SIZE("has_dynamic_font_size"),
        PLUS_HAS_SUBSCRIPTION("plus_has_subscription"),
        PLUS_HAS_LIFETIME("plus_has_lifetime"),
        PLUS_SUBSCRIPTION_TYPE("plus_subscription_type"),
        PLUS_SUBSCRIPTION_PLATFORM("plus_subscription_platform"),
        PLUS_SUBSCRIPTION_FREQUENCY("plus_subscription_frequency"),
    }

    companion object {
        private const val TRACKS_ANON_ID = "nosara_tracks_anon_id"
        private const val EVENTS_PREFIX = "pcandroid_"
    }
}
