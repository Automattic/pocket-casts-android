package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.preferences.Settings

object AnalyticsTracker {
    private val trackers: MutableList<Tracker> = mutableListOf()
    private lateinit var settings: Settings

    var sendUsageStats: Boolean = true
        set(value) {
            if (value != field) {
                field = value
                settings.setSendUsageStats(sendUsageStats)
                if (!field) {
                    trackers.forEach { it.clearAllData() }
                }
            }
        }

    fun init(settings: Settings) {
        this.settings = settings
        trackers.forEach { it.clearAllData() }
        sendUsageStats = settings.getSendUsageStats()
    }

    fun registerTracker(tracker: Tracker?) {
        tracker?.let { trackers.add(tracker) }
    }

    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        // TODO only sending usage stats for debug builds while this feature is in development. Once we're
        // ready to release this, we should reverse this and default to _only_ sending usage stats when
        // it is _not_ a debug build (or do more checks when setting the `sendUsageStats` variable).
        if (sendUsageStats && BuildConfig.DEBUG) {
            trackers.forEach { it.track(event, properties) }
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
}
