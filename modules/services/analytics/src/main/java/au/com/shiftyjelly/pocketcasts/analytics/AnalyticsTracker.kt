package au.com.shiftyjelly.pocketcasts.analytics

class AnalyticsTracker(
    val trackers: List<Tracker>,
    val isTrackingEnabled: () -> Boolean,
) {
    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        if (isTrackingEnabled()) {
            trackers.forEach { it.track(event, properties) }
        }
    }

    fun refreshMetadata() = trackers.forEach(Tracker::refreshMetadata)

    fun flush() = trackers.forEach(Tracker::flush)

    fun clearAllData() = trackers.forEach(Tracker::clearAllData)
}
