package au.com.shiftyjelly.pocketcasts.analytics

interface Tracker {
    val id: String

    fun shouldTrack(event: AnalyticsEvent): Boolean

    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()): TrackedEvent

    fun refreshMetadata()

    fun flush()

    fun clearAllData()

    companion object {
        const val INVALID_OR_NULL_VALUE = "none"
    }
}

internal object NoOpTracker : Tracker {
    override val id = "no-op"

    override fun shouldTrack(event: AnalyticsEvent) = false

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>) = TrackedEvent(event, properties)

    override fun refreshMetadata() = Unit

    override fun flush() = Unit

    override fun clearAllData() = Unit
}
