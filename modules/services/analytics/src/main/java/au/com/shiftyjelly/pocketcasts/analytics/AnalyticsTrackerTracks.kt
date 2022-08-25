package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import androidx.preference.PreferenceManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker.PREFKEY_SEND_USAGE_STATS
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker.sendUsageStats
import com.automattic.android.tracks.TracksClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AnalyticsTrackerTracks @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : Tracker {
    private val tracksClient: TracksClient? = TracksClient.getClient(appContext)
    override fun track(event: AnalyticsEvent, properties: Map<String, *>?) {
        TODO("Not yet implemented")
    }

    override fun flush() {
        tracksClient?.flush()
    }

    override fun clearAllData() {
        tracksClient?.clearUserProperties()
        tracksClient?.clearQueues()
    }

    override fun storeUsagePref() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        prefs.edit().putBoolean(PREFKEY_SEND_USAGE_STATS, sendUsageStats).apply()
    }
}
