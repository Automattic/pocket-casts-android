package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

open class AnalyticsTracker(
    val trackers: List<Tracker>,
    val isFirstPartyTrackingEnabled: () -> Boolean,
) {
    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        val isFirstPartyEnabled = isFirstPartyTrackingEnabled()
        trackers.forEach { tracker ->
            if (tracker.getTrackerType() == TrackerType.FirstParty && isFirstPartyEnabled) {
                tracker.track(event, properties)
            }
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

    companion object {
        fun test(vararg trackers: Tracker, isFirstPartyEnabled: Boolean = false) = AnalyticsTracker(trackers.toList(), { isFirstPartyEnabled })
    }
}
