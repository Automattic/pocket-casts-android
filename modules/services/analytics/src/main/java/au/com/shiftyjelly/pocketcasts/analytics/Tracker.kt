package au.com.shiftyjelly.pocketcasts.analytics

interface Tracker {
    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap())
    fun refreshMetadata()
    fun flush()
    fun clearAllData()
}
