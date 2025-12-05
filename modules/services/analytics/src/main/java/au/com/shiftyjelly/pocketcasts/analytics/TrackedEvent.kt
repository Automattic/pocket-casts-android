package au.com.shiftyjelly.pocketcasts.analytics

data class TrackedEvent(
    val key: AnalyticsEvent,
    val properties: Map<String, Any>,
    val usedKey: String = key.key,
)
