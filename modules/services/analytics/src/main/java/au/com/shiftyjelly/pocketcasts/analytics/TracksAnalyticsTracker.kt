package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import au.com.shiftyjelly.pocketcasts.utils.DisplayUtil
import com.automattic.android.tracks.TracksClient
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class TracksAnalyticsTracker @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val displayUtil: DisplayUtil,
) : Tracker(appContext) {
    private val tracksClient: TracksClient? = TracksClient.getClient(appContext)
    override val anonIdPrefKey: String = TRACKS_ANON_ID

    private val predefinedEventProperties: Map<String, Any?>
        get() = mapOf(AnalyticsEvent.HAS_DYNAMIC_FONT_SIZE.toName() to displayUtil.hasDynamicFontSize())

    override fun track(event: AnalyticsEvent, properties: Map<String, *>) {
        if (tracksClient == null) return

        val eventName = event.toName()
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

        tracksClient.track(EVENTS_PREFIX + eventName, propertiesToJSON, user, userType)
        if (propertiesToJSON.length() > 0) {
            Timber.i("\uD83D\uDD35 Tracked: $eventName, Properties: $propertiesToJSON")
        } else {
            Timber.i("\uD83D\uDD35 Tracked: $eventName")
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

    companion object {
        private const val TRACKS_ANON_ID = "nosara_tracks_anon_id"
        private const val EVENTS_PREFIX = "pcandroid_"
        private fun AnalyticsEvent.toName() = name.lowercase(Locale.getDefault())
    }
}
