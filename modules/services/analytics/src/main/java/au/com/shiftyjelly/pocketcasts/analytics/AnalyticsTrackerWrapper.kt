package au.com.shiftyjelly.pocketcasts.analytics

import javax.inject.Inject

class AnalyticsTrackerWrapper @Inject constructor() {

    fun track(event: AnalyticsEvent, properties: Map<String, AnalyticsPropValue> = emptyMap()) {
        AnalyticsTracker.track(event, properties)
    }

    fun refreshMetadata() {
        AnalyticsTracker.refreshMetadata()
    }

    fun flush() {
        AnalyticsTracker.flush()
    }

    fun clearAllData() {
        AnalyticsTracker.clearAllData()
    }

    fun setSendUsageStats(send: Boolean) {
        AnalyticsTracker.setSendUsageStats(send)
    }

    fun getSendUsageStats() = AnalyticsTracker.getSendUsageStats()
}
