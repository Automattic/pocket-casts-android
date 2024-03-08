package au.com.shiftyjelly.pocketcasts.analytics

import javax.inject.Inject

open class AnalyticsTrackerWrapper @Inject constructor() {

    open fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
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
