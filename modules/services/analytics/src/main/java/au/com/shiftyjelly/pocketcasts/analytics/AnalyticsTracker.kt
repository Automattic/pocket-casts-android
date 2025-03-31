package au.com.shiftyjelly.pocketcasts.analytics

open class AnalyticsTracker(
    val trackers: List<Tracker>,
    val isFirstPartyTrackingEnabled: () -> Boolean,
    val isThirdPartyTrackingEnabled: () -> Boolean,
) {
    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        val isFirstPartyEnabled = isFirstPartyTrackingEnabled()
        val isThirdPartyEnabled = isThirdPartyTrackingEnabled()
        trackers.forEach { tracker ->
            if (
                (tracker.getTrackerType() == TrackerType.FirstParty && isFirstPartyEnabled) ||
                (tracker.getTrackerType() == TrackerType.ThirdParty && isThirdPartyEnabled)
            ) {
                tracker.track(event, properties)
            }
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
        fun test(vararg trackers: Tracker, isFirstPartyEnabled: Boolean = false, isThirdPartyEnabled: Boolean = false) =
            AnalyticsTracker(trackers.toList(), { isFirstPartyEnabled }, { isThirdPartyEnabled })
    }
}
