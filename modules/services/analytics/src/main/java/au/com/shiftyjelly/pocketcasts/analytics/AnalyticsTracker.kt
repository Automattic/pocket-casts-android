package au.com.shiftyjelly.pocketcasts.analytics

open class AnalyticsTracker(
    val trackers: List<Tracker>,
    val isTrackingEnabled: () -> Boolean,
) {
    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        if (isTrackingEnabled()) {
            trackers.forEach { it.track(event, properties) }
        }
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

    companion object {
        fun test(vararg trackers: Tracker, isEnabled: Boolean = false) = AnalyticsTracker(trackers.toList()) { isEnabled }
    }
}
