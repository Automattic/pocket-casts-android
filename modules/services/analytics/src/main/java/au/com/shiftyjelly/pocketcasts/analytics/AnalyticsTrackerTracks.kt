package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import androidx.preference.PreferenceManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker.PREFKEY_SEND_USAGE_STATS
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker.sendUsageStats
import com.automattic.android.tracks.TracksClient
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject

class AnalyticsTrackerTracks @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : Tracker(appContext) {
    private val tracksClient: TracksClient? = TracksClient.getClient(appContext)
    override val anonIdPrefKey: String
        get() = TRACKS_ANON_ID

    override fun track(event: AnalyticsEvent, properties: Map<String, *>) {
        if (tracksClient == null) return

        val eventName = event.name.lowercase(Locale.getDefault())
        val user = anonID ?: generateNewAnonID()
        val userType = TracksClient.NosaraUserType.ANON

        val propertiesJson = JSONObject(properties)
        tracksClient.track(EVENTS_PREFIX + eventName, propertiesJson, user, userType)
    }

    override fun flush() {
        tracksClient?.flush()
    }

    override fun clearAllData() {
        super.clearAllData()
        tracksClient?.clearUserProperties()
        tracksClient?.clearQueues()
    }

    override fun storeUsagePref() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        prefs.edit().putBoolean(PREFKEY_SEND_USAGE_STATS, sendUsageStats).apply()
    }

    companion object {
        private const val TRACKS_ANON_ID = "nosara_tracks_anon_id"
        private const val EVENTS_PREFIX = "pcandroid_"
    }
}
