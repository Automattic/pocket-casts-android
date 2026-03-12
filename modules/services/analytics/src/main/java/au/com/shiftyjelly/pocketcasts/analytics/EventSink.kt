package au.com.shiftyjelly.pocketcasts.analytics

import com.automattic.eventhorizon.Trackable

class EventSink(
    private val trackers: Set<AnalyticsTracker>,
    private val listeners: Set<AnalyticsListener>,
) : (Trackable) -> Unit,
    AnalyticsController {
    override fun invoke(event: Trackable) {
        val trackedEvents = trackers.associate { tracker ->
            tracker.id to tracker.track(event)
        }
        listeners.forEach { listener ->
            listener.onEvent(event, trackedEvents)
        }
    }

    override fun refreshMetadata() {
        trackers.forEach(AnalyticsTracker::refreshMetadata)
    }

    override fun flush() {
        trackers.forEach(AnalyticsTracker::flush)
    }

    override fun clearAllData() {
        trackers.forEach(AnalyticsTracker::clearAllData)
    }
}

data class TrackedEvent(
    val key: String,
    val properties: Map<String, Any>,
)

interface AnalyticsTracker {
    val id: String

    fun track(event: Trackable): TrackedEvent?

    fun refreshMetadata() = Unit

    fun flush() = Unit

    fun clearAllData() = Unit

    companion object {
        const val INVALID_OR_NULL_VALUE = "none"
    }
}

interface AnalyticsListener {
    fun onEvent(
        event: Trackable,
        trackedEvents: Map<String, TrackedEvent?> = emptyMap(),
    )
}

interface AnalyticsController {
    fun refreshMetadata()

    fun flush()

    fun clearAllData()
}

internal object NoOpTracker : AnalyticsTracker {
    override val id = "no-op"

    override fun track(event: Trackable) = null
}
