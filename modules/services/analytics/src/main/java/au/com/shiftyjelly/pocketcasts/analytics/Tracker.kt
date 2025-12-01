package au.com.shiftyjelly.pocketcasts.analytics

interface Tracker {
    val id: String

    fun shouldTrack(event: AnalyticsEvent): Boolean

    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()): TrackedEvent

    fun refreshMetadata()

    fun flush()

    fun clearAllData()
}
