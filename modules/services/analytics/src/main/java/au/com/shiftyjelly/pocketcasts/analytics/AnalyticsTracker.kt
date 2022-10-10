package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.preferences.Settings

object AnalyticsTracker {
    private val trackers: MutableList<Tracker> = mutableListOf()
    private lateinit var settings: Settings

    fun init(settings: Settings) {
        this.settings = settings
        trackers.forEach { it.clearAllData() }
    }

    fun registerTracker(tracker: Tracker?) {
        tracker?.let { trackers.add(tracker) }
    }

    fun track(event: AnalyticsEvent, properties: Map<String, AnalyticsPropValue> = emptyMap()) {
        if (getSendUsageStats()) {
            val convertedProps = properties.mapValues { it.value.propValue }
            trackers.forEach { it.track(event, convertedProps) }
        }
    }

    fun refreshMetadata() {
        trackers.forEach { it.refreshMetadata() }
    }

    fun flush() {
        trackers.forEach { it.flush() }
    }

    fun clearAllData() {
        trackers.forEach { it.clearAllData() }
    }

    fun setSendUsageStats(send: Boolean) {
        if (send != getSendUsageStats()) {
            settings.setSendUsageStats(send)
            if (!send) {
                trackers.forEach { it.clearAllData() }
            }
        }
    }

    fun getSendUsageStats() = settings.getSendUsageStats()
}
