package au.com.shiftyjelly.pocketcasts.analytics

class AnalyticsTracker(
    private val trackers: Set<Tracker>,
) {
    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        trackers.forEach { tracker ->
            if (tracker.shouldTrack(event)) {
                tracker.track(event, properties)
            }
        }
    }

    companion object {
        fun test(tracker: Tracker) = AnalyticsTracker(
            trackers = setOf(tracker),
        )
    }
}
