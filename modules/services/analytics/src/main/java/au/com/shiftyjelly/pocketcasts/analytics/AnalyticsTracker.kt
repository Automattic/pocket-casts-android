package au.com.shiftyjelly.pocketcasts.analytics

import javax.inject.Inject

open class AnalyticsTracker @Inject constructor() {

    open fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        AnalyticsTracker2.track(event, properties)
    }

    fun refreshMetadata() {
        AnalyticsTracker2.refreshMetadata()
    }

    fun flush() {
        AnalyticsTracker2.flush()
    }

    fun clearAllData() {
        AnalyticsTracker2.clearAllData()
    }

    fun setSendUsageStats(send: Boolean) {
        AnalyticsTracker2.setSendUsageStats(send)
    }

    fun getSendUsageStats() = AnalyticsTracker2.getSendUsageStats()
}
