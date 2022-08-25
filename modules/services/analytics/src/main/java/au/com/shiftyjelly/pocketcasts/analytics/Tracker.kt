package au.com.shiftyjelly.pocketcasts.analytics

interface Tracker {
    fun track(event: AnalyticsEvent, properties: Map<String, *>? = null)
    fun flush()
    fun clearAllData()
    fun storeUsagePref()
}
