package au.com.shiftyjelly.pocketcasts.analytics

import java.util.concurrent.CopyOnWriteArrayList

open class AnalyticsTracker(
    val trackers: List<Tracker>,
    val isFirstPartyTrackingEnabled: () -> Boolean,
) {
    private val listeners = CopyOnWriteArrayList<Listener>()

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        val isFirstPartyEnabled = isFirstPartyTrackingEnabled()
        if (isFirstPartyEnabled) {
            trackers.forEach { tracker ->
                tracker.track(event, properties)
            }
        }
        listeners.forEach { listener ->
            listener.onEvent(event, properties)
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
        fun onEvent(event: AnalyticsEvent, properties: Map<String, Any>)
    }

    companion object {
        fun test(vararg trackers: Tracker, isFirstPartyEnabled: Boolean = false) = AnalyticsTracker(trackers.toList(), { isFirstPartyEnabled })
    }
}
