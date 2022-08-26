package au.com.shiftyjelly.pocketcasts.analytics

import javax.inject.Inject

class AnalyticsTrackerWrapper @Inject constructor() {
    fun track(event: AnalyticsEvent, properties: Map<String, *> = emptyMap<String, Any>()) {
        AnalyticsTracker.track(event, properties)
    }

    fun refreshMetadata() {
        AnalyticsTracker.refreshMetadata()
    }
}
