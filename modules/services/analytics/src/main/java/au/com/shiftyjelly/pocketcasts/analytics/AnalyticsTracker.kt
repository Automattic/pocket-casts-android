package au.com.shiftyjelly.pocketcasts.analytics

class AnalyticsTracker(
    private val trackers: Set<Tracker>,
    private val listeners: Set<Listener>,
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

    fun trackBannerAdImpression(id: String, location: String) {
        track(
            AnalyticsEvent.BANNER_AD_IMPRESSION,
            mapOf(
                "id" to id,
                "location" to location,
            ),
        )
    }

    fun trackBannerAdTapped(id: String, location: String) {
        track(
            AnalyticsEvent.BANNER_AD_TAPPED,
            mapOf(
                "id" to id,
                "location" to location,
            ),
        )
    }

    fun trackBannerAdReport(id: String, reason: String, location: String) {
        track(
            AnalyticsEvent.BANNER_AD_REPORT,
            mapOf(
                "id" to id,
                "reason" to reason,
                "location" to location,
            ),
        )
    }

    fun refreshMetadata() {
        trackers.forEach(Tracker::refreshMetadata)
    }

    fun flush() {
        trackers.forEach(Tracker::flush)
    }

    fun clearAllData() {
        trackers.forEach(Tracker::clearAllData)
    }

    interface Listener {
        fun onEvent(
            event: AnalyticsEvent,
            properties: Map<String, Any>,
            trackedEvents: Map<String, TrackedEvent?>,
        )
    }

    companion object {
        fun test() = AnalyticsTracker(emptySet(), emptySet())

        fun test(tracker: Tracker) = AnalyticsTracker(
            trackers = setOf(tracker),
            listeners = emptySet(),
        )

        fun test(listener: Listener) = AnalyticsTracker(
            trackers = emptySet(),
            listeners = setOf(listener),
        )
    }
}
