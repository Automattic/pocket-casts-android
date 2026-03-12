package au.com.shiftyjelly.pocketcasts.analytics

class AnalyticsTracker(
    private val trackers: Set<Tracker>,
    private val listeners: Set<AnalyticsListener>,
) {
    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        val trackedEvents = trackers.associate { tracker ->
            val trackedEvent = if (tracker.shouldTrack(event)) {
                tracker.track(event, properties)
            } else {
                null
            }
            tracker.id to trackedEvent
        }
        listeners.forEach { listener ->
            listener.onEvent(event, properties, trackedEvents)
        }
    }

    companion object {
        fun test() = AnalyticsTracker(emptySet(), emptySet())

        fun test(tracker: Tracker) = AnalyticsTracker(
            trackers = setOf(tracker),
            listeners = emptySet(),
        )

        fun test(listener: AnalyticsListener) = AnalyticsTracker(
            trackers = emptySet(),
            listeners = setOf(listener),
        )
    }
}
